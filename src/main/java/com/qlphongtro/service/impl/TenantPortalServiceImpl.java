package com.qlphongtro.service.impl;

import com.qlphongtro.dto.IncidentReportRequest;
import com.qlphongtro.dto.TenantDashboardView;
import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.service.TenantPortalService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class TenantPortalServiceImpl implements TenantPortalService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final ContractRepository contractRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final IncidentRepository incidentRepository;

    public TenantPortalServiceImpl(UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   ContractRepository contractRepository,
                                   ContractTenantRepository contractTenantRepository,
                                   InvoiceRepository invoiceRepository,
                                   PaymentRepository paymentRepository,
                                   UserNotificationRepository userNotificationRepository,
                                   IncidentRepository incidentRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.contractRepository = contractRepository;
        this.contractTenantRepository = contractTenantRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    public TenantDashboardView getDashboard(String username) {
        User user = currentUser(username);
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        List<Contract> contracts = contractsForTenant(tenant, user.getUserId());
        List<Integer> contractIds = contracts.stream().map(Contract::getContractId).toList();
        Map<Integer, ContractTenant> tenantLinks = tenantLinks(tenant);
        List<Invoice> invoices = invoicesForContracts(contractIds).stream()
                .filter(invoice -> invoice.getContract() != null
                        && tenantHasAccessDuring(user.getUserId(), tenant, invoice.getContract(), tenantLinks,
                        invoice.getPeriodStart(), invoice.getPeriodEnd()))
                .toList();
        LocalDate today = LocalDate.now();
        Contract activeContract = contracts.stream()
                .filter(contract -> isActiveContract(contract, today)
                        && tenantHasActiveContractAccess(user.getUserId(), tenant, contract, tenantLinks, today))
                .max(Comparator.comparing(Contract::getStartDate))
                .orElse(null);
        YearMonth currentMonth = YearMonth.now();
        Invoice currentInvoice = invoices.stream()
                .filter(invoice -> invoice.getPeriodStart() != null
                        && YearMonth.from(invoice.getPeriodStart()).equals(currentMonth))
                .findFirst().orElse(null);
        long unpaidCount = invoices.stream().filter(TenantPortalServiceImpl::isOutstanding).count();
        BigDecimal outstandingAmount = invoices.stream()
                .filter(TenantPortalServiceImpl::isOutstanding)
                .map(TenantPortalServiceImpl::outstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long unreadCount = userNotificationRepository.countByRecipient_UserIdAndReadAtIsNull(user.getUserId());
        return new TenantDashboardView(user, tenant, contracts, activeContract, invoices, currentInvoice,
                unpaidCount, unreadCount, outstandingAmount);
    }

    @Override
    public List<Contract> getContracts(String username) {
        User user = currentUser(username);
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        return contractsForTenant(tenant, user.getUserId());
    }

    @Override
    public List<Contract> getActiveContracts(String username) {
        User user = currentUser(username);
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        Map<Integer, ContractTenant> links = tenantLinks(tenant);
        LocalDate today = LocalDate.now();
        return contractsForTenant(tenant, user.getUserId()).stream()
                .filter(contract -> isActiveContract(contract, today)
                        && tenantHasActiveContractAccess(user.getUserId(), tenant, contract, links, today))
                .toList();
    }

    @Override
    public List<Invoice> getInvoices(String username, String status, String month) {
        List<Invoice> invoices = invoicesForUser(username);
        String normalizedStatus = status == null || status.isBlank()
                ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "UNPAID", "PAID", "OVERDUE", "CANCELLED").contains(normalizedStatus)) {
            throw new IllegalArgumentException("Bộ lọc trạng thái hóa đơn không hợp lệ.");
        }
        YearMonth period = null;
        if (month != null && !month.isBlank()) {
            try {
                period = YearMonth.parse(month);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Tháng lọc không hợp lệ.");
            }
        }
        YearMonth selectedPeriod = period;
        return invoices.stream()
                .filter(invoice -> "ALL".equals(normalizedStatus)
                        || normalizedStatus.equals(normalizeInvoiceStatus(invoice.getEffectiveStatus())))
                .filter(invoice -> selectedPeriod == null || (invoice.getPeriodStart() != null
                        && YearMonth.from(invoice.getPeriodStart()).equals(selectedPeriod)))
                .toList();
    }

    @Override
    public Invoice getInvoice(String username, Integer invoiceId) {
        return authorizedInvoice(username, invoiceId);
    }

    @Override
    public List<Payment> getPayments(String username) {
        List<Payment> payments = new ArrayList<>();
        for (Invoice invoice : invoicesForUser(username)) {
            payments.addAll(paymentRepository.findByInvoice_InvoiceId(invoice.getInvoiceId()));
        }
        payments.sort(Comparator.comparing(Payment::getPaidAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return payments;
    }

    @Override
    public List<UserNotification> getNotifications(String username) {
        User user = currentUser(username);
        return userNotificationRepository.findByRecipient_UserIdOrderByCreatedAtDesc(user.getUserId());
    }

    @Override
    @Transactional
    public String markNotificationRead(String username, Integer notificationId) {
        User user = currentUser(username);
        UserNotification notification = userNotificationRepository
                .findByNotificationIdAndRecipient_UserId(notificationId, user.getUserId())
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy thông báo thuộc tài khoản này."));
        String target;
        if (notification.getTerminationRequest() != null) {
            ContractTerminationRequest terminationRequest = notification.getTerminationRequest();
            if (!contractBelongsToUser(terminationRequest.getContract(), user)) {
                throw new AccessDeniedException("Bạn không có quyền mở yêu cầu chấm dứt trong thông báo này.");
            }
            target = "/tenant/terminations/" + terminationRequest.getTerminationRequestId();
        } else if (notification.getInvoice() != null) {
            Invoice invoice = authorizedInvoice(username, notification.getInvoice().getInvoiceId());
            target = "/tenant/invoices/" + invoice.getInvoiceId();
        } else if (notification.getContract() != null) {
            Contract contract = notification.getContract();
            if (!contractBelongsToUser(contract, user)) {
                throw new AccessDeniedException("Bạn không có quyền mở hợp đồng trong thông báo này.");
            }
            target = "/tenant/rental/contracts/" + contract.getContractId();
        } else if (notification.getRentalRequest() != null
                && notification.getRentalRequest().getUser() != null
                && notification.getRentalRequest().getUser().getUserId().equals(user.getUserId())) {
            target = "/tenant/rental/requests";
        } else {
            target = "/tenant/notifications";
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(java.time.LocalDateTime.now());
            userNotificationRepository.save(notification);
        }
        return target;
    }

    @Override
    public List<Incident> getIncidents(String username) {
        User user = currentUser(username);
        return incidentRepository.findByReportedBy_UserIdOrderByReportedAtDesc(user.getUserId());
    }

    @Override
    @Transactional
    public Incident reportIncident(String username, IncidentReportRequest request) {
        if (request == null || request.getContractId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn phòng đang thuê.");
        }
        User user = currentUser(username);
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        Map<Integer, ContractTenant> links = tenantLinks(tenant);
        LocalDate today = LocalDate.now();
        Contract contract = getContracts(username).stream()
                .filter(item -> item.getContractId().equals(request.getContractId()))
                .filter(item -> isActiveContract(item, today))
                .filter(item -> tenantHasActiveContractAccess(user.getUserId(), tenant, item, links, today))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Phòng được chọn không thuộc hợp đồng đang thuê của bạn."));
        String priority = request.getPriority() == null ? "MEDIUM" : request.getPriority().trim().toUpperCase();
        if (!List.of("LOW", "MEDIUM", "HIGH").contains(priority)) {
            throw new IllegalArgumentException("Mức độ ưu tiên không hợp lệ.");
        }
        Incident incident = new Incident();
        incident.setRoom(contract.getRoom());
        incident.setReportedBy(user);
        if (tenant != null) {
            incident.setTenant(tenant);
        }
        incident.setTitle(request.getTitle().trim());
        incident.setDescription(request.getDescription().trim());
        incident.setPriority(priority);
        incident.setStatus("Tiếp nhận");
        incident.setReportedAt(java.time.LocalDateTime.now());
        return incidentRepository.save(incident);
    }

    private User currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản đang đăng nhập."));
        if (!user.isActiveTenant()) {
            throw new AccessDeniedException("Chức năng này chỉ dành cho người thuê.");
        }
        return user;
    }

    private List<Invoice> invoicesForUser(String username) {
        User user = currentUser(username);
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        List<Contract> contracts = contractsForTenant(tenant, user.getUserId());
        Map<Integer, ContractTenant> links = tenantLinks(tenant);
        return invoicesForContracts(contracts.stream().map(Contract::getContractId).toList()).stream()
                .filter(invoice -> invoice.getContract() != null
                        && tenantHasAccessDuring(user.getUserId(), tenant, invoice.getContract(), links,
                        invoice.getPeriodStart(), invoice.getPeriodEnd()))
                .toList();
    }

    private Invoice authorizedInvoice(String username, Integer invoiceId) {
        User user = currentUser(username);
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        Map<Integer, ContractTenant> links = tenantLinks(tenant);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
        if (invoice.getContract() == null || !tenantHasAccessDuring(user.getUserId(), tenant,
                invoice.getContract(), links,
                invoice.getPeriodStart(), invoice.getPeriodEnd())) {
            throw new AccessDeniedException("Bạn không có quyền xem hóa đơn này.");
        }
        return invoice;
    }

    private List<Contract> contractsForTenant(Tenant tenant, Integer userId) {
        Map<Integer, Contract> contracts = new LinkedHashMap<>();
        contractRepository.findByTenantUser_UserIdOrderByContractIdDesc(userId)
                .forEach(contract -> contracts.put(contract.getContractId(), contract));
        if (tenant != null && tenant.getTenantId() != null) {
            contractRepository.findByRepresentative_TenantId(tenant.getTenantId())
                    .forEach(contract -> contracts.put(contract.getContractId(), contract));
            contractTenantRepository.findById_TenantId(tenant.getTenantId()).stream()
                    .map(ContractTenant::getContract)
                    .filter(contract -> contract != null)
                    .forEach(contract -> contracts.put(contract.getContractId(), contract));
        }
        return contracts.values().stream()
                .filter(contract -> !"DRAFT".equalsIgnoreCase(contract.getStatus()))
                .sorted(Comparator.comparing(Contract::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<Invoice> invoicesForContracts(List<Integer> contractIds) {
        if (contractIds.isEmpty()) {
            return List.of();
        }
        return invoiceRepository.findByContract_ContractIdInOrderByPeriodStartDesc(contractIds);
    }

    private Map<Integer, ContractTenant> tenantLinks(Tenant tenant) {
        if (tenant == null || tenant.getTenantId() == null) {
            return Map.of();
        }
        Map<Integer, ContractTenant> links = new LinkedHashMap<>();
        contractTenantRepository.findById_TenantId(tenant.getTenantId()).stream()
                .filter(link -> link.getContract() != null && link.getContract().getContractId() != null)
                .forEach(link -> links.put(link.getContract().getContractId(), link));
        return links;
    }

    private static boolean tenantHasAccessDuring(Integer userId, Tenant tenant, Contract contract,
                                                 Map<Integer, ContractTenant> tenantLinks,
                                                 LocalDate periodStart, LocalDate periodEnd) {
        if (contract == null || periodStart == null || periodEnd == null) {
            return false;
        }
        if (contract.getTenantUser() != null
                && contract.getTenantUser().getUserId().equals(userId)) {
            return (contract.getStartDate() == null || !contract.getStartDate().isAfter(periodEnd))
                    && (contract.getEndDate() == null || !contract.getEndDate().isBefore(periodStart));
        }
        if (tenant == null || tenant.getTenantId() == null) return false;
        ContractTenant link = tenantLinks.get(contract.getContractId());
        if (link != null) {
            return (link.getMoveInDate() == null || !link.getMoveInDate().isAfter(periodEnd))
                    && (link.getMoveOutDate() == null || !link.getMoveOutDate().isBefore(periodStart));
        }
        return contract.getRepresentative() != null
                && tenant.getTenantId().equals(contract.getRepresentative().getTenantId());
    }

    private static boolean tenantHasActiveContractAccess(Integer userId, Tenant tenant, Contract contract,
                                                        Map<Integer, ContractTenant> tenantLinks,
                                                        LocalDate today) {
        return (("TENANT_ACCEPTED".equalsIgnoreCase(contract.getStatus()) || "ACTIVE".equalsIgnoreCase(contract.getStatus()))
                && contract.getTenantUser() != null
                && contract.getTenantUser().getUserId().equals(userId))
                || tenantHasAccessDuring(userId, tenant, contract, tenantLinks, today, today);
    }

    private static boolean isActiveContract(Contract contract, LocalDate today) {
        String status = contract.getStatus();
        if ("TENANT_ACCEPTED".equalsIgnoreCase(status) || "TERMINATION_REQUESTED".equalsIgnoreCase(status)) {
            return contract.getStartDate() != null
                    && (contract.getEndDate() == null || !contract.getEndDate().isBefore(today));
        }
        return ("ACTIVE".equalsIgnoreCase(status) || "HIỆU LỰC".equalsIgnoreCase(status))
                && contract.getStartDate() != null && contract.getEndDate() != null
                && (contract.getEndDate() == null || !contract.getEndDate().isBefore(today));
    }

    private static boolean isOutstanding(Invoice invoice) {
        String status = normalizeInvoiceStatus(invoice.getEffectiveStatus());
        return "UNPAID".equals(status) || "OVERDUE".equals(status);
    }

    private static String normalizeInvoiceStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "UNPAID", "CHƯA THANH TOÁN" -> "UNPAID";
            case "PAID", "ĐÃ THANH TOÁN" -> "PAID";
            case "OVERDUE", "QUÁ HẠN" -> "OVERDUE";
            case "CANCELLED", "CANCELED", "ĐÃ HỦY" -> "CANCELLED";
            default -> status.toUpperCase(Locale.ROOT);
        };
    }

    private static BigDecimal outstandingBalance(Invoice invoice) {
        BigDecimal total = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
        BigDecimal paid = invoice.getPayments() == null ? BigDecimal.ZERO : invoice.getPayments().stream()
                .map(Payment::getAmount).filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.subtract(paid).max(BigDecimal.ZERO);
    }

    private boolean contractBelongsToUser(Contract contract, User user) {
        if (contract.getTenantUser() != null
                && contract.getTenantUser().getUserId().equals(user.getUserId())) {
            return true;
        }
        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId()).orElse(null);
        return tenant != null && (contract.getRepresentative() != null
                && java.util.Objects.equals(contract.getRepresentative().getTenantId(), tenant.getTenantId())
                || contractTenantRepository.existsById_ContractIdAndId_TenantId(
                contract.getContractId(), tenant.getTenantId()));
    }
}
