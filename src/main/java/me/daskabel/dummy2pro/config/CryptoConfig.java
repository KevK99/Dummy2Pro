package me.daskabel.dummy2pro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/*
liefert den BCryptPasswordEncoder als Bean, damit du man im UserService injizieren kann
 */

@Configuration
public class CryptoConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
