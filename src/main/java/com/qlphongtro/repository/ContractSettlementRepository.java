package com.qlphongtro.repository;

import com.qlphongtro.entity.ContractSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractSettlementRepository extends JpaRepository<ContractSettlement, Integer> {

    Optional<ContractSettlement> findByTerminationRequest_TerminationRequestId(Integer requestId);
}
