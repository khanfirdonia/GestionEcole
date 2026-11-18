package dao;

import models.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO pour la gestion des utilisateurs
 * Gère les opérations CRUD sur la table users
 */
public class UserDAO {
    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());
    private static final String TABLE = "users";

    /**
     * Authentifier un utilisateur avec son nom d'utilisateur et mot de passe
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM " + TABLE + " WHERE username = ? AND password = ? AND is_active = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                logger.info("Utilisateur authentifié : " + username);
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'authentification", e);
        }
        return null;
    }

    /**
     * Récupérer tous les utilisateurs
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " ORDER BY last_name, first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            logger.info("Récupération de " + users.size() + " utilisateurs");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des utilisateurs", e);
        }
        return users;
    }

    /**
     * Récupérer un utilisateur par son ID
     */
    public User getUserById(int id) {
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'utilisateur ID: " + id, e);
        }
        return null;
    }

    /**
     * Récupérer un utilisateur par son nom d'utilisateur
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM " + TABLE + " WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'utilisateur: " + username, e);
        }
        return null;
    }

    /**
     * Récupérer tous les utilisateurs d'un rôle spécifique
     */
    public List<User> getUsersByRole(String role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE role = ? ORDER BY last_name, first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, role);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des utilisateurs par rôle: " + role, e);
        }
        return users;
    }

    /**
     * Sauvegarder un nouvel utilisateur
     */
    public int saveUser(User user) {
        String sql = "INSERT INTO " + TABLE + " (username, password, role, first_name, last_name, email, phone, is_active, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getFirstName());
            stmt.setString(5, user.getLastName());
            stmt.setString(6, user.getEmail());
            stmt.setString(7, user.getPhone());
            stmt.setBoolean(8, user.isActive());
            
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    logger.info("Utilisateur créé avec succès: ID = " + userId);
                    return userId;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création de l'utilisateur", e);
        }
        return -1;
    }

    /**
     * Mettre à jour un utilisateur existant
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE " + TABLE + " SET username = ?, password = ?, role = ?, first_name = ?, " +
                    "last_name = ?, email = ?, phone = ?, is_active = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getFirstName());
            stmt.setString(5, user.getLastName());
            stmt.setString(6, user.getEmail());
            stmt.setString(7, user.getPhone());
            stmt.setBoolean(8, user.isActive());
            stmt.setInt(9, user.getId());
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Utilisateur mis à jour: ID = " + user.getId());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour de l'utilisateur ID: " + user.getId(), e);
        }
        return false;
    }

    /**
     * Supprimer un utilisateur
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM " + TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                logger.info("Utilisateur supprimé: ID = " + userId);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la suppression de l'utilisateur ID: " + userId, e);
        }
        return false;
    }

    /**
     * Vérifier si un nom d'utilisateur existe
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la vérification du nom d'utilisateur", e);
        }
        return false;
    }

    /**
     * Compter les utilisateurs par rôle
     */
    public int countUsersByRole(String role) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, role);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors du comptage des utilisateurs", e);
        }
        return 0;
    }

    /**
     * Convertir une ligne ResultSet en objet User
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setActive(rs.getBoolean("is_active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return user;
    }
}
