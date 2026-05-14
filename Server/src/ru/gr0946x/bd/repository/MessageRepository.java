package ru.gr0946x.bd.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gr0946x.bd.entity.Message;
import ru.gr0946x.bd.entity.User;
import ru.gr0946x.net.DeliveryStatus;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender = :u1 AND m.receiver = :u2) OR " +
            "(m.sender = :u2 AND m.receiver = :u1)) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findLastBetween(@Param("u1") User u1,
                                  @Param("u2") User u2,
                                  Pageable pageable);

    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender = :u1 AND m.receiver = :u2) OR " +
            "(m.sender = :u2 AND m.receiver = :u1)) AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :fragment, '%')) " +
            "ORDER BY m.sentAt DESC")
    List<Message> searchInConversation(@Param("fragment") String fragment,
                                       @Param("u1") User u1,
                                       @Param("u2") User u2);

    @Query("SELECT m FROM Message m WHERE m.receiver = :user OR m.sender = :user")
    List<Message> findByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.receiver IS NULL ORDER BY m.sentAt DESC")
    List<Message> findBroadcastMessages(Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.status = :status WHERE m.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") DeliveryStatus status);
}