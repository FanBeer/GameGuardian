package com.nbbackup.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtil {
    private static final String SECRET_KEY = "NetworkDeviceBackup_Secret_Key_2024";

    public static String encrypt(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String salted = SECRET_KEY + password;
            byte[] hash = md.digest(salted.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    public static boolean verify(String inputPassword, String storedPassword) {
        if (inputPassword == null || storedPassword == null) {
            return false;
        }
        return encrypt(inputPassword).equals(storedPassword);
    }
}
