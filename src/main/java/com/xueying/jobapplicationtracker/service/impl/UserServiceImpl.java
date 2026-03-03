package com.xueying.jobapplicationtracker.service.impl;

import com.xueying.jobapplicationtracker.entity.User;
import com.xueying.jobapplicationtracker.mapper.UserMapper;
import com.xueying.jobapplicationtracker.service.UserService;
import com.xueying.jobapplicationtracker.utils.MD5Util;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * User service with BCrypt primary hashing and legacy MD5 compatibility fallback.
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public User findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            return null;
        }
        return userMapper.findByEmail(normalizedEmail);
    }

    @Override
    public boolean register(String email, String plainPassword) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPassword = plainPassword == null ? "" : plainPassword.trim();
        if (!isValidEmail(normalizedEmail) || normalizedPassword.length() < 6) {
            return false;
        }
        if (findByEmail(normalizedEmail) != null) {
            return false;
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedEmail);
        user.setSalt("");
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        return userMapper.insert(user) > 0;
    }

    @Override
    /**
     * Verifies password against BCrypt hash and supports legacy MD5+salt records for migration safety.
     */
    public boolean verifyPassword(User user, String plainPassword) {
        if (user == null) {
            return false;
        }
        String normalizedPassword = plainPassword == null ? "" : plainPassword;
        String storedPassword = user.getPassword() == null ? "" : user.getPassword();
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(normalizedPassword, storedPassword);
        }
        String salt = user.getSalt() == null ? "" : user.getSalt();
        if (!salt.isEmpty()) {
            return MD5Util.verify(normalizedPassword, salt, storedPassword);
        }
        return false;
    }

    @Override
    public Long currentUserId() {
        User user = findByEmail(currentUserEmail());
        return user == null ? null : user.getId();
    }

    @Override
    public String currentUserEmail() {
        Subject subject = SecurityUtils.getSubject();
        if (subject == null) {
            return "";
        }
        Object principal = subject.getPrincipal();
        return principal == null ? "" : normalizeEmail(principal.toString());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        return !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
    }
}

