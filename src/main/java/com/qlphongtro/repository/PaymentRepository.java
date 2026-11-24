package com.qlphongtro.repository;

import com.qlphongtro.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByInvoice_InvoiceId(Integer invoiceId);

    List<Payment> findByInvoice_Contract_Room_Building_BuildingIdIn(List<Integer> buildingIds);

    List<Payment> findByInvoice_InvoiceIdAndInvoice_Contract_Room_Building_BuildingIdIn(Integer invoiceId, List<Integer> buildingIds);

    @Query("select distinct p from Payment p left join p.invoice i left join i.contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where ru.username = :username or tu.username = :username")
    List<Payment> findAllForTenant(@Param("username") String username);

    @Query("select distinct p from Payment p left join p.invoice i left join i.contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where p.paymentId = :paymentId and (ru.username = :username or tu.username = :username)")
    Optional<Payment> findByIdAndTenantUsername(@Param("paymentId") Integer paymentId, @Param("username") String username);

    @Query("select distinct p from Payment p left join p.invoice i left join i.contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where i.invoiceId = :invoiceId and (ru.username = :username or tu.username = :username)")
    List<Payment> findByInvoiceIdAndTenantUsername(@Param("invoiceId") Integer invoiceId, @Param("username") String username);
    Optional<Payment> findByTransactionCode(String transactionCode);
    List<Payment> findByContract_ContractIdOrderByCreatedAtDesc(Integer contractId);
    boolean existsByContract_ContractIdAndStatusIgnoreCase(Integer contractId, String status);

    @Query("SELECT p FROM Payment p WHERE p.contract.contractId = :contractId AND UPPER(COALESCE(p.provider, '')) = 'DEPOSIT' ORDER BY p.createdAt DESC")
    List<Payment> findDepositPaymentsByContractId(@Param("contractId") Integer contractId);

    @Query("SELECT (COUNT(p) > 0) FROM Payment p WHERE p.contract.contractId = :contractId AND UPPER(COALESCE(p.provider, '')) = 'DEPOSIT' AND UPPER(COALESCE(p.status, '')) = UPPER(:status)")
    boolean existsDepositPaymentByContractIdAndStatus(@Param("contractId") Integer contractId, @Param("status") String status);

    @Query("SELECT (COUNT(p) > 0) FROM Payment p WHERE p.contract.contractId = :contractId AND UPPER(COALESCE(p.provider, '')) = 'DEPOSIT' AND UPPER(COALESCE(p.status, '')) IN ('SUCCESS', 'PENDING')")
    boolean existsUnresolvedDepositPaymentByContractId(@Param("contractId") Integer contractId);
}