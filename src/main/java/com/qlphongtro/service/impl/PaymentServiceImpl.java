package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Invoice;
import com.qlphongtro.entity.Payment;
import com.qlphongtro.repository.InvoiceRepository;
import com.qlphongtro.repository.PaymentRepository;
import com.qlphongtro.security.CurrentUser;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final BuildingAccess buildingAccess;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository, BuildingAccess buildingAccess) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.buildingAccess = buildingAccess;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        if (CurrentUser.isTenant()) {
            return paymentRepository.findAllForTenant(CurrentUser.username());
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : paymentRepository.findByInvoice_Contract_Room_Building_BuildingIdIn(buildingIds);
        }
        return paymentRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoiceId(Integer invoiceId) {
        if (CurrentUser.isTenant()) {
            return paymentRepository.findByInvoiceIdAndTenantUsername(invoiceId, CurrentUser.username());
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : paymentRepository.findByInvoice_InvoiceIdAndInvoice_Contract_Room_Building_BuildingIdIn(invoiceId, buildingIds);
        }
        return paymentRepository.findByInvoice_InvoiceId(invoiceId);
    }

    /**
     * Ghi nhận thanh toán cho hóa đơn.
     * Tự động kiểm tra: Nếu tổng số tiền đã thanh toán >= tổng tiền hóa đơn,
     * tự động chuyển trạng thái hóa đơn thành "Đã thanh toán" (PAID).
     */
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Payment processPayment(Payment payment) {
        if (buildingAccess.isStaff()) {
            Integer invoiceId = payment.getInvoice() == null ? null : payment.getInvoice().getInvoiceId();
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            if (invoiceId == null || buildingIds.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
            }
            Invoice scopedInvoice = invoiceRepository.findByInvoiceIdAndContract_Room_Building_BuildingIdIn(
                            invoiceId, buildingIds)
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
            payment.setInvoice(scopedInvoice);
        }
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
