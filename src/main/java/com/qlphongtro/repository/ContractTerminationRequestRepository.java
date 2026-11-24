package com.qlphongtro.repository;

import com.qlphongtro.entity.ContractTerminationRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractTerminationRequestRepository extends JpaRepository<ContractTerminationRequest, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tr FROM ContractTerminationRequest tr JOIN FETCH tr.contract c " +
           "JOIN FETCH c.room r JOIN FETCH r.building b LEFT JOIN FETCH b.owner " +
           "LEFT JOIN FETCH tr.requester LEFT JOIN FETCH tr.responder " +
           "LEFT JOIN FETCH tr.settlement WHERE tr.terminationRequestId = :id")
    Optional<ContractTerminationRequest> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT tr FROM ContractTerminationRequest tr JOIN FETCH tr.contract c " +
           "JOIN FETCH c.room r JOIN FETCH r.building b LEFT JOIN FETCH b.owner " +
           "LEFT JOIN FETCH tr.requester LEFT JOIN FETCH tr.responder " +
           "LEFT JOIN FETCH tr.settlement WHERE b.owner.username = :username " +
           "ORDER BY tr.createdAt DESC")
    List<ContractTerminationRequest> findForLandlord(@Param("username") String username);

    @Query("SELECT tr FROM ContractTerminationRequest tr JOIN FETCH tr.contract c " +
           "JOIN FETCH c.room r JOIN FETCH r.building b LEFT JOIN FETCH b.owner " +
           "LEFT JOIN FETCH tr.requester LEFT JOIN FETCH tr.responder " +
           "LEFT JOIN FETCH tr.settlement WHERE tr.requester.userId = :userId " +
           "OR tr.responder.userId = :userId ORDER BY tr.createdAt DESC")
    List<ContractTerminationRequest> findForParticipant(@Param("userId") Integer userId);

    boolean existsByContract_ContractIdAndStatusIn(Integer contractId, List<String> statuses);

    List<ContractTerminationRequest> findByContract_ContractIdOrderByCreatedAtDesc(Integer contractId);
}
