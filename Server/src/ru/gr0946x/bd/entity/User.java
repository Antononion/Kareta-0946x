package ru.gr0946x.bd.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = @Index(columnList = "nick", unique = true))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nick", nullable = false, unique = true)
    private String nick;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Message> receivedMessages = new ArrayList<>();

    public User() {}

    public User(String nick, String passwordHash) {
        this.nick = nick;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getNick() { return nick; }
    public String getPasswordHash() { return passwordHash; }
    public List<Message> getSentMessages() { return sentMessages; }
    public List<Message> getReceivedMessages() { return receivedMessages; }

    public void setNick(String nick) { this.nick = nick; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @Override
    public String toString() {
        return "User{id=" + id + ", nick='" + nick + "'}";
    }
}