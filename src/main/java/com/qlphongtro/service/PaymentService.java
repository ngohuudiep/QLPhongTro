package com.qlphongtro.service;

import com.qlphongtro.entity.Payment;
import java.util.List;

public interface PaymentService {

    List<Payment> getAllPayments();

    List<Payment> getPaymentsByInvoiceId(Integer invoiceId);

    Payment processPayment(Payment payment);
}
