package com.xueying.jobapplicationtracker.utils;

import org.apache.commons.codec.digest.DigestUtils;

public final class MD5Util {
    private MD5Util() {
    }

    public static String generate(String password, String salt) {
        return DigestUtils.md5Hex(password + salt);
    }

    public static boolean verify(String password, String salt, String hashedPassword) {
        return generate(password, salt).equals(hashedPassword);
    }
}

