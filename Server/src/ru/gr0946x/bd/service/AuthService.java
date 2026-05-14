package ru.gr0946x.bd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gr0946x.bd.entity.User;
import ru.gr0946x.bd.repository.UserRepository;
import ru.gr0946x.bd.util.PasswordHasher;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private static final Pattern NICK_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{2,19}$");

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean isNickValid(String nick) {
        return nick != null && NICK_PATTERN.matcher(nick).matches();
    }

    @Transactional(readOnly = true)
    public boolean isNickTaken(String nick) {
        return userRepository.existsByNickIgnoreCase(nick);
    }

    @Transactional
    public User register(String nick, String password) {
        String normalized = nick.trim();
        if (!isNickValid(normalized)) {
            throw new IllegalArgumentException("Ник должен начинаться с буквы и содержать 3-20 символов");
        }
        if (isNickTaken(normalized)) {
            throw new IllegalArgumentException("Ник " + normalized + " уже занят");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов");
        }
        String passwordHash = PasswordHasher.hashPassword(password);
        return userRepository.save(new User(normalized, passwordHash));
    }

    @Transactional
    public User login(String nick, String password) {
        Optional<User> userOpt = userRepository.findByNickIgnoreCase(nick);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordHasher.checkPassword(password, user.getPasswordHash())) {
                return user;
            }
        }
        throw new IllegalArgumentException("Неверный ник или пароль");
    }

    @Transactional(readOnly = true)
    public Optional<User> findByNick(String nick) {
        return userRepository.findByNickIgnoreCase(nick);
    }
}