package com.xxl.job.admin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {
    @Test
    void acceptsStrongPasswordAndRejectsBootstrapStylePasswords() {
        assertTrue(PasswordPolicy.isStrong("N0t-Guessable!42", "admin"));
        assertFalse(PasswordPolicy.isStrong("123456", "admin"));
        assertFalse(PasswordPolicy.isStrong("Admin-Password!42", "admin"));
    }
}
