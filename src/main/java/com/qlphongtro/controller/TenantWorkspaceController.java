package com.qlphongtro.controller;

import com.qlphongtro.dto.IncidentReportRequest;
import com.qlphongtro.dto.TenantDashboardView;
import com.qlphongtro.entity.Invoice;
import com.qlphongtro.service.TenantPortalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tenant")
@PreAuthorize("hasAnyRole('TENANT', 'NGUOITHUE', 'NGUOI_THUE')")
public class TenantWorkspaceController {

    private final TenantPortalService tenantPortalService;

    public TenantWorkspaceController(TenantPortalService tenantPortalService) {
        this.tenantPortalService = tenantPortalService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        TenantDashboardView dashboard = tenantPortalService.getDashboard(authentication.getName());
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("activePage", "dashboard");
        return "tenant/dashboard";
    }

    @GetMapping("/room")
    public String room(Authentication authentication, Model model) {
        TenantDashboardView dashboard = tenantPortalService.getDashboard(authentication.getName());
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("activePage", "room");
        return "tenant/room";
    }

    @GetMapping("/contracts")
    public String contracts(Authentication authentication, Model model) {
        model.addAttribute("contracts", tenantPortalService.getContracts(authentication.getName()));
        model.addAttribute("activePage", "contracts");
        return "tenant/contracts";
    }

    @GetMapping("/invoices")
    public String invoices(@RequestParam(required = false, defaultValue = "ALL") String status,
                           @RequestParam(required = false) String month,
                           @RequestParam(required = false, defaultValue = "0") int page,
                           Authentication authentication,
                           Model model) {
        Page<Invoice> invoicePage = paginate(
                tenantPortalService.getInvoices(authentication.getName(), status, month), page);
        model.addAttribute("invoices", invoicePage.getContent());
        model.addAttribute("invoicePage", invoicePage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("month", month);
        model.addAttribute("activePage", "invoices");
        return "tenant/invoices";
    }

    @GetMapping("/invoices/{id}")
    public String invoiceDetails(@PathVariable Integer id, Authentication authentication, Model model) {
        Invoice invoice = tenantPortalService.getInvoice(authentication.getName(), id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("activePage", "invoices");
        return "tenant/invoice-detail";
    }

    @GetMapping("/payments")
    public String payments(Authentication authentication, Model model) {
        model.addAttribute("payments", tenantPortalService.getPayments(authentication.getName()));
        model.addAttribute("activePage", "payments");
        return "tenant/payments";
    }

    @GetMapping("/notifications")
    public String notifications(Authentication authentication, Model model) {
        model.addAttribute("notifications", tenantPortalService.getNotifications(authentication.getName()));
        model.addAttribute("activePage", "notifications");
        return "tenant/notifications";
    }

    @GetMapping("/incidents")
    public String incidents(Authentication authentication, Model model) {
        model.addAttribute("contracts", tenantPortalService.getActiveContracts(authentication.getName()));
        model.addAttribute("incidents", tenantPortalService.getIncidents(authentication.getName()));
        model.addAttribute("incidentForm", new IncidentReportRequest());
        model.addAttribute("activePage", "incidents");
        return "tenant/incidents";
    }

    @PostMapping("/incidents")
    public String reportIncident(@Valid @ModelAttribute("incidentForm") IncidentReportRequest request,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/tenant/incidents";
        }
        try {
            tenantPortalService.reportIncident(authentication.getName(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu báo sự cố đến ban quản lý.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tenant/incidents";
    }

    @PostMapping("/notifications/{id}/open")
    public String openNotification(@PathVariable Integer id, Authentication authentication) {
        return "redirect:" + tenantPortalService.markNotificationRead(authentication.getName(), id);
    }

    private static Page<Invoice> paginate(java.util.List<Invoice> invoices, int requestedPage) {
        int pageSize = 10;
        int pageCount = Math.max(1, (int) Math.ceil(invoices.size() / (double) pageSize));
        int pageNumber = Math.min(Math.max(requestedPage, 0), pageCount - 1);
        int from = Math.min(pageNumber * pageSize, invoices.size());
        int to = Math.min(from + pageSize, invoices.size());
        return new PageImpl<>(invoices.subList(from, to), PageRequest.of(pageNumber, pageSize), invoices.size());
    }
}
