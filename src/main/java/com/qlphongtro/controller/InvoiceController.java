package com.qlphongtro.controller;

import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Payment;
import com.qlphongtro.service.ContractService;
import com.qlphongtro.service.InvoiceService;
import com.qlphongtro.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ContractService contractService;
    private final PaymentService paymentService;

    @Autowired
    public InvoiceController(
            InvoiceService invoiceService,
            ContractService contractService,
            PaymentService paymentService) {
        this.invoiceService = invoiceService;
        this.contractService = contractService;
        this.paymentService = paymentService;
    }

    // 1. Danh sách hóa đơn
    @GetMapping
    public String listInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        model.addAttribute("contracts", contractService.getAllContracts());
        return "invoice/list";
    }

    // 2. Kích hoạt sinh hóa đơn hàng tháng tự động
    @PostMapping("/generate")
    public String generateInvoice(
            @RequestParam("contractId") Integer contractId,
            @RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") LocalDate month,
            RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.generateMonthlyInvoice(contractId, month);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Sinh hóa đơn thành công (" + invoice.getInvoiceCode() + ")! Tổng tiền: " + invoice.getTotalAmount() + " ₫");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể sinh hóa đơn: " + e.getMessage());
        }
        return "redirect:/invoices";
    }

    // 3. Xem chi tiết hóa đơn
    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable("id") Integer id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", paymentService.getPaymentsByInvoiceId(id));
        return "invoice/detail";
    }

    // 4. Thanh toán hóa đơn (Tạo Payment)
    @PostMapping("/{id}/pay")
    public String payInvoice(
            @PathVariable("id") Integer invoiceId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("method") String method,
            @RequestParam(value = "referenceNo", required = false) String referenceNo,
            RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId);
            Payment payment = Payment.builder()
                    .invoice(invoice)
                    .amount(amount)
                    .method(method)
                    .referenceNo(referenceNo)
                    .build();

            paymentService.processPayment(payment);
            redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận thanh toán thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thanh toán: " + e.getMessage());
        }
        return "redirect:/invoices/" + invoiceId;
    }
}
