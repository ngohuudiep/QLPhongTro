package com.qlphongtro.controller;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.Tenant;
import com.qlphongtro.dto.InvoiceCreateRequest;
import com.qlphongtro.dto.LandlordTenantView;
import com.qlphongtro.service.InvoiceService;
import com.qlphongtro.service.LandlordWorkspaceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.BindingResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/landlord")
@PreAuthorize("hasRole('LANDLORD')")
public class LandlordWorkspaceController {

    private final LandlordWorkspaceService landlordWorkspaceService;
    private final InvoiceService invoiceService;
    private final com.qlphongtro.service.ReviewService reviewService;

    public LandlordWorkspaceController(LandlordWorkspaceService landlordWorkspaceService, InvoiceService invoiceService) {
        this(landlordWorkspaceService, invoiceService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public LandlordWorkspaceController(LandlordWorkspaceService landlordWorkspaceService,
                                       InvoiceService invoiceService,
                                       @org.springframework.beans.factory.annotation.Autowired(required = false) com.qlphongtro.service.ReviewService reviewService) {
        this.landlordWorkspaceService = landlordWorkspaceService;
        this.invoiceService = invoiceService;
        this.reviewService = reviewService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("stats", landlordWorkspaceService.getDashboard(authentication.getName()));
        model.addAttribute("unrepliedReviewsCount", reviewService != null ? reviewService.getUnrepliedReviewsCountForLandlord(authentication.getName()) : 0L);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("activePage", "dashboard");
        return "landlord/dashboard";
    }

    @GetMapping("/buildings")
    public String buildings(Authentication authentication, Model model) {
        model.addAttribute("buildings", landlordWorkspaceService.getBuildings(authentication.getName()));
        model.addAttribute("activePage", "buildings");
        return "landlord/buildings";
    }

    @GetMapping("/buildings/new")
    public String newBuilding(Model model) {
        model.addAttribute("building", new Building());
        model.addAttribute("activePage", "buildings");
        return "landlord/building-form";
    }

    @GetMapping("/buildings/{id}/edit")
    public String editBuilding(@PathVariable Integer id, Authentication authentication, Model model) {
        model.addAttribute("building", landlordWorkspaceService.getBuilding(id, authentication.getName()));
        model.addAttribute("activePage", "buildings");
        return "landlord/building-form";
    }

    @PostMapping("/buildings/save")
    public String saveBuilding(Building building, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            landlordWorkspaceService.saveBuilding(building, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu thông tin khu trọ.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return building.getBuildingId() == null ? "redirect:/landlord/buildings/new"
                    : "redirect:/landlord/buildings/" + building.getBuildingId() + "/edit";
        }
        return "redirect:/landlord/buildings";
    }

    @PostMapping("/buildings/inline")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createInlineBuilding(Building building, Authentication authentication) {
        try {
            Building saved = landlordWorkspaceService.saveBuilding(building, authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "buildingId", saved.getBuildingId(),
                    "buildingName", saved.getBuildingName(),
                    "address", saved.getAddress()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/buildings/{id}/delete")
    public String deleteBuilding(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            landlordWorkspaceService.deleteBuilding(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khu trọ.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/buildings";
    }

    @GetMapping("/rooms")
    public String rooms(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false, defaultValue = "ALL") String status,
                        Authentication authentication,
                        Model model) {
        model.addAttribute("rooms", landlordWorkspaceService.getRooms(authentication.getName(), keyword, status));
        model.addAttribute("buildings", landlordWorkspaceService.getBuildings(authentication.getName()));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activePage", "rooms");
        return "landlord/rooms";
    }

    @GetMapping("/rooms/new")
    public String newRoom(Authentication authentication, Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("listing", new RoomListing());
        model.addAttribute("buildings", landlordWorkspaceService.getBuildings(authentication.getName()));
        model.addAttribute("activePage", "rooms");
        return "landlord/room-form";
    }

    @GetMapping("/rooms/{id}")
    public String roomDetails(@PathVariable Integer id, Authentication authentication, Model model) {
        model.addAttribute("room", landlordWorkspaceService.getRoom(id, authentication.getName()));
        model.addAttribute("listing", landlordWorkspaceService.getRoomListingForRoom(id, authentication.getName()));
        model.addAttribute("activePage", "rooms");
        return "landlord/room-detail";
    }

    @GetMapping("/rooms/{id}/edit")
    public String editRoom(@PathVariable Integer id, Authentication authentication, Model model) {
        model.addAttribute("room", landlordWorkspaceService.getRoom(id, authentication.getName()));
        model.addAttribute("listing", landlordWorkspaceService.getRoomListingForRoom(id, authentication.getName()));
        model.addAttribute("buildings", landlordWorkspaceService.getBuildings(authentication.getName()));
        model.addAttribute("activePage", "rooms");
        return "landlord/room-form";
    }

    @PostMapping("/rooms/save")
    public String saveRoom(Room room,
                           @ModelAttribute("listing") RoomListing listing,
                           @RequestParam Integer buildingId,
                           @RequestParam(required = false) List<String> retainedImageUrls,
                           @RequestParam(required = false) String primaryImageUrl,
                           @RequestParam(required = false) Integer primaryUploadIndex,
                           @RequestParam(name = "images", required = false) List<MultipartFile> images,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            landlordWorkspaceService.saveRoom(room, listing, buildingId, retainedImageUrls,
                    primaryImageUrl, primaryUploadIndex, images, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu thông tin phòng.");
            return "redirect:/landlord/rooms";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return room.getRoomId() == null ? "redirect:/landlord/rooms/new"
                    : "redirect:/landlord/rooms/" + room.getRoomId() + "/edit";
        }
    }

    @PostMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            landlordWorkspaceService.deleteRoom(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phòng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/rooms";
    }

    @GetMapping("/tenants")
    public String tenants(@RequestParam(required = false) String keyword, Authentication authentication, Model model) {
        List<LandlordTenantView> tenants = landlordWorkspaceService.getTenantOccupancies(authentication.getName());
        if (keyword != null && !keyword.isBlank()) {
            String query = keyword.trim().toLowerCase(Locale.ROOT);
            tenants = tenants.stream().filter(tenant ->
                    contains(tenant.fullName(), query)
                            || contains(tenant.phone(), query)
                            || contains(tenant.roomCode(), query)
                            || contains(tenant.contractCode(), query)
                            || contains(tenant.username(), query)).toList();
        }
        model.addAttribute("tenants", tenants);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "tenants");
        return "landlord/tenants";
    }

    @GetMapping("/contracts")
    public String contracts(Authentication authentication, Model model) {
        model.addAttribute("contracts", landlordWorkspaceService.getContracts(authentication.getName()));
        model.addAttribute("activePage", "contracts");
        return "landlord/contracts";
    }

    @GetMapping("/contracts/new")
    public String newContract(Authentication authentication, Model model) {
        model.addAttribute("rooms", landlordWorkspaceService.getAvailableRooms(authentication.getName()));
        model.addAttribute("buildings", landlordWorkspaceService.getBuildings(authentication.getName()));
        model.addAttribute("tenants", landlordWorkspaceService.getTenants(authentication.getName()));
        model.addAttribute("tenantAccounts", landlordWorkspaceService.getTenantAccountOptions());
        model.addAttribute("tenant", new Tenant());
        model.addAttribute("contract", new Contract());
        model.addAttribute("activePage", "contracts");
        return "landlord/contract-form";
    }

    @GetMapping("/contracts/{id}")
    public String contractDetails(@PathVariable Integer id, Authentication authentication, Model model) {
        model.addAttribute("contract", landlordWorkspaceService.getContract(id, authentication.getName()));
        model.addAttribute("activePage", "contracts");
        return "landlord/contract-detail";
    }

    @GetMapping("/contracts/{id}/edit")
    public String editContract(@PathVariable Integer id, Authentication authentication, Model model) {
        model.addAttribute("contract", landlordWorkspaceService.getContract(id, authentication.getName()));
        model.addAttribute("activePage", "contracts");
        return "landlord/contract-form";
    }

    @PostMapping("/contracts")
    public String createContract(@ModelAttribute("contract") Contract contract,
                                @RequestParam Integer roomId,
                                @RequestParam(required = false) Integer tenantId,
                                @RequestParam(required = false) Integer tenantUserId,
                                @ModelAttribute("tenant") Tenant tenant,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            Contract saved = landlordWorkspaceService.createContract(contract, roomId, tenantId,
                    tenantUserId, tenant, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo hợp đồng " + saved.getContractCode() + ".");
            return "redirect:/landlord/contracts";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/landlord/contracts/new";
        }
    }

    @PostMapping("/contracts/{id}")
    public String updateContract(@PathVariable Integer id,
                                @ModelAttribute("contract") Contract contract,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            landlordWorkspaceService.updateContract(id, contract, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hợp đồng.");
            return "redirect:/landlord/contracts/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/landlord/contracts/" + id + "/edit";
        }
    }

    @GetMapping("/invoices")
    public String invoices(@RequestParam(required = false) Integer contractId,
                           @RequestParam(required = false) String month,
                           @RequestParam(required = false, defaultValue = "ALL") String status,
                           @RequestParam(required = false, defaultValue = "0") int page,
                           Authentication authentication,
                           Model model) {
        List<Invoice> invoices = landlordWorkspaceService.getInvoices(authentication.getName());
        long paidCount = invoices.stream()
                .filter(invoice -> "PAID".equals(normalizeInvoiceStatus(invoice.getEffectiveStatus())))
                .count();
        long overdueCount = invoices.stream()
                .filter(invoice -> "OVERDUE".equals(normalizeInvoiceStatus(invoice.getEffectiveStatus())))
                .count();
        long unpaidCount = invoices.stream()
                .filter(invoice -> "UNPAID".equals(normalizeInvoiceStatus(invoice.getEffectiveStatus())))
                .count();
        BigDecimal revenue = invoices.stream()
                .filter(invoice -> "PAID".equals(normalizeInvoiceStatus(invoice.getEffectiveStatus())))
                .map(Invoice::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivables = invoices.stream()
                .filter(invoice -> {
                    String effectiveStatus = normalizeInvoiceStatus(invoice.getEffectiveStatus());
                    return "UNPAID".equals(effectiveStatus) || "OVERDUE".equals(effectiveStatus);
                })
                .map(Invoice::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("invoiceStats", Map.of(
                "totalCount", invoices.size(),
                "paidCount", paidCount,
                "unpaidCount", unpaidCount,
                "overdueCount", overdueCount,
                "revenue", revenue,
                "receivables", receivables));
        String normalizedStatus = status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "UNPAID", "PAID", "OVERDUE", "CANCELLED").contains(normalizedStatus)) {
            throw new IllegalArgumentException("Bộ lọc trạng thái hóa đơn không hợp lệ.");
        }
        YearMonth selectedMonth = null;
        if (month != null && !month.isBlank()) {
            try {
                selectedMonth = YearMonth.parse(month);
            } catch (java.time.format.DateTimeParseException ex) {
                throw new IllegalArgumentException("Tháng lọc không hợp lệ.");
            }
        }
        YearMonth periodFilter = selectedMonth;
        invoices = invoices.stream()
                .filter(invoice -> contractId == null || (invoice.getContract() != null
                        && contractId.equals(invoice.getContract().getContractId())))
                .filter(invoice -> periodFilter == null || (invoice.getPeriodStart() != null
                        && YearMonth.from(invoice.getPeriodStart()).equals(periodFilter)))
                .filter(invoice -> "ALL".equals(normalizedStatus)
                        || normalizedStatus.equals(normalizeInvoiceStatus(invoice.getEffectiveStatus())))
                .toList();
        Page<Invoice> invoicePage = paginate(invoices, page);
        model.addAttribute("invoices", invoicePage.getContent());
        model.addAttribute("invoicePage", invoicePage);
        model.addAttribute("contracts", landlordWorkspaceService.getContracts(authentication.getName()));
        model.addAttribute("selectedContractId", contractId);
        model.addAttribute("month", month);
        model.addAttribute("selectedStatus", normalizedStatus);
        model.addAttribute("invoiceForm", new InvoiceCreateRequest());
        model.addAttribute("currentMonth", YearMonth.now().toString());
        model.addAttribute("activePage", "invoices");
        return "landlord/invoices";
    }

    @GetMapping("/invoices/{id}")
    public String invoiceDetails(@PathVariable Integer id, Authentication authentication, Model model) {
        model.addAttribute("invoice", invoiceService.getInvoiceForLandlord(id, authentication.getName()));
        model.addAttribute("activePage", "invoices");
        return "landlord/invoice-detail";
    }

    @PostMapping("/invoices")
    public String createInvoice(@Valid @ModelAttribute("invoiceForm") InvoiceCreateRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/landlord/invoices";
        }
        try {
            Invoice invoice = invoiceService.createMonthlyInvoice(request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi hóa đơn " + invoice.getInvoiceCode() + " và thông báo đến người thuê.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/invoices";
    }

    @PostMapping("/invoices/{id}/confirm-paid")
    public String confirmInvoicePaid(@PathVariable Integer id,
                                     @RequestParam String method,
                                     @RequestParam(required = false) String referenceNo,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            invoiceService.confirmInvoicePaidByLandlord(id, authentication.getName(), method, referenceNo);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã ghi nhận khoản thanh toán thực nhận cho hóa đơn.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/invoices/" + id;
    }

    @PostMapping("/invoices/{id}/cancel")
    public String cancelInvoice(@PathVariable Integer id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            invoiceService.cancelInvoice(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy hóa đơn.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/invoices/" + id;
    }

    @PostMapping("/invoices/generate")
    public String generateInvoice(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "Vui lòng lập hóa đơn bằng biểu mẫu chi tiết để nhập chỉ số, dịch vụ và gửi thông báo cho người thuê.");
        return "redirect:/landlord/invoices";
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
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

    private static Page<Invoice> paginate(List<Invoice> invoices, int requestedPage) {
        int pageSize = 10;
        int pageCount = Math.max(1, (int) Math.ceil(invoices.size() / (double) pageSize));
        int pageNumber = Math.min(Math.max(requestedPage, 0), pageCount - 1);
        int from = Math.min(pageNumber * pageSize, invoices.size());
        int to = Math.min(from + pageSize, invoices.size());
        return new PageImpl<>(invoices.subList(from, to), PageRequest.of(pageNumber, pageSize), invoices.size());
    }
}
