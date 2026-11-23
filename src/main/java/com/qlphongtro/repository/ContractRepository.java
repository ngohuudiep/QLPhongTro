package com.qlphongtro.repository;

import com.qlphongtro.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer> {

    Optional<Contract> findByContractCode(String contractCode);

    boolean existsByContractCode(String contractCode);

    List<Contract> findByRoom_RoomId(Integer roomId);

    List<Contract> findDistinctByRoom_Building_BuildingIdIn(List<Integer> buildingIds);

    Optional<Contract> findByContractIdAndRoom_Building_BuildingIdIn(Integer contractId, List<Integer> buildingIds);

    List<Contract> findByRepresentative_TenantId(Integer representativeId);

    @Query("select distinct c from Contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where ru.username = :username or tu.username = :username")
    List<Contract> findAllForTenant(@Param("username") String username);

    @Query("select distinct c from Contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where c.contractId = :contractId and (ru.username = :username or tu.username = :username)")
    Optional<Contract> findByIdAndTenantUsername(@Param("contractId") Integer contractId, @Param("username") String username);

    @Query("select case when count(c) > 0 then true else false end from Contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where c.room.roomId = :roomId and (ru.username = :username or tu.username = :username)")
    boolean existsForTenantAndRoom(@Param("username") String username, @Param("roomId") Integer roomId);

    List<Contract> findByStatus(String status);
    List<Contract> findByTenantUser_UserId(Integer userId);
    List<Contract> findByTenantUser_UserIdOrderByContractIdDesc(Integer userId);
    List<Contract> findByRoom_Building_Owner_UsernameOrderByStartDateDesc(String username);
    Optional<Contract> findByRentalRequest_RequestId(Integer requestId);
    boolean existsByContractCodeAndContractIdNot(String contractCode, Integer contractId);
    boolean existsByRoom_RoomIdAndStatusIgnoreCase(Integer roomId, String status);
    boolean existsByRoom_RoomIdAndStatusInAndContractIdNot(Integer roomId, List<String> statuses, Integer contractId);
    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contract c WHERE c.contractId = :id")
    Optional<Contract> findByIdForUpdate(@Param("id") Integer id);
}