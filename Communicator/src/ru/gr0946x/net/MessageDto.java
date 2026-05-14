package ru.gr0946x.net;

import java.time.LocalDateTime;

public record MessageDto(
        Long id,
        String fromNick,
        String toNick,
        String content,
        LocalDateTime sentAt,
        DeliveryStatus status
) {
    public static MessageDto broadcast(String fromNick, String content) {
        return new MessageDto(null, fromNick, null, content, LocalDateTime.now(), DeliveryStatus.SENT);
    }

    public static MessageDto personal(String fromNick, String toNick, String content) {
        return new MessageDto(null, fromNick, toNick, content, LocalDateTime.now(), DeliveryStatus.SENT);
    }
}