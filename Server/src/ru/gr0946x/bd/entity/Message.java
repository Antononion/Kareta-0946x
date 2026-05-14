package ru.gr0946x.bd.entity;

import jakarta.persistence.*;
import ru.gr0946x.net.DeliveryStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
        @Index(columnList = "sender_id, receiver_id, sent_at"),
        @Index(columnList = "content", name = "idx_content_search")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status;

    public Message() {}

    public Message(User sender, User receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.status = DeliveryStatus.SENT;
    }

    public Long getId() { return id; }
    public User getSender() { return sender; }
    public User getReceiver() { return receiver; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public DeliveryStatus getStatus() { return status; }

    public void setStatus(DeliveryStatus status) { this.status = status; }

    public boolean isBroadcast() {
        return receiver == null;
    }

    @Override
    public String toString() {
        return "Message{id=" + id + ", from=" + sender.getNick() +
                ", to=" + (receiver != null ? receiver.getNick() : "ALL") +
                ", content='" + content + "', status=" + status + "}";
    }
}