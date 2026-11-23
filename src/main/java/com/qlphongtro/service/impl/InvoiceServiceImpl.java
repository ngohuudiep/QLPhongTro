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
import com.qlphongtro.dto.InvoiceCreateRequest;
import com.qlphongtro.dto.InvoiceServiceChargeRequest;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;

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
    private final PaymentRepository paymentRepository;
    private final UserNotificationRepository userNotificationRepository;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            ContractRepository contractRepository,
            MeterReadingRepository meterReadingRepository,
            RoomServiceRepository roomServiceRepository,
            PaymentRepository paymentRepository,
            UserNotificationRepository userNotificationRepository) {
        this(invoiceRepository, contractRepository, meterReadingRepository, roomServiceRepository, null, paymentRepository, userNotificationRepository);
    }

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            ContractRepository contractRepository,
            MeterReadingRepository meterReadingRepository,
            RoomServiceRepository roomServiceRepository,
            BuildingAccess buildingAccess) {
        this(invoiceRepository, contractRepository, meterReadingRepository, roomServiceRepository, buildingAccess, null, null);
    }

    @Autowired
    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            ContractRepository contractRepository,
            MeterReadingRepository meterReadingRepository,
            RoomServiceRepository roomServiceRepository,
            BuildingAccess buildingAccess,
            @Autowired(required = false) PaymentRepository paymentRepository,
            @Autowired(required = false) UserNotificationRepository userNotificationRepository) {
        this.invoiceRepository = invoiceRepository;
        this.contractRepository = contractRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.roomServiceRepository = roomServiceRepository;
        this.buildingAccess = buildingAccess;
        this.paymentRepository = paymentRepository;
        this.userNotificationRepository = userNotificationRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER', 'LANDLORD')")
    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        if (CurrentUser.isTenant()) {
            return invoiceRepository.findAllForTenant(CurrentUser.username());
        }
        if (buildingAccess != null && buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : invoiceRepository.findByContract_Room_Building_BuildingIdIn(buildingIds);
        }
        return invoiceRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER', 'LANDLORD')")
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Integer id) {
        if (CurrentUser.isTenant()) {
            return invoiceRepository.findByIdAndTenantUsername(id, CurrentUser.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        if (buildingAccess != null && buildingAccess.isStaff()) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER', 'LANDLORD')")
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByContractId(Integer contractId) {
        if (CurrentUser.isTenant()) {
            return invoiceRepository.findByContractIdAndTenantUsername(contractId, CurrentUser.username());
        }
        if (buildingAccess != null && buildingAccess.isStaff()) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'LANDLORD')")
    public Invoice generateMonthlyInvoice(Integer contractId, Date month) {
        if (month == null) {
            month = new Date();
        }
        LocalDate localDate = month.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return generateMonthlyInvoice(contractId, localDate);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'LANDLORD')")
    public Invoice generateMonthlyInvoice(Integer contractId, LocalDate month) {
        final LocalDate targetMonth = (month != null) ? month : LocalDate.now();

        // Lấy thông tin hợp đồng
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng ID: " + contractId));

        if (buildingAccess != null && buildingAccess.isStaff()) {
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
    public Invoice createMonthlyInvoice(InvoiceCreateRequest request, String landlordUsername) {
        if (request == null || request.getContractId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn hợp đồng/phòng.");
        }
        YearMonth billingMonth;
        try {
            billingMonth = YearMonth.parse(request.getMonth());
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("Tháng thanh toán không hợp lệ.");
        }

        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hợp đồng."));
        ensureLandlordOwns(contract, landlordUsername);

        Room room = contract.getRoom();
        if (room == null || room.getBuilding() == null) {
            throw new IllegalStateException("Hợp đồng chưa liên kết phòng và khu trọ hợp lệ.");
        }
        LocalDate periodStart = billingMonth.atDay(1);
        LocalDate periodEnd = billingMonth.atEndOfMonth();
        if (!isActiveContract(contract, periodStart, periodEnd)) {
            throw new IllegalArgumentException("Chỉ có thể lập hóa đơn cho hợp đồng còn hiệu lực trong tháng đã chọn.");
        }
        if (request.getDueDate() == null || request.getDueDate().isBefore(periodStart)) {
            throw new IllegalArgumentException("Hạn thanh toán phải từ ngày đầu kỳ hóa đơn trở đi.");
        }
        if (invoiceRepository.existsByContract_ContractIdAndPeriodStart(contract.getContractId(), periodStart)
                || invoiceRepository.existsByContract_Room_RoomIdAndPeriodStart(room.getRoomId(), periodStart)) {
            throw new IllegalStateException("Phòng đã có hóa đơn cho tháng này.");
        }

        validateNonNegative(request.getRoomAmount(), "Tiền phòng");
        validateNonNegative(request.getElectricityPreviousReading(), "Chỉ số điện cũ");
        validateNonNegative(request.getElectricityCurrentReading(), "Chỉ số điện mới");
        validateNonNegative(request.getElectricityUnitPrice(), "Đơn giá điện");
        validateNonNegative(request.getWaterPreviousReading(), "Chỉ số nước cũ");
        validateNonNegative(request.getWaterCurrentReading(), "Chỉ số nước mới");
        validateNonNegative(request.getWaterUnitPrice(), "Đơn giá nước");

        BigDecimal electricityUsed = request.getElectricityCurrentReading()
                .subtract(request.getElectricityPreviousReading());
        BigDecimal waterUsed = request.getWaterCurrentReading()
                .subtract(request.getWaterPreviousReading());
        if (electricityUsed.signum() < 0 || waterUsed.signum() < 0) {
            throw new IllegalArgumentException("Chỉ số mới không được nhỏ hơn chỉ số cũ.");
        }
        if (request.getNotes() != null && request.getNotes().length() > 2000) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 2.000 ký tự.");
        }

        List<InvoiceLineItem> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        total = addLine(lines, "Tiền phòng", BigDecimal.ONE, "tháng",
                money(request.getRoomAmount()), null, null);
        total = total.add(addLine(lines, "Tiền điện", electricityUsed, "kWh",
                money(request.getElectricityUnitPrice()),
                request.getElectricityPreviousReading(), request.getElectricityCurrentReading()));
        total = total.add(addLine(lines, "Tiền nước", waterUsed, "m³",
                money(request.getWaterUnitPrice()),
                request.getWaterPreviousReading(), request.getWaterCurrentReading()));

        if (request.getServiceCharges() != null) {
            for (InvoiceServiceChargeRequest charge : request.getServiceCharges()) {
                if (charge == null) {
                    continue;
                }
                String name = charge.getName() == null ? "" : charge.getName().trim();
                BigDecimal amount = charge.getAmount();
                if (name.isEmpty() && amount == null) {
                    continue;
                }
                if (name.isEmpty() || amount == null) {
                    throw new IllegalArgumentException("Mỗi khoản dịch vụ phải có tên và số tiền.");
                }
                if (name.length() > 120) {
                    throw new IllegalArgumentException("Tên khoản dịch vụ tối đa 120 ký tự.");
                }
                validateNonNegative(amount, "Số tiền dịch vụ");
                BigDecimal serviceAmount = money(amount);
                total = total.add(addLine(lines, name, BigDecimal.ONE, "khoản",
                        serviceAmount, null, null));
            }
        }

        Map<Integer, User> recipients = invoiceRecipients(contract, periodStart, periodEnd);
        if (recipients.isEmpty()) {
            throw new IllegalStateException("Hợp đồng chưa liên kết với tài khoản người thuê đang hoạt động; chưa thể gửi hóa đơn.");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(String.format("INV-%d-%s", room.getRoomId(),
                billingMonth.format(DateTimeFormatter.ofPattern("yyyyMM"))));
        invoice.setContract(contract);
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setDueDate(request.getDueDate());
        invoice.setTotalAmount(money(total));
        invoice.setStatus("UNPAID");
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setNotes(blankToNull(request.getNotes()));
        for (InvoiceLineItem line : lines) {
            line.setInvoice(invoice);
        }
        invoice.setLineItems(lines);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        if (userNotificationRepository != null) {
            String title = "Hóa đơn tháng " + billingMonth.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            String message = String.format(
                    "Chủ trọ vừa gửi hóa đơn thanh toán tháng %s. Tổng số tiền cần thanh toán: %s VNĐ. Hạn thanh toán: %s.",
                    billingMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")),
                    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                            .format(money(savedInvoice.getTotalAmount())),
                    request.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            for (User recipient : recipients.values()) {
                UserNotification notification = new UserNotification();
                notification.setRecipient(recipient);
                notification.setInvoice(savedInvoice);
                notification.setNotificationType(UserNotification.TYPE_INVOICE);
                notification.setTitle(title);
                notification.setMessage(message);
                userNotificationRepository.save(notification);
            }
        }
        return savedInvoice;
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getInvoiceForLandlord(Integer invoiceId, String landlordUsername) {
        Invoice invoice = getInvoiceById(invoiceId);
        ensureLandlordOwns(invoice.getContract(), landlordUsername);
        return invoice;
    }

    @Override
    public void confirmInvoicePaidByLandlord(Integer invoiceId, String landlordUsername,
                                             String method, String referenceNo) {
        Invoice invoice = getInvoiceById(invoiceId);
        ensureLandlordOwns(invoice.getContract(), landlordUsername);
        String status = invoice.getStatus();
        if ("CANCELLED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)
                || "ĐÃ HỦY".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Không thể ghi nhận thanh toán cho hóa đơn đã hủy.");
        }
        if ("PAID".equalsIgnoreCase(status) || "ĐÃ THANH TOÁN".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Hóa đơn đã được xác nhận thanh toán.");
        }
        if (!"CASH".equals(method) && !"BANK_TRANSFER".equals(method)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
        if (referenceNo != null && referenceNo.length() > 50) {
            throw new IllegalArgumentException("Mã giao dịch tối đa 50 ký tự.");
        }
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(money(invoice.getTotalAmount()));
        payment.setMethod(method);
        payment.setReferenceNo(blankToNull(referenceNo));
        LocalDateTime confirmedAt = LocalDateTime.now();
        payment.setPaidAt(confirmedAt);
        payment.setProcessedAt(confirmedAt);
        payment.setStatus("SUCCESS");
        if (paymentRepository != null) {
            paymentRepository.save(payment);
        }
        invoice.setStatus("PAID");
        invoiceRepository.save(invoice);
    }

    @Override
    public void cancelInvoice(Integer invoiceId, String landlordUsername) {
        Invoice invoice = getInvoiceById(invoiceId);
        ensureLandlordOwns(invoice.getContract(), landlordUsername);
        String status = invoice.getStatus();
        if ("PAID".equalsIgnoreCase(status) || "ĐÃ THANH TOÁN".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Không thể hủy hóa đơn đã thanh toán.");
        }
        if ("CANCELLED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)
                || "ĐÃ HỦY".equalsIgnoreCase(status)) {
            return;
        }
        invoice.setStatus("CANCELLED");
        invoiceRepository.save(invoice);
    }

    private static BigDecimal addLine(List<InvoiceLineItem> lines, String name, BigDecimal quantity,
                                      String unit, BigDecimal unitPrice, BigDecimal previousReading,
                                      BigDecimal currentReading) {
        BigDecimal amount = money(quantity.multiply(unitPrice));
        InvoiceLineItem line = new InvoiceLineItem();
        line.setName(name);
        line.setQuantity(quantity);
        line.setUnit(unit);
        line.setUnitPrice(unitPrice);
        line.setAmount(amount);
        line.setPreviousReading(previousReading);
        line.setCurrentReading(currentReading);
        lines.add(line);
        return amount;
    }

    private Map<Integer, User> invoiceRecipients(Contract contract, LocalDate periodStart, LocalDate periodEnd) {
        Map<Integer, User> recipients = new LinkedHashMap<>();
        Tenant representative = contract.getRepresentative();
        if (representative != null && tenantWasLinkedForPeriod(contract, representative, periodStart, periodEnd)) {
            addEligibleRecipient(recipients, representative.getUser());
        }
        addEligibleRecipient(recipients, contract.getTenantUser());
        if (contract.getContractTenants() != null) {
            contract.getContractTenants().stream()
                    .filter(link -> isContractTenantPresent(link, periodStart, periodEnd))
                    .map(link -> link.getTenant() == null ? null : link.getTenant().getUser())
                    .forEach(user -> addEligibleRecipient(recipients, user));
        }
        return recipients;
    }

    private static boolean tenantWasLinkedForPeriod(Contract contract, Tenant tenant,
                                                    LocalDate periodStart, LocalDate periodEnd) {
        if (contract.getContractTenants() != null) {
            for (ContractTenant link : contract.getContractTenants()) {
                if (link.getTenant() != null && tenant.getTenantId().equals(link.getTenant().getTenantId())) {
                    return isContractTenantPresent(link, periodStart, periodEnd);
                }
            }
        }
        return contract.getRepresentative() != null
                && tenant.getTenantId().equals(contract.getRepresentative().getTenantId());
    }

    private static boolean isContractTenantPresent(ContractTenant link, LocalDate periodStart,
                                                   LocalDate periodEnd) {
        return (link.getMoveInDate() == null || !link.getMoveInDate().isAfter(periodEnd))
                && (link.getMoveOutDate() == null || !link.getMoveOutDate().isBefore(periodStart));
    }

    private static void addEligibleRecipient(Map<Integer, User> recipients, User user) {
        if (user == null || user.getUserId() == null || !user.isActiveTenant()) {
            return;
        }
        recipients.put(user.getUserId(), user);
    }

    private static boolean isActiveContract(Contract contract, LocalDate periodStart, LocalDate periodEnd) {
        String status = contract.getStatus();
        boolean activeStatus = "ACTIVE".equalsIgnoreCase(status)
                || "HIỆU LỰC".equalsIgnoreCase(status)
                || "ĐANG HIỆU LỰC".equalsIgnoreCase(status)
                || "TENANT_ACCEPTED".equalsIgnoreCase(status);
        return activeStatus
                && contract.getStartDate() != null
                && contract.getEndDate() != null
                && !contract.getStartDate().isAfter(periodEnd)
                && !contract.getEndDate().isBefore(periodStart);
    }

    private static void ensureLandlordOwns(Contract contract, String landlordUsername) {
        if (contract == null || contract.getRoom() == null || contract.getRoom().getBuilding() == null
                || contract.getRoom().getBuilding().getOwner() == null
                || !landlordUsername.equals(contract.getRoom().getBuilding().getOwner().getUsername())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập hóa đơn/hợp đồng này.");
        }
    }

    private static void validateNonNegative(BigDecimal amount, String label) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException(label + " không được để trống hoặc âm.");
        }
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Invoice saveInvoice(Invoice invoice) {
        if (buildingAccess != null && buildingAccess.isStaff() && invoice.getInvoiceId() != null) {
            getInvoiceById(invoice.getInvoiceId());
        }
        if (buildingAccess != null && invoice.getContract() != null) {
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
