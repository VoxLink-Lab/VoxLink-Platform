package voxlink.server.src.main.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {

    // Hash a password with a randomly generated salt
    public static String hashPassword(String password) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltString = Base64.getEncoder().encodeToString(salt);
        
        String hashString = hashWithSalt(password, saltString);
        return saltString + ":" + hashString; // Format: "salt:hash"
    }

    // Verify a password against a stored format "salt:hash"
    public static boolean verifyPassword(String password, String storedPasswordHash) {
        if (storedPasswordHash == null || !storedPasswordHash.contains(":")) {
            return false;
        }
        String[] parts = storedPasswordHash.split(":");
        if (parts.length != 2) {
            return false;
        }
        String salt = parts[0];
        String hash = parts[1];
        
        String newHash = hashWithSalt(password, salt);
        return newHash.equals(hash);
    }
    
    private static String hashWithSalt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
