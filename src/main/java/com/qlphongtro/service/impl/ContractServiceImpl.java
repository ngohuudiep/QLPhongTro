package com.qlphongtro.service.impl;

import com.qlphongtro.entity.*;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.ContractTenantRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public ContractServiceImpl(
            ContractRepository contractRepository,
            ContractTenantRepository contractTenantRepository,
            RoomRepository roomRepository,
            TenantRepository tenantRepository) {
        this.contractRepository = contractRepository;
        this.contractTenantRepository = contractTenantRepository;
        this.roomRepository = roomRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getContractById(Integer id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng ID: " + id));
    }

    /**
     * Lưu hợp đồng.
     * Logic: Khi một hợp đồng mới được tạo thành công, tự động cập nhật status của Room tương ứng thành "Đang thuê" (OCCUPIED).
     */
    @Override
    public Contract saveContract(Contract contract) {
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
    public void removeTenantFromContract(Integer contractId, Integer tenantId) {
        ContractTenantId id = new ContractTenantId(contractId, tenantId);
        if (contractTenantRepository.existsById(id)) {
            contractTenantRepository.deleteById(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenant> getTenantsByContractId(Integer contractId) {
        return contractTenantRepository.findById_ContractId(contractId);
    }
}
