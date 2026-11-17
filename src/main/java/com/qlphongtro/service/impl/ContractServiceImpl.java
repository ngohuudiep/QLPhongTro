package com.qlphongtro.service.impl;

import com.qlphongtro.entity.*;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.security.CurrentUser;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final BuildingAccess buildingAccess;

    @Autowired
    public ContractServiceImpl(
            ContractRepository contractRepository,
            ContractTenantRepository contractTenantRepository,
            RoomRepository roomRepository,
            TenantRepository tenantRepository,
            BuildingAccess buildingAccess) {
        this.contractRepository = contractRepository;
        this.contractTenantRepository = contractTenantRepository;
        this.roomRepository = roomRepository;
        this.tenantRepository = tenantRepository;
        this.buildingAccess = buildingAccess;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<Contract> getAllContracts() {
        if (CurrentUser.isTenant()) {
            return contractRepository.findAllForTenant(CurrentUser.username());
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : contractRepository.findDistinctByRoom_Building_BuildingIdIn(buildingIds);
        }
        return contractRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public Contract getContractById(Integer id) {
        if (CurrentUser.isTenant()) {
            return contractRepository.findByIdAndTenantUsername(id, CurrentUser.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            if (buildingIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return contractRepository.findByContractIdAndRoom_Building_BuildingIdIn(id, buildingIds)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        return contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng ID: " + id));
    }

    /**
     * Lưu hợp đồng.
     * Logic: Khi một hợp đồng mới được tạo thành công, tự động cập nhật status của Room tương ứng thành "Đang thuê" (OCCUPIED).
     */
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Contract saveContract(Contract contract) {
        if (buildingAccess.isStaff()) {
            if (contract.getContractId() != null) {
                getContractById(contract.getContractId());
            }
            buildingAccess.requireRoom(contract.getRoom());
        }
        boolean isNewContract = (contract.getContractId() == null);

        // Lưu hợp đồng vào database
        Contract savedContract = contractRepository.save(contract);

        // >>> YÊU CẦU: Tự động cập nhật status của Room thành "Đang thuê" (OCCUPIED) khi tạo hợp đồng mới <<<
        if (isNewContract && savedContract.getRoom() != null) {
            Integer roomId = savedContract.getRoom().getRoomId();
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room != null) {
                room.setStatus("Đang thuê"); // Cập nhật trạng thái thành Đang thuê (OCCUPIED)
                roomRepository.save(room);
            }
        }

        return savedContract;
    }

    @Override
    @PreAuthorize("denyAll()")
    public void deleteContract(Integer id) {
        Contract contract = getContractById(id);
        
        // Tùy chọn: Khi xóa hợp đồng, có thể cập nhật lại phòng về trạng thái "Trống" nếu không còn hợp đồng nào khác
        Room room = contract.getRoom();
        contractRepository.deleteById(id);

        if (room != null) {
            List<Contract> remainingContracts = contractRepository.findByRoom_RoomId(room.getRoomId());
            if (remainingContracts.isEmpty()) {
                room.setStatus("Trống");
                roomRepository.save(room);
            }
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public void addTenantToContract(Integer contractId, Integer tenantId, LocalDate moveInDate) {
        Contract contract = getContractById(contractId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách thuê ID: " + tenantId));

        ContractTenantId id = new ContractTenantId(contractId, tenantId);
        ContractTenant contractTenant = ContractTenant.builder()
                .id(id)
                .contract(contract)
                .tenant(tenant)
                .moveInDate(moveInDate != null ? moveInDate : LocalDate.now())
                .build();

        contractTenantRepository.save(contractTenant);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public void removeTenantFromContract(Integer contractId, Integer tenantId) {
        getContractById(contractId);
        ContractTenantId id = new ContractTenantId(contractId, tenantId);
        if (contractTenantRepository.existsById(id)) {
            contractTenantRepository.deleteById(id);
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<ContractTenant> getTenantsByContractId(Integer contractId) {
        if (!CurrentUser.isTenant()) {
            getContractById(contractId);
        }
        if (CurrentUser.isTenant()) {
            return contractTenantRepository.findById_ContractIdAndTenant_User_Username(contractId, CurrentUser.username());
        }
        return contractTenantRepository.findById_ContractId(contractId);
    }
}
