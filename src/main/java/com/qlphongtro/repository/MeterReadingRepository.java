package com.qlphongtro.repository;

import com.qlphongtro.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Integer> {

    // Lấy lịch sử chỉ số của một phòng sắp xếp theo tháng giảm dần
    List<MeterReading> findByRoom_RoomIdOrderByBillingMonthDesc(Integer roomId);

    // Tìm chỉ số của phòng theo tháng cụ thể
    Optional<MeterReading> findByRoom_RoomIdAndBillingMonth(Integer roomId, LocalDate billingMonth);

    @Query("select mr from MeterReading mr where exists (select c.contractId from Contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where c.room = mr.room and (ru.username = :username or tu.username = :username))")
    List<MeterReading> findAllForTenant(@Param("username") String username);

    @Query("select mr from MeterReading mr where mr.readingId = :readingId and exists (select c.contractId from Contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where c.room = mr.room and (ru.username = :username or tu.username = :username))")
    Optional<MeterReading> findByIdAndTenantUsername(@Param("readingId") Integer readingId, @Param("username") String username);

    @Query("select mr from MeterReading mr where mr.room.roomId = :roomId and exists (select c.contractId from Contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where c.room = mr.room and (ru.username = :username or tu.username = :username)) order by mr.billingMonth desc")
    List<MeterReading> findByRoomIdAndTenantUsername(@Param("roomId") Integer roomId, @Param("username") String username);

    @Query("select mr from MeterReading mr where mr.room.roomId = :roomId and mr.billingMonth = :billingMonth and exists (select c.contractId from Contract c left join c.representative rep left join rep.user ru left join c.contractTenants ct left join ct.tenant t left join t.user tu where c.room = mr.room and (ru.username = :username or tu.username = :username))")
    Optional<MeterReading> findByRoomIdAndBillingMonthAndTenantUsername(@Param("roomId") Integer roomId, @Param("billingMonth") LocalDate billingMonth, @Param("username") String username);
}
