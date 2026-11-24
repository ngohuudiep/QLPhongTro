package com.qlphongtro.service;

import com.qlphongtro.dto.InvoiceCreateRequest;

import com.qlphongtro.entity.Invoice;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface InvoiceService {

    List<Invoice> getAllInvoices();

    Invoice getInvoiceById(Integer id);

    List<Invoice> getInvoicesByContractId(Integer contractId);

    // Yêu cầu: Hàm sinh hóa đơn hàng tháng tính tổng: Tiền phòng + Tiền điện/nước (MeterReadings) + Tiền dịch vụ cố định (Room_Services)
    Invoice generateMonthlyInvoice(Integer contractId, Date month);

    // Phiên bản nạp chồng tiện lợi nhận LocalDate
    Invoice generateMonthlyInvoice(Integer contractId, LocalDate month);

    Invoice createMonthlyInvoice(InvoiceCreateRequest request, String landlordUsername);

    Invoice getInvoiceForLandlord(Integer invoiceId, String landlordUsername);

    void confirmInvoicePaidByLandlord(Integer invoiceId, String landlordUsername, String method, String referenceNo);

    void cancelInvoice(Integer invoiceId, String landlordUsername);

    Invoice saveInvoice(Invoice invoice);

    void deleteInvoice(Integer id);
}
