package com.qlphongtro.repository;

import com.qlphongtro.entity.ContractTerminationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractTerminationHistoryRepository extends JpaRepository<ContractTerminationHistory, Integer> {

    List<ContractTerminationHistory> findByTerminationRequest_TerminationRequestIdOrderByHappenedAtAsc(Integer requestId);
}
