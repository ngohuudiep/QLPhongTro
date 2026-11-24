package com.qlphongtro.repository;

import com.qlphongtro.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {

    List<UserNotification> findByRecipient_UserIdOrderByCreatedAtDesc(Integer userId);

    Optional<UserNotification> findByNotificationIdAndRecipient_UserId(Integer notificationId, Integer userId);

    long countByRecipient_UserIdAndReadAtIsNull(Integer userId);
}
