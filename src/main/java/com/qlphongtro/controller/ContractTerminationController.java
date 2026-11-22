package com.qlphongtro.controller;

import com.qlphongtro.dto.TerminationRequestForm;
import com.qlphongtro.dto.TerminationSettlementForm;
import com.qlphongtro.entity.ContractSettlement;
import com.qlphongtro.entity.ContractTerminationRequest;
import com.qlphongtro.service.ContractTerminationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class ContractTerminationController {

    private static final List<String> FILTER_STATUSES = List.of(
            "PENDING_RESPONSE", "PENDING_SETTLEMENT", "DISPUTED", "REJECTED", "CANCELLED", "COMPLETED");
    private final ContractTerminationService terminationService;

    public ContractTerminationController(ContractTerminationService terminationService) {
        this.terminationService = terminationService;
    }

    @GetMapping("/landlord/terminations")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordRequests(@RequestParam(required = false, defaultValue = "ALL") String status,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) LocalDate from,
                                   @RequestParam(required = false) LocalDate to,
                                   @RequestParam(required = false, defaultValue = "0") int page,
                                   Authentication authentication,
                                   Model model) {
        List<ContractTerminationRequest> all = terminationService.getLandlordRequests(authentication.getName());
        String normalizedStatus = normalizeFilterStatus(status);
        List<ContractTerminationRequest> filtered = filter(all, normalizedStatus, q, from, to);
        Page<ContractTerminationRequest> requestPage = paginate(filtered, page);
        model.addAttribute("requests", requestPage.getContent());
        model.addAttribute("requestPage", requestPage);
        model.addAttribute("stats", countStatuses(all));
        model.addAttribute("selectedStatus", normalizedStatus);
        model.addAttribute("q", q);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("activePage", "terminations");
        return "landlord/terminations";
    }

    @GetMapping("/tenant/terminations")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantRequests(@RequestParam(required = false, defaultValue = "ALL") String status,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(required = false, defaultValue = "0") int page,
                                 Authentication authentication,
                                 Model model) {
        List<ContractTerminationRequest> all = terminationService.getTenantRequests(authentication.getName());
        List<ContractTerminationRequest> filtered = filter(all, normalizeFilterStatus(status), q, null, null);
        Page<ContractTerminationRequest> requestPage = paginate(filtered, page);
        model.addAttribute("requests", requestPage.getContent());
        model.addAttribute("requestPage", requestPage);
        model.addAttribute("selectedStatus", normalizeFilterStatus(status));
        model.addAttribute("q", q);
        model.addAttribute("activePage", "terminations");
        return "tenant/terminations";
    }

    @GetMapping("/landlord/terminations/{id}")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordDetails(@PathVariable Integer id, Authentication authentication, Model model) {
        ContractTerminationRequest request = terminationService.getLandlordRequest(id, authentication.getName());
        populateDetail(request, true, authentication.getName(), model);
        return "termination/detail";
    }

    @GetMapping("/tenant/terminations/{id}")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantDetails(@PathVariable Integer id, Authentication authentication, Model model) {
        ContractTerminationRequest request = terminationService.getTenantRequest(id, authentication.getName());
        populateDetail(request, false, authentication.getName(), model);
        return "termination/detail";
    }

    @PostMapping("/tenant/contracts/{id}/termination-requests")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantRequest(@PathVariable Integer id, @Valid @ModelAttribute TerminationRequestForm form,
                               BindingResult bindingResult, Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/tenant/contracts";
        }
        try {
            ContractTerminationRequest request = terminationService.requestFromTenant(
                    id, form, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm dứt hợp đồng.");
            return "redirect:/tenant/terminations/" + request.getTerminationRequestId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/tenant/contracts";
        }
    }

    @PostMapping("/landlord/contracts/{id}/termination-requests")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordRequest(@PathVariable Integer id, @Valid @ModelAttribute TerminationRequestForm form,
                                  BindingResult bindingResult, Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/landlord/contracts";
        }
        try {
            ContractTerminationRequest request = terminationService.requestFromLandlord(
                    id, form, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi đề nghị chấm dứt cho người thuê.");
            return "redirect:/landlord/terminations/" + request.getTerminationRequestId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/landlord/contracts";
        }
    }

    @PostMapping("/landlord/terminations/{id}/respond")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordRespond(@PathVariable Integer id, @RequestParam boolean accept,
                                  @RequestParam(required = false) String reason,
                                  Authentication authentication, RedirectAttributes redirectAttributes) {
        return respond(id, accept, reason, authentication, true, redirectAttributes);
    }

    @PostMapping("/tenant/terminations/{id}/respond")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantRespond(@PathVariable Integer id, @RequestParam boolean accept,
                                @RequestParam(required = false) String reason,
                                Authentication authentication, RedirectAttributes redirectAttributes) {
        return respond(id, accept, reason, authentication, false, redirectAttributes);
    }

    @PostMapping("/landlord/terminations/{id}/settlement")
    @PreAuthorize("hasRole('LANDLORD')")
    public String submitSettlement(@PathVariable Integer id,
                                   @Valid @ModelAttribute("settlementForm") TerminationSettlementForm form,
                                   BindingResult bindingResult, Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            redirectAttributes.addFlashAttribute("settlementForm", form);
            return "redirect:/landlord/terminations/" + id;
        }
        try {
            terminationService.submitSettlement(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi bảng thanh lý để người thuê xác nhận.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("settlementForm", form);
        }
        return "redirect:/landlord/terminations/" + id;
    }

    @PostMapping("/tenant/terminations/{id}/settlement/confirm")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String confirmSettlement(@PathVariable Integer id, Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            terminationService.confirmSettlement(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận bảng thanh lý.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tenant/terminations/" + id;
    }

    @PostMapping("/tenant/terminations/{id}/settlement/dispute")
    @PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String tenantDispute(@PathVariable Integer id, @RequestParam String reason,
                                Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            terminationService.disputeSettlement(id, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận tranh chấp; phòng chưa được giải phóng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tenant/terminations/" + id;
    }

    @PostMapping("/landlord/terminations/{id}/settlement/dispute")
    @PreAuthorize("hasRole('LANDLORD')")
    public String landlordDispute(@PathVariable Integer id, @RequestParam String reason,
                                  Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            terminationService.disputeSettlement(id, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận tranh chấp; phòng chưa được giải phóng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/terminations/" + id;
    }

    @PostMapping("/landlord/terminations/{id}/settlement/transfers")
    @PreAuthorize("hasRole('LANDLORD')")
    public String confirmTransfers(@PathVariable Integer id,
                                   @RequestParam(required = false) String tenantPaymentReference,
                                   @RequestParam(required = false) String depositRefundReference,
                                   Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            terminationService.confirmFinancialTransfers(
                    id, tenantPaymentReference, depositRefundReference, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã lưu xác nhận giao dịch thực tế. Chưa hoàn tất hợp đồng cho đến khi đủ điều kiện.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/terminations/" + id;
    }

    @PostMapping("/landlord/terminations/{id}/complete")
    @PreAuthorize("hasRole('LANDLORD')")
    public String complete(@PathVariable Integer id, Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            terminationService.completeTermination(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hoàn tất thanh lý và kết thúc hợp đồng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/landlord/terminations/" + id;
    }

    @PostMapping({"/tenant/terminations/{id}/cancel", "/landlord/terminations/{id}/cancel"})
    @PreAuthorize("hasAnyRole('LANDLORD', 'TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
    public String cancel(@PathVariable Integer id, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            terminationService.cancelRequest(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy yêu cầu chấm dứt.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirectToDetailByRole(authentication, id);
    }

    private String respond(Integer id, boolean accept, String reason, Authentication authentication,
                           boolean landlord, RedirectAttributes redirectAttributes) {
        try {
            terminationService.respond(id, accept, reason, authentication.getName(), landlord);
            redirectAttributes.addFlashAttribute("successMessage",
                    accept ? "Đã chấp nhận yêu cầu; cần thống nhất bảng thanh lý."
                            : "Đã từ chối yêu cầu chấm dứt.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + (landlord ? "/landlord" : "/tenant") + "/terminations/" + id;
    }

    private void populateDetail(ContractTerminationRequest request, boolean landlord, String username, Model model) {
        ContractSettlement settlement = request.getSettlement();
        if (settlement == null) {
            settlement = new ContractSettlement();
        }
        TerminationSettlementForm form = settlementForm(settlement);
        if (!model.containsAttribute("settlementForm")) {
            model.addAttribute("settlementForm", form);
        }
        model.addAttribute("terminationRequest", request);
        model.addAttribute("settlement", settlement);
        model.addAttribute("history", terminationService.getHistory(
                request.getTerminationRequestId(), username, landlord));
        boolean pendingResponse = ContractTerminationRequest.PENDING_RESPONSE.equals(request.getStatus());
        boolean actorIsResponder = landlord
                ? !"LANDLORD".equals(request.getRequesterRole())
                : "LANDLORD".equals(request.getRequesterRole());
        boolean canRespond = pendingResponse && actorIsResponder;
        boolean canCancel = pendingResponse && !actorIsResponder;
        boolean confirmed = ContractSettlement.CONFIRMED.equals(settlement.getStatus());
        boolean canSettle = landlord && !confirmed
                && (ContractTerminationRequest.PENDING_SETTLEMENT.equals(request.getStatus())
                || ContractTerminationRequest.DISPUTED.equals(request.getStatus()));
        model.addAttribute("isLandlord", landlord);
        model.addAttribute("canRespond", canRespond);
        model.addAttribute("canCancel", canCancel);
        model.addAttribute("canSettle", canSettle);
        model.addAttribute("canConfirmSettlement", !landlord
                && ContractSettlement.PENDING_CONFIRMATION.equals(settlement.getStatus()));
        model.addAttribute("canDispute", ContractSettlement.PENDING_CONFIRMATION.equals(settlement.getStatus()));
        model.addAttribute("canConfirmTransfers", landlord && confirmed);
        model.addAttribute("canComplete", landlord && confirmed
                && settlement.getLandlordConfirmedAt() != null
                && settlement.getTenantConfirmedAt() != null
                && (!positive(settlement.getTotalDueAmount()) || settlement.getTenantPaymentConfirmedAt() != null)
                && (!positive(settlement.getDepositRefundAmount()) || settlement.getDepositRefundedAt() != null));
        model.addAttribute("activePage", "terminations");
    }

    private static TerminationSettlementForm settlementForm(ContractSettlement settlement) {
        TerminationSettlementForm form = new TerminationSettlementForm();
        form.setDepositAmount(settlement.getDepositAmount());
        form.setRentThroughMoveOutAmount(settlement.getRentThroughMoveOutAmount());
        form.setElectricityOldReading(settlement.getElectricityOldReading());
        form.setElectricityNewReading(settlement.getElectricityNewReading());
        form.setElectricityUnitPrice(settlement.getElectricityUnitPrice());
        form.setWaterOldReading(settlement.getWaterOldReading());
        form.setWaterNewReading(settlement.getWaterNewReading());
        form.setWaterUnitPrice(settlement.getWaterUnitPrice());
        form.setServiceFeeAmount(settlement.getServiceFeeAmount());
        form.setCompensationAmount(settlement.getCompensationAmount());
        form.setDepositDeductionAmount(settlement.getDepositDeductionAmount());
        form.setCompensationReason(settlement.getCompensationReason());
        form.setDepositDeductionReason(settlement.getDepositDeductionReason());
        form.setRoomHandoverCondition(settlement.getRoomHandoverCondition());
        form.setDamageNotes(settlement.getDamageNotes());
        form.setNotes(settlement.getNotes());
        return form;
    }

    private static List<ContractTerminationRequest> filter(List<ContractTerminationRequest> requests, String status,
                                                           String keyword, LocalDate from, LocalDate to) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return requests.stream()
                .filter(request -> "ALL".equals(status) || status.equals(request.getStatus()))
                .filter(request -> from == null || (request.getCreatedAt() != null
                        && !request.getCreatedAt().toLocalDate().isBefore(from)))
                .filter(request -> to == null || (request.getCreatedAt() != null
                        && !request.getCreatedAt().toLocalDate().isAfter(to)))
                .filter(request -> normalizedKeyword.isEmpty()
                        || contains(request.getContract().getContractCode(), normalizedKeyword)
                        || contains(request.getContract().getRoom().getRoomCode(), normalizedKeyword)
                        || contains(request.getContract().getTenantFullName(), normalizedKeyword)
                        || contains(String.valueOf(request.getTerminationRequestId()), normalizedKeyword))
                .toList();
    }

    private static Map<String, Long> countStatuses(List<ContractTerminationRequest> requests) {
        return Map.of(
                "total", (long) requests.size(),
                "pending", count(requests, ContractTerminationRequest.PENDING_RESPONSE),
                "settlement", count(requests, ContractTerminationRequest.PENDING_SETTLEMENT),
                "disputed", count(requests, ContractTerminationRequest.DISPUTED),
                "rejected", count(requests, ContractTerminationRequest.REJECTED),
                "completed", count(requests, ContractTerminationRequest.COMPLETED));
    }

    private static long count(List<ContractTerminationRequest> requests, String status) {
        return requests.stream().filter(request -> status.equals(request.getStatus())).count();
    }

    private static String normalizeFilterStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return "ALL";
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!FILTER_STATUSES.contains(value)) {
            throw new IllegalArgumentException("Bộ lọc trạng thái yêu cầu chấm dứt không hợp lệ.");
        }
        return value;
    }

    private static Page<ContractTerminationRequest> paginate(List<ContractTerminationRequest> requests, int requestedPage) {
        int pageSize = 10;
        int pageCount = Math.max(1, (int) Math.ceil(requests.size() / (double) pageSize));
        int pageNumber = Math.min(Math.max(requestedPage, 0), pageCount - 1);
        int from = Math.min(pageNumber * pageSize, requests.size());
        int to = Math.min(from + pageSize, requests.size());
        return new PageImpl<>(requests.subList(from, to), PageRequest.of(pageNumber, pageSize), requests.size());
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static boolean positive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String firstError(BindingResult result) {
        return result.getAllErrors().isEmpty() ? "Dữ liệu nhập không hợp lệ."
                : result.getAllErrors().get(0).getDefaultMessage();
    }

    private static String redirectToDetailByRole(Authentication authentication, Integer id) {
        boolean landlord = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_LANDLORD".equals(authority.getAuthority()));
        return "redirect:/" + (landlord ? "landlord" : "tenant") + "/terminations/" + id;
    }
}
