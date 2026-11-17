package com.qlphongtro.repository;

import com.qlphongtro.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    @Query("select distinct i from Invoice i left join i.contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where ru.username = :username or tu.username = :username")
    List<Invoice> findAllForTenant(@Param("username") String username);

    @Query("select distinct i from Invoice i left join i.contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where i.invoiceId = :invoiceId and (ru.username = :username or tu.username = :username)")
    Optional<Invoice> findByIdAndTenantUsername(@Param("invoiceId") Integer invoiceId, @Param("username") String username);

    @Query("select distinct i from Invoice i left join i.contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where c.contractId = :contractId and (ru.username = :username or tu.username = :username)")
    List<Invoice> findByContractIdAndTenantUsername(@Param("contractId") Integer contractId, @Param("username") String username);

    List<Invoice> findByContract_ContractId(Integer contractId);

    List<Invoice> findByStatus(String status);
}
