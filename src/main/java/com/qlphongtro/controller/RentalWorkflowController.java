package com.qlphongtro.controller;

import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.BookingRequest;
import com.qlphongtro.entity.DepositConfirmationRequest;
import com.qlphongtro.entity.Payment;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.UserNotification;
import com.qlphongtro.service.RentalWorkflowService;
import com.qlphongtro.service.RoomListingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Controller
public class RentalWorkflowController {

    private static final Logger log = LoggerFactory.getLogger(RentalWorkflowController.class);

    private final RentalWorkflowService rentalWorkflowService;
    private final RoomListingService roomListingService;

    public RentalWorkflowController(RentalWorkflowService rentalWorkflowService,
                                    RoomListingService roomListingService) {
        this.rentalWorkflowService = rentalWorkflowService;
        this.roomListingService = roomListingService;
    }

    @GetMapping("/tenant/rental/search")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantSearch(Model model) {
        model.addAttribute("rooms", roomListingService.getAvailableRooms());
        model.addAttribute("activePage", "rental-search");
        return "tenant/rental-search";
    }

    @GetMapping("/tenant/rental/request/{listingId}")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantRequestForm(@PathVariable Integer listingId, RedirectAttributes redirectAttributes) {
        RoomListing room;
        try {
            room = roomListingService.getById(listingId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/tenant/rental/search";
        }
        if (!"TRONG".equalsIgnoreCase(room.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phòng này không còn trống.");
            return "redirect:/rooms/" + listingId;
        }
        redirectAttributes.addFlashAttribute("openRentalModalListingId", listingId);
        return "redirect:/rooms/list";
    }

    @PostMapping("/tenant/rental/request")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantCreateRequest(@RequestParam(required = false) String listingId,
                                      @RequestParam(required = false) String expectedOccupants,
                                      @RequestParam(required = false) String expectedMoveInDate,
                                      @RequestParam(required = false) String note,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        Integer parsedListingId = null;
        try {
            if (listingId == null || listingId.isBlank()) {
                throw new IllegalArgumentException("Không xác định được phòng cần đăng ký.");
            }
            parsedListingId = Integer.valueOf(listingId);
            if (expectedOccupants == null || expectedOccupants.isBlank()
                    || expectedMoveInDate == null || expectedMoveInDate.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập đầy đủ ngày nhận phòng và số người ở.");
            }
            Short parsedOccupants = Short.valueOf(expectedOccupants);
            LocalDate parsedMoveInDate = LocalDate.parse(expectedMoveInDate);
            rentalWorkflowService.createRequest(authentication.getName(), parsedListingId,
                    parsedOccupants, parsedMoveInDate, note);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi yêu cầu thuê phòng thành công! Yêu cầu đã được gửi đến chủ trọ. Vui lòng chờ chủ trọ xác nhận.");
        } catch (NumberFormatException | DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ngày nhận phòng hoặc số người ở không hợp lệ.");
            preserveRentalForm(redirectAttributes, parsedListingId, expectedMoveInDate, expectedOccupants, note);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            preserveRentalForm(redirectAttributes, parsedListingId, expectedMoveInDate, expectedOccupants, note);
        } catch (DataAccessException ex) {
            log.error("Không thể lưu yêu cầu thuê: tenant={}, listingId={}",
                    authentication.getName(), parsedListingId, ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Hệ thống chưa thể lưu yêu cầu vào cơ sở dữ liệu. Vui lòng thử lại sau.");
            preserveRentalForm(redirectAttributes, parsedListingId, expectedMoveInDate, expectedOccupants, note);
        }
        return "redirect:/rooms/list";
    }

    private void preserveRentalForm(RedirectAttributes redirectAttributes, Integer listingId,
                                    String expectedMoveInDate, String expectedOccupants, String note) {
        if (listingId != null) {
            redirectAttributes.addFlashAttribute("openRentalModalListingId", listingId);
        }
        redirectAttributes.addFlashAttribute("submittedExpectedMoveInDate", expectedMoveInDate);
        redirectAttributes.addFlashAttribute("submittedExpectedOccupants", expectedOccupants);
        redirectAttributes.addFlashAttribute("submittedRentalNote", note);
    }

    @GetMapping("/tenant/rental/requests")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantRequests(Authentication authentication, Model model) {
        model.addAttribute("requests", rentalWorkflowService.getTenantRequests(authentication.getName()));
        model.addAttribute("activePage", "rental-requests");
        return "tenant/rental-requests";
    }

    @GetMapping("/tenant/rental/contracts")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantContracts(Authentication authentication, Model model) {
        model.addAttribute("contracts", rentalWorkflowService.getTenantContracts(authentication.getName()));
        model.addAttribute("activePage", "rental-contracts");
        return "tenant/rental-contracts";
    }

    @GetMapping("/tenant/rental/contracts/{id}")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantContract(@PathVariable Integer id, Authentication authentication, Model model) {
        Contract contract = rentalWorkflowService.getTenantContract(id, authentication.getName());
        List<Payment> payments = rentalWorkflowService.getContractPayments(id, authentication.getName());
        List<DepositConfirmationRequest> depositRequests =
                rentalWorkflowService.getTenantDepositRequests(id, authentication.getName());
        DepositConfirmationRequest latestDepositRequest =
                depositRequests.isEmpty() ? null : depositRequests.get(0);
        model.addAttribute("contract", contract);
        model.addAttribute("payments", payments);
        model.addAttribute("depositRequests", depositRequests);
        model.addAttribute("latestDepositRequest", latestDepositRequest);
        model.addAttribute("depositStatus", depositStatus(contract, payments, latestDepositRequest));
        model.addAttribute("canRequestDeposit", canRequestDeposit(contract, payments, latestDepositRequest));
        model.addAttribute("activePage", "rental-contracts");
        return "tenant/rental-contract-detail";
    }

    @PostMapping("/tenant/rental/contracts/{id}/accept")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String acceptContract(@PathVariable Integer id, Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.tenantAcceptContract(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đồng ý hợp đồng. Phòng đã được ghi nhận cho bạn; tiền cọc được theo dõi riêng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tenant/rental/contracts/" + id;
    }

    @PostMapping("/tenant/rental/contracts/{id}/reject")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantRejectContract(@PathVariable Integer id,
                                       @RequestParam String reason,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.tenantRejectContract(id, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối hợp đồng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tenant/rental/contracts/" + id;
    }

    @PostMapping("/tenant/rental/contracts/{id}/deposit")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String createDeposit(@PathVariable Integer id, Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.createDepositConfirmationRequest(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi yêu cầu xác nhận tiền đặt cọc thành công. Vui lòng chờ chủ trọ kiểm tra.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataAccessException ex) {
            log.error("Failed to create deposit confirmation request for contract {}", id, ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể lưu yêu cầu xác nhận tiền cọc. Vui lòng thử lại sau.");
        }
        return "redirect:/tenant/rental/contracts/" + id;
    }

    @GetMapping("/landlord/rental/requests")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordRequests(Authentication authentication,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) Integer roomId,
                                   @RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   Model model) {
        List<BookingRequest> allRequests = rentalWorkflowService.getLandlordRequests(authentication.getName());
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<BookingRequest> filtered = allRequests.stream()
                .filter(request -> status == null || status.isBlank() || "ALL".equals(status)
                        || status.equals(request.getStatus()))
                .filter(request -> roomId == null || (request.getRoom() != null
                        && roomId.equals(request.getRoom().getListingId())))
                .filter(request -> from == null || (request.getRequestDate() != null
                        && !request.getRequestDate().isBefore(from.atStartOfDay())))
                .filter(request -> to == null || (request.getRequestDate() != null
                        && request.getRequestDate().isBefore(to.plusDays(1).atStartOfDay())))
                .filter(request -> keyword.isBlank()
                        || containsIgnoreCase(request.getFullName(), keyword)
                        || containsIgnoreCase(request.getRoom() != null ? request.getRoom().getRoomCode() : null, keyword)
                        || containsIgnoreCase(request.getRoom() != null ? request.getRoom().getTitle() : null, keyword)
                        || (request.getRequestId() != null
                            && request.getRequestId().toString().contains(keyword)))
                .sorted(Comparator.comparing(BookingRequest::getRequestDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int pageSize = 10;
        int pageCount = Math.max(1, (int) Math.ceil(filtered.size() / (double) pageSize));
        int pageNumber = Math.min(Math.max(page, 0), pageCount - 1);
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        model.addAttribute("requests",
                filtered.subList(fromIndex, Math.min(fromIndex + pageSize, filtered.size())));
        model.addAttribute("rooms", allRequests.stream().map(BookingRequest::getRoom)
                .filter(Objects::nonNull).distinct().sorted(Comparator.comparing(RoomListing::getRoomCode)).toList());
        model.addAttribute("totalRequests", allRequests.size());
        model.addAttribute("pendingCount", countRequests(allRequests, "PENDING_LANDLORD_REVIEW"));
        model.addAttribute("approvedCount", countRequests(allRequests, "APPROVED_BY_LANDLORD"));
        model.addAttribute("awaitingTenantCount", countRequests(allRequests, "CONTRACT_SENT"));
        model.addAttribute("completedCount", countRequests(allRequests, "COMPLETED"));
        model.addAttribute("closedCount", allRequests.stream().filter(request ->
                List.of("REJECTED_BY_LANDLORD", "CANCELLED").contains(request.getStatus())).count());
        model.addAttribute("selectedStatus", status == null ? "ALL" : status);
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("keyword", q);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("totalPages", pageCount);
        model.addAttribute("hasPrevious", pageNumber > 0);
        model.addAttribute("hasNext", pageNumber + 1 < pageCount);
        model.addAttribute("activePage", "rental-requests");
        return "landlord/rental-requests";
    }

    @GetMapping("/landlord/rental/requests/{id}")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordRequestDetail(@PathVariable Integer id, Authentication authentication, Model model) {
        BookingRequest request = rentalWorkflowService.getLandlordRequest(id, authentication.getName());
        model.addAttribute("rentalRequest", request);
        try {
            model.addAttribute("contract", rentalWorkflowService.getLandlordContractForRequest(
                    id, authentication.getName()));
        } catch (IllegalArgumentException ignored) {
            model.addAttribute("contract", null);
        }
        model.addAttribute("activePage", "rental-requests");
        return "landlord/rental-request-detail";
    }

    @GetMapping("/landlord/rental/contracts/{id}")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordContract(@PathVariable Integer id, Authentication authentication, Model model) {
        Contract contract = rentalWorkflowService.getLandlordContract(id, authentication.getName());
        List<Payment> payments =
                rentalWorkflowService.getLandlordContractPayments(id, authentication.getName());
        DepositConfirmationRequest latestDepositRequest =
                rentalWorkflowService.getLandlordLatestDepositRequest(id, authentication.getName());
        model.addAttribute("contract", contract);
        model.addAttribute("payments", payments);
        model.addAttribute("latestDepositRequest", latestDepositRequest);
        model.addAttribute("depositStatus", depositStatus(contract, payments, latestDepositRequest));
        model.addAttribute("activePage", "contracts");
        return "landlord/rental-contract-detail";
    }

    @GetMapping("/landlord/rental/deposit-requests")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordDepositRequests(Authentication authentication,
                                          @RequestParam(required = false, defaultValue = "PENDING") String status,
                                          Model model) {
        model.addAttribute("requests",
                rentalWorkflowService.getLandlordDepositRequests(authentication.getName(), status));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activePage", "deposit-requests");
        return "landlord/deposit-requests";
    }

    @PostMapping("/landlord/rental/deposit-requests/{requestId}/approve")
    @PreAuthorize("hasRole('LANDLORD')")
    public String approveDepositRequest(@PathVariable Integer requestId,
                                        @RequestParam(required = false) String referenceNo,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.approveDepositConfirmationRequest(
                    requestId, referenceNo, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận nhận tiền cọc.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataAccessException ex) {
            log.error("Failed to approve deposit confirmation request {}", requestId, ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể lưu kết quả xác nhận tiền cọc. Vui lòng thử lại sau.");
        }
        return "redirect:/landlord/rental/deposit-requests";
    }

    @PostMapping("/landlord/rental/deposit-requests/{requestId}/reject")
    @PreAuthorize("hasRole('LANDLORD')")
    public String rejectDepositRequest(@PathVariable Integer requestId,
                                       @RequestParam String reason,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.rejectDepositConfirmationRequest(
                    requestId, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu xác nhận tiền cọc.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataAccessException ex) {
            log.error("Failed to reject deposit confirmation request {}", requestId, ex);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể lưu kết quả từ chối yêu cầu. Vui lòng thử lại sau.");
        }
        return "redirect:/landlord/rental/deposit-requests";
    }

    @PostMapping("/landlord/rental/requests/{id}/approve")
    @PreAuthorize("hasRole('LANDLORD')")
    public String approveRequest(@PathVariable Integer id, Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.approveRequest(id, authentication.getName());
            Contract draft = rentalWorkflowService.getLandlordDraftForRequest(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Yêu cầu đã được duyệt và bản nháp hợp đồng đã tạo. Kiểm tra, bổ sung điều khoản rồi gửi cho người thuê.");
            return "redirect:/landlord/rental/contracts/" + draft.getContractId() + "/edit";
        } catch (IllegalArgumentException | IllegalStateException orgEx) {
            redirectAttributes.addFlashAttribute("errorMessage", orgEx.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            throw ex;
        }
        return "redirect:/landlord/rental/requests";
    }

    @PostMapping("/landlord/rental/requests/{id}/reject")
    @PreAuthorize("hasRole('LANDLORD')")
    public String rejectRequest(@PathVariable Integer id, @RequestParam String reason,
                                Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.rejectRequest(id, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu thuê.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/rental/requests";
    }

    @GetMapping("/landlord/rental/contracts/new/{requestId}")
    @PreAuthorize("hasRole('LANDLORD')")
    public String newContract(@PathVariable Integer requestId, Authentication authentication) {
        return "redirect:/landlord/rental/contracts/"
                + rentalWorkflowService.getLandlordDraftForRequest(requestId, authentication.getName())
                        .getContractId() + "/edit";
    }

    @GetMapping("/landlord/rental/contracts/{id}/edit")
    @PreAuthorize("hasRole('LANDLORD')")
    public String editRentalContract(@PathVariable Integer id, Authentication authentication, Model model) {
        Contract contract = rentalWorkflowService.getLandlordContract(id, authentication.getName());
        if (!List.of("DRAFT", "TENANT_REJECTED").contains(contract.getStatus())) {
            throw new IllegalStateException("Hợp đồng này không còn ở trạng thái có thể chỉnh sửa.");
        }
        model.addAttribute("contract", contract);
        model.addAttribute("request", contract.getRentalRequest());
        model.addAttribute("activePage", "rental-requests");
        return "landlord/rental-contract-form";
    }

    @PostMapping("/landlord/rental/contracts/{id}/send")
    @PreAuthorize("hasRole('LANDLORD')")
    public String sendRentalContract(@PathVariable Integer id,
                                     @ModelAttribute("contract") Contract contract,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        Contract existing = rentalWorkflowService.getLandlordContract(id, authentication.getName());
        try {
            Contract saved = rentalWorkflowService.createAndSendContract(
                    existing.getRentalRequest().getRequestId(), contract, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi hợp đồng " + saved.getContractCode() + " cho người thuê.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/landlord/rental/contracts/" + id + "/edit";
        }
        return "redirect:/landlord/rental/requests/" + existing.getRentalRequest().getRequestId();
    }

    @PostMapping("/landlord/rental/contracts/{id}/approve")
    @PreAuthorize("hasRole('LANDLORD')")
    public String approveContract(@PathVariable Integer id, Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.landlordApproveContract(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt hợp đồng và kích hoạt phòng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/contracts";
    }

    @PostMapping("/landlord/rental/contracts/{id}/reject")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordRejectContract(@PathVariable Integer id, @RequestParam String reason,
                                         Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            rentalWorkflowService.landlordRejectContract(id, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối phê duyệt hợp đồng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/contracts";
    }

    @GetMapping("/landlord/notifications")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordNotifications(Authentication authentication, Model model) {
        model.addAttribute("notifications",
                rentalWorkflowService.getLandlordNotifications(authentication.getName()));
        model.addAttribute("activePage", "notifications");
        return "landlord/notifications";
    }

    @PostMapping("/landlord/notifications/{id}/open")
    @PreAuthorize("hasRole('LANDLORD')")
    public String openLandlordNotification(@PathVariable Integer id, Authentication authentication) {
        return "redirect:" + rentalWorkflowService.openLandlordNotification(id, authentication.getName());
    }

    private static long countRequests(List<BookingRequest> requests, String status) {
        return requests.stream().filter(request -> status.equals(request.getStatus())).count();
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String depositStatus(
            Contract contract, List<Payment> payments, DepositConfirmationRequest latestRequest) {
        if (contract.getDepositAmount() == null) return "Chưa cấu hình";
        if (contract.getDepositAmount().signum() == 0) return "Không yêu cầu tiền cọc";
        if ((latestRequest != null
                && DepositConfirmationRequest.STATUS_APPROVED.equalsIgnoreCase(latestRequest.getStatus()))
                || payments.stream().anyMatch(payment -> "SUCCESS".equalsIgnoreCase(payment.getStatus()))) {
            return "Đã thanh toán";
        }
        if (latestRequest != null
                && DepositConfirmationRequest.STATUS_PENDING.equalsIgnoreCase(latestRequest.getStatus())) {
            return "Đang chờ chủ trọ xác nhận";
        }
        if (payments.stream().anyMatch(payment -> List.of("PENDING", "PROCESSING")
                .contains(payment.getStatus() == null ? "" : payment.getStatus().toUpperCase(Locale.ROOT)))) {
            return "Đang xử lý";
        }
        if (latestRequest != null
                && DepositConfirmationRequest.STATUS_REJECTED.equalsIgnoreCase(latestRequest.getStatus())) {
            return "Bị từ chối";
        }
        return "Chưa thanh toán";
    }

    private static boolean canRequestDeposit(
            Contract contract, List<Payment> payments, DepositConfirmationRequest latestRequest) {
        return ("TENANT_ACCEPTED".equalsIgnoreCase(contract.getStatus())
                    || "ACTIVE".equalsIgnoreCase(contract.getStatus()))
                && contract.getDepositAmount() != null
                && contract.getDepositAmount().signum() > 0
                && (latestRequest == null
                    || !DepositConfirmationRequest.STATUS_PENDING.equalsIgnoreCase(latestRequest.getStatus()))
                && (latestRequest == null
                    || !DepositConfirmationRequest.STATUS_APPROVED.equalsIgnoreCase(latestRequest.getStatus()))
                && payments.stream().noneMatch(payment -> List.of("SUCCESS", "PENDING", "PROCESSING")
                        .contains(payment.getStatus() == null ? "" : payment.getStatus().toUpperCase(Locale.ROOT)));
    }

}
