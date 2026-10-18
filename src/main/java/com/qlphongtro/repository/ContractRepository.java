package com.qlphongtro.repository;

import com.qlphongtro.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer> {

    Optional<Contract> findByContractCode(String contractCode);

    boolean existsByContractCode(String contractCode);

    List<Contract> findByRoom_RoomId(Integer roomId);

    List<Contract> findByRepresentative_TenantId(Integer representativeId);

    List<Contract> findByStatus(String status);
}
