package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Payment;
import com.qlphongtro.repository.InvoiceRepository;
import com.qlphongtro.repository.PaymentRepository;
import com.qlphongtro.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoiceId(Integer invoiceId) {
        return paymentRepository.findByInvoice_InvoiceId(invoiceId);
    }

    /**
     * Ghi nhận thanh toán cho hóa đơn.
     * Tự động kiểm tra: Nếu tổng số tiền đã thanh toán >= tổng tiền hóa đơn,
     * tự động chuyển trạng thái hóa đơn thành "Đã thanh toán" (PAID).
     */
    @Override
    public Payment processPayment(Payment payment) {
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Kiểm tra và cập nhật trạng thái hóa đơn
        if (savedPayment.getInvoice() != null) {
            Integer invoiceId = savedPayment.getInvoice().getInvoiceId();
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);

            if (invoice != null && invoice.getTotalAmount() != null) {
                List<Payment> allPayments = paymentRepository.findByInvoice_InvoiceId(invoiceId);
                BigDecimal totalPaid = allPayments.stream()
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
                    invoice.setStatus("Đã thanh toán");
                    invoiceRepository.save(invoice);
                }
            }
        }

        return savedPayment;
    }
}
