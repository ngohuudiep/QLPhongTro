package com.qlphongtro.repository;

import com.qlphongtro.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    List<Invoice> findByContract_ContractId(Integer contractId);

    List<Invoice> findByStatus(String status);
}
