package utils;

import java.util.regex.Pattern;

/**
 * Classe utilitaire pour la validation des données
 */
public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9]{8,15}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{3,20}$"
    );

    /**
     * Valider un email
     */
    public static boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Valider un numéro de téléphone
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && !phone.isEmpty() && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Valider un nom d'utilisateur
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Vérifier si une chaîne n'est pas vide
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Vérifier si une chaîne est vide
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Valider une note (0-20)
     */
    public static boolean isValidGrade(double grade) {
        return grade >= Constants.MIN_GRADE && grade <= Constants.MAX_GRADE;
    }

    /**
     * Valider une année
     */
    public static boolean isValidYear(int year) {
        int currentYear = java.time.Year.now().getValue();
        return year >= 2000 && year <= currentYear + 10;
    }

    /**
     * Valider une date au format dd/MM/yyyy
     */
    public static boolean isValidDateFormat(String date) {
        if (isEmpty(date)) {
            return false;
        }
        return date.matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[012])/\\d{4}$");
    }

    /**
     * Vérifier si deux mots de passe correspondent
     */
    public static boolean passwordsMatch(String password1, String password2) {
        return password1 != null && password1.equals(password2);
    }

    /**
     * Valider les dimensions du nom complet
     */
    public static boolean isValidFullName(String firstName, String lastName) {
        return isNotEmpty(firstName) && isNotEmpty(lastName) &&
               firstName.length() >= 2 && lastName.length() >= 2 &&
               firstName.length() <= 50 && lastName.length() <= 50;
    }

    /**
     * Valider un matricule
     */
    public static boolean isValidMatricule(String matricule) {
        return isNotEmpty(matricule) && matricule.length() >= 4 && matricule.length() <= 20;
    }

    /**
     * Vérifier si une chaîne contient seulement des lettres
     */
    public static boolean isAlpha(String str) {
        return str != null && str.matches("^[a-zA-Zàâäéèêëïîôùûüœæç ]*$");
    }

    /**
     * Vérifier si une chaîne contient seulement des chiffres
     */
    public static boolean isNumeric(String str) {
        return str != null && str.matches("^[0-9]*$");
    }

    /**
     * Vérifier si une chaîne est alphanumérique
     */
    public static boolean isAlphanumeric(String str) {
        return str != null && str.matches("^[a-zA-Z0-9]*$");
    }
}
