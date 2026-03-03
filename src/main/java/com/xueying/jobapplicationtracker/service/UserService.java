package com.xueying.jobapplicationtracker.service;

import com.xueying.jobapplicationtracker.entity.User;

/**
 * Provides account and current-session user helpers.
 */
public interface UserService {
    User findByEmail(String email);

    boolean register(String email, String plainPassword);

    boolean verifyPassword(User user, String plainPassword);

    Long currentUserId();

    String currentUserEmail();
}

