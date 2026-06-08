package com.libseat.repository;

import com.libseat.entity.Notification;
import com.libseat.entity.NotificationType;
import com.libseat.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryTest extends RepositoryTestBase {

    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository         userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(user());

        // 2 条未读，1 条已读
        notificationRepository.save(notification(user, NotificationType.RESERVATION_SUCCESS, false));
        notificationRepository.save(notification(user, NotificationType.CHECKIN_REMINDER, false));
        notificationRepository.save(notification(user, NotificationType.NO_SHOW_WARNING, true));
    }

    @Test
    void countByUserIdAndIsRead_unread_returnsCorrectCount() {
        assertThat(notificationRepository.countByUserIdAndIsRead(user.getId(), false)).isEqualTo(2);
    }

    @Test
    void countByUserIdAndIsRead_read_returnsCorrectCount() {
        assertThat(notificationRepository.countByUserIdAndIsRead(user.getId(), true)).isEqualTo(1);
    }

    @Test
    void markAllRead_setsAllUnreadToRead() {
        int updated = notificationRepository.markAllRead(user.getId(), OffsetDateTime.now());
        assertThat(updated).isEqualTo(2);

        assertThat(notificationRepository.countByUserIdAndIsRead(user.getId(), false)).isZero();
        assertThat(notificationRepository.countByUserIdAndIsRead(user.getId(), true)).isEqualTo(3);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsAllNotifications() {
        assertThat(notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(3);
    }

    // ---- helpers ----

    private User user() {
        User u = new User();
        u.setUserNo("U" + System.nanoTime());
        u.setRealName("通知测试用户");
        u.setPasswordHash("hash");
        u.setEmail(System.nanoTime() + "@test.com");
        return u;
    }

    private Notification notification(User u, NotificationType type, boolean read) {
        Notification n = new Notification();
        n.setUser(u);
        n.setType(type);
        n.setTitle("测试通知");
        n.setContent("测试内容");
        n.setIsRead(read);
        return n;
    }
}
