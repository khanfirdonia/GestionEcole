package services;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import dao.DatabaseConnection;

/**
 * Service d'audit pour enregistrer les actions importantes
 * Logs: authentification, déconnexion, modifications critiques
 */
public class AuditService {
    private static final Logger logger = Logger.getLogger(AuditService.class.getName());
    private static final String TABLE = "audit_logs";
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Enregistrer une connexion utilisateur
     */
    public static void logLogin(int userId, String username) {
        try {
            logAction("LOGIN", userId, username, "Connexion réussie");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'enregistrement de la connexion", e);
        }
    }

    /**
     * Enregistrer une déconnexion utilisateur
     */
    public static void logLogout(int userId, String username) {
        try {
            logAction("LOGOUT", userId, username, "Déconnexion");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'enregistrement de la déconnexion", e);
        }
    }

    /**
     * Enregistrer l'approbation d'un élève
     */
    public static void logStudentApproval(int userId, String username, int studentId, String studentName) {
        try {
            logAction("STUDENT_APPROVAL", userId, username, "Approbation de l'élève: " + studentName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'enregistrement de l'approbation d'élève", e);
        }
    }

    /**
     * Enregistrer la saisie de notes
     */
    public static void logGradeEntry(int userId, String username, String gradeInfo) {
        try {
            logAction("GRADE_ENTRY", userId, username, "Saisie de note: " + gradeInfo);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'enregistrement de la saisie de note", e);
        }
    }

    /**
     * Enregistrer la génération d'un bulletin
     */
    public static void logBulletinGeneration(int userId, String username, String bulletinInfo) {
        try {
            logAction("BULLETIN_GENERATION", userId, username, "Génération de bulletin: " + bulletinInfo);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors de l'enregistrement de la génération de bulletin", e);
        }
    }

    /**
     * Enregistrer une action générique
     */
    private static void logAction(String actionType, Integer userId, String username, String details) {
        String sql = "INSERT INTO " + TABLE + " (user_id, username, action_type, details, action_date) " +
                    "VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (userId == null || userId <= 0) {
                stmt.setNull(1, Types.INTEGER);
            } else {
                stmt.setInt(1, userId);
            }
            stmt.setString(2, username);
            stmt.setString(3, actionType);
            stmt.setString(4, details);
            
            stmt.executeUpdate();
            logger.fine("Audit log enregistré: " + actionType);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Erreur lors de l'enregistrement de l'audit", e);
        }
    }
}
