package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Classe utilitaire pour la gestion des mots de passe
 */
public class PasswordUtils {
    private static final Logger logger = Logger.getLogger(PasswordUtils.class.getName());

    /**
     * Hacher un mot de passe en utilisant SHA-256
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Erreur lors du hachage du mot de passe", e);
            return null;
        }
    }

    /**
     * Vérifier si un mot de passe correspond au hash
     */
    public static boolean verifyPassword(String password, String hash) {
        String passwordHash = hashPassword(password);
        return passwordHash != null && passwordHash.equals(hash);
    }

    /**
     * Générer un mot de passe aléatoire
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }

    /**
     * Valider la force d'un mot de passe
     * Minimum: 8 caractères, 1 majuscule, 1 minuscule, 1 chiffre
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        
        return hasUpperCase && hasLowerCase && hasDigit;
    }

    /**
     * Obtenir la force du mot de passe
     */
    public static String getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Très faible";
        }
        
        int strength = 0;
        
        // Longueur
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        
        // Caractères
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*\\d.*")) strength++;
        if (password.matches(".*[!@#$%^&*].*")) strength++;
        
        return switch (strength) {
            case 0, 1 -> "Très faible";
            case 2, 3 -> "Faible";
            case 4 -> "Moyen";
            case 5 -> "Bon";
            default -> "Très bon";
        };
    }
}
