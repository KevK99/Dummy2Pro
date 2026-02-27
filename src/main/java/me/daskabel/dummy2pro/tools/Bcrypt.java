package me.daskabel.dummy2pro.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Damit man manuell Sachen encrypten kann. Z.B. zu Testzwecken
 */

public class Bcrypt {
    public static void main(String[] args) {
        String raw = "ASDFqwer1234!";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = encoder.encode(raw);
        System.out.println("hash=" + hash);
        System.out.println("matches=" + encoder.matches(raw, hash));
    }
}
