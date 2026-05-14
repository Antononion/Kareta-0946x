package ru.gr0946x.bd.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.gr0946x.bd.entity.Message;
import ru.gr0946x.bd.entity.User;
import ru.gr0946x.bd.repository.MessageRepository;
import ru.gr0946x.net.DeliveryStatus;
import ru.gr0946x.net.MessageDto;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChatService {
    private final MessageRepository messageRepository;
    private static final int HISTORY_LIMIT = 50;
    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    public ChatService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Message saveMessage(User sender, User receiver, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }
        Message msg = new Message(sender, receiver, content.trim());
        return messageRepository.save(msg);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getHistory(User user1, User user2) {
        var pageable = PageRequest.of(0, HISTORY_LIMIT);
        return messageRepository.findLastBetween(user1, user2, pageable)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageDto> searchMessages(User user1, User user2, String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return List.of();
        }
        return messageRepository.searchInConversation(fragment.trim(), user1, user2)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getBroadcastMessages() {
        var pageable = PageRequest.of(0, HISTORY_LIMIT);
        return messageRepository.findBroadcastMessages(pageable)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void markAsDelivered(Long messageId) {
        messageRepository.updateStatus(messageId, DeliveryStatus.DELIVERED);
    }

    @Transactional
    public void markAsRead(Long messageId) {
        messageRepository.updateStatus(messageId, DeliveryStatus.READ);
    }

    private MessageDto toDto(Message m) {
        return new MessageDto(
                m.getId(),
                m.getSender().getNick(),
                m.getReceiver() != null ? m.getReceiver().getNick() : null,
                m.getContent(),
                m.getSentAt(),
                m.getStatus()
        );
    }

    public String formatTimestamp(java.time.LocalDateTime dt) {
        return dt.format(DT_FORMATTER);
    }
}