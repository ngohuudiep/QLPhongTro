package com.qlphongtro.repository;

import com.qlphongtro.entity.DepositConfirmationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepositConfirmationRequestRepository
        extends JpaRepository<DepositConfirmationRequest, Integer> {

    boolean existsByContract_ContractIdAndStatusIgnoreCase(Integer contractId, String status);

    @Query("SELECT request.status FROM DepositConfirmationRequest request WHERE request.requestId = :requestId")
    Optional<String> findStatusByRequestId(@Param("requestId") Integer requestId);

    Optional<DepositConfirmationRequest> findTopByContract_ContractIdAndTenantUser_UserIdOrderByRequestedAtDesc(
            Integer contractId, Integer tenantUserId);

    Optional<DepositConfirmationRequest> findTopByContract_ContractIdAndLandlordUser_UserIdOrderByRequestedAtDesc(
            Integer contractId, Integer landlordUserId);

    List<DepositConfirmationRequest> findByContract_ContractIdAndTenantUser_UserIdOrderByRequestedAtDesc(
            Integer contractId, Integer tenantUserId);

    List<DepositConfirmationRequest> findByLandlordUser_UserIdOrderByRequestedAtDesc(Integer landlordUserId);

    List<DepositConfirmationRequest> findByLandlordUser_UserIdAndStatusIgnoreCaseOrderByRequestedAtDesc(
            Integer landlordUserId, String status);
}
