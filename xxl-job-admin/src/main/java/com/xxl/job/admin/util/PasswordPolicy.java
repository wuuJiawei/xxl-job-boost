package com.xxl.job.admin.util;

import java.util.regex.Pattern;
import java.util.Locale;

/** Password rules shared by login administration and forced password changes. */
public final class PasswordPolicy {
    private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^A-Za-z\\d].*");

    private PasswordPolicy() {
    }

    public static boolean isStrong(String password, String username) {
        if (password == null || password.length() < 12 || password.length() > 64 || password.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        if (!UPPERCASE.matcher(password).matches()
                || !LOWERCASE.matcher(password).matches()
                || !DIGIT.matcher(password).matches()
                || !SPECIAL.matcher(password).matches()) {
            return false;
        }
        return username == null || username.isBlank()
                || !password.toLowerCase(Locale.ROOT).contains(username.trim().toLowerCase(Locale.ROOT));
    }

    public static String validationMessage() {
        return "密码必须为12-64位，且同时包含大写字母、小写字母、数字和特殊字符，不能包含用户名或空白字符。弱密码容易被暴力破解，继续使用可能导致管理后台被攻击。";
    }
}
