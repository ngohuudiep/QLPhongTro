package com.qlphongtro.service.impl;

import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.security.CurrentUser;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RoomServiceRepository roomServiceRepository;
    private final BuildingAccess buildingAccess;

    @Autowired
    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            ContractRepository contractRepository,
            MeterReadingRepository meterReadingRepository,
            RoomServiceRepository roomServiceRepository,
            BuildingAccess buildingAccess) {
        this.invoiceRepository = invoiceRepository;
        this.contractRepository = contractRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.roomServiceRepository = roomServiceRepository;
        this.buildingAccess = buildingAccess;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        if (CurrentUser.isTenant()) {
            return invoiceRepository.findAllForTenant(CurrentUser.username());
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : invoiceRepository.findByContract_Room_Building_BuildingIdIn(buildingIds);
        }
        return invoiceRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Integer id) {
        if (CurrentUser.isTenant()) {
            return invoiceRepository.findByIdAndTenantUsername(id, CurrentUser.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            if (buildingIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return invoiceRepository.findByInvoiceIdAndContract_Room_Building_BuildingIdIn(id, buildingIds)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + id));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByContractId(Integer contractId) {
        if (CurrentUser.isTenant()) {
            return invoiceRepository.findByContractIdAndTenantUsername(contractId, CurrentUser.username());
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : invoiceRepository.findByContract_ContractIdAndContract_Room_Building_BuildingIdIn(contractId, buildingIds);
        }
        return invoiceRepository.findByContract_ContractId(contractId);
    }

    /**
     * YÊU CẦU ĐỀ BÀI:
     * generateMonthlyInvoice(Integer contractId, Date month)
     * Tính tổng tiền bao gồm:
     * 1. Tiền phòng cơ bản
     * 2. Tiền điện/nước (từ bảng MeterReadings của tháng đó)
     * 3. Tiền dịch vụ cố định (từ Room_Services)
     * Trả về object Invoice đã được lưu.
     */
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Invoice generateMonthlyInvoice(Integer contractId, Date month) {
        if (month == null) {
            month = new Date();
        }
        LocalDate localDate = month.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return generateMonthlyInvoice(contractId, localDate);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Invoice generateMonthlyInvoice(Integer contractId, LocalDate month) {
        final LocalDate targetMonth = (month != null) ? month : LocalDate.now();

        // Lấy thông tin hợp đồng
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng ID: " + contractId));

        if (buildingAccess.isStaff()) {
            buildingAccess.requireRoom(contract.getRoom());
        }
        Room room = contract.getRoom();
        if (room == null) {
            throw new RuntimeException("Hợp đồng chưa được liên kết với phòng nào!");
        }

        // Xác định kỳ hóa đơn (từ ngày đầu tháng đến ngày cuối tháng)
        LocalDate periodStart = targetMonth.withDayOfMonth(1);
        LocalDate periodEnd = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());
        LocalDate dueDate = periodStart.plusMonths(1).withDayOfMonth(5); // Hạn thanh toán: ngày 5 tháng sau

        // =====================================================================
        // 1. TIỀN PHÒNG CƠ BẢN
        // =====================================================================
        BigDecimal roomPrice = BigDecimal.ZERO;
        if (room.getPriceOverride() != null && room.getPriceOverride().compareTo(BigDecimal.ZERO) > 0) {
            roomPrice = room.getPriceOverride();
        }

        // =====================================================================
        // 2. TIỀN ĐIỆN / NƯỚC (Từ bảng MeterReadings của tháng đó)
        // =====================================================================
        BigDecimal utilityCost = BigDecimal.ZERO;

        // Lấy các dịch vụ của phòng này để biết đơn giá theo chỉ số (Điện / Nước)
        List<RoomService> assignedServices = roomServiceRepository.findByRoom_RoomId(room.getRoomId());
        
        // Tìm bản ghi chốt chỉ số điện/nước trong tháng tương ứng
        List<MeterReading> readings = meterReadingRepository.findByRoom_RoomIdOrderByBillingMonthDesc(room.getRoomId());
        MeterReading monthlyReading = readings.stream()
                .filter(r -> r.getBillingMonth() != null &&
                        r.getBillingMonth().getYear() == targetMonth.getYear() &&
                        r.getBillingMonth().getMonthValue() == targetMonth.getMonthValue())
                .findFirst()
                .orElse(null);

        if (monthlyReading != null && monthlyReading.getConsumption() != null) {
            BigDecimal consumption = monthlyReading.getConsumption();
            
            // Tìm đơn giá dịch vụ theo chỉ số (calc_type = 'INDEX' hoặc dịch vụ tính theo chỉ số)
            BigDecimal unitPriceIndex = assignedServices.stream()
                    .filter(rs -> rs.getService() != null && "INDEX".equalsIgnoreCase(rs.getService().getCalcType()))
                    .map(RoomService::getUnitPrice)
                    .findFirst()
                    .orElse(new BigDecimal("3500")); // Đơn giá dự phòng: 3,500 đ/số

            utilityCost = consumption.multiply(unitPriceIndex);
        }

        // =====================================================================
        // 3. TIỀN DỊCH VỤ CỐ ĐỊNH (Từ bảng Room_Services)
        // =====================================================================
        BigDecimal fixedServicesCost = BigDecimal.ZERO;
        for (RoomService rs : assignedServices) {
            // Kiểm tra dịch vụ còn trong thời hạn hiệu lực
            boolean isEffective = !rs.getEffectiveFrom().isAfter(periodEnd) &&
                    (rs.getEffectiveTo() == null || !rs.getEffectiveTo().isBefore(periodStart));

            // Chỉ tính các dịch vụ cố định (không phải loại tính theo chỉ số điện/nước đã tính ở trên)
            if (isEffective && rs.getService() != null && !"INDEX".equalsIgnoreCase(rs.getService().getCalcType())) {
                if (rs.getUnitPrice() != null) {
                    fixedServicesCost = fixedServicesCost.add(rs.getUnitPrice());
                }
            }
        }

        // =====================================================================
        // 4. TÍNH TỔNG TIỀN VÀ TẠO HÓA ĐƠN
        // =====================================================================
        BigDecimal totalAmount = roomPrice.add(utilityCost).add(fixedServicesCost);

        // Tạo mã hóa đơn duy nhất: INV-MãPhòng-NămTháng (Ví dụ: INV-P101-202610)
        String invoiceCode = String.format("INV-%s-%d%02d",
                room.getRoomCode().replace(" ", ""),
                targetMonth.getYear(),
                targetMonth.getMonthValue());

        Invoice invoice = Invoice.builder()
                .invoiceCode(invoiceCode)
                .contract(contract)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .dueDate(dueDate)
                .totalAmount(totalAmount)
                .status("Chưa thanh toán")
                .build();

        // Lưu vào Database và trả về
        return invoiceRepository.save(invoice);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Invoice saveInvoice(Invoice invoice) {
        if (buildingAccess.isStaff() && invoice.getInvoiceId() != null) {
            getInvoiceById(invoice.getInvoiceId());
        }
        if (invoice.getContract() != null) {
            buildingAccess.requireRoom(invoice.getContract().getRoom());
        }
        return invoiceRepository.save(invoice);
    }

    @Override
    @PreAuthorize("denyAll()")
    public void deleteInvoice(Integer id) {
        if (!invoiceRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy hóa đơn ID: " + id);
        }
        invoiceRepository.deleteById(id);
    }
}
