package com.hawkeye.auth.business.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class GeneratePassword {
    @Test
    public void generatePassword() {
        // 运行此方法生成新的密码哈希
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode("password");
        System.out.println("加密后的密码: " + encodedPassword);

    }
}
