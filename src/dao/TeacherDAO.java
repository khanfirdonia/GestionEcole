package dao;

import models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO pour la gestion des enseignants
 * Gère les opérations CRUD sur la table teachers
 */
public class TeacherDAO {
    private static final Logger logger = Logger.getLogger(TeacherDAO.class.getName());
    private static final String TABLE = "teachers";

    /**
     * Récupérer tous les enseignants
     */
    public List<Teacher> getAllTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT t.*, t.id AS teacher_id, u.id AS user_id, u.* FROM " + TABLE + " t JOIN users u ON t.user_id = u.id " +
                    "WHERE u.is_active = 1 ORDER BY u.last_name, u.first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                teachers.add(mapResultSetToTeacher(rs));
            }
            logger.info("Récupération de " + teachers.size() + " enseignants");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des enseignants", e);
        }
        return teachers;
    }

    /**
     * Récupérer un enseignant par son ID
     */
    public Teacher getTeacherById(int id) {
        String sql = "SELECT t.*, t.id AS teacher_id, u.id AS user_id, u.* FROM " + TABLE + " t JOIN users u ON t.user_id = u.id WHERE t.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToTeacher(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'enseignant ID: " + id, e);
        }
        return null;
    }

    /**
     * Récupérer un enseignant par son User ID
     */
    public Teacher getTeacherByUserId(int userId) {
        String sql = "SELECT t.*, t.id AS teacher_id, u.id AS user_id, u.* FROM " + TABLE + " t JOIN users u ON t.user_id = u.id WHERE t.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToTeacher(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'enseignant", e);
        }
        return null;
    }

    /**
     * Sauvegarder un nouvel enseignant
     */
    public int saveTeacher(Teacher teacher) {
        String sql = "INSERT INTO " + TABLE + " (user_id, matricule, specialization, qualifications, department, phone) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, teacher.getUser().getId());
            stmt.setString(2, teacher.getMatricule());
            stmt.setString(3, teacher.getSpecialization());
            stmt.setString(4, teacher.getQualifications());
            stmt.setString(5, teacher.getDepartment());
            stmt.setString(6, teacher.getPhone());
            
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int teacherId = generatedKeys.getInt(1);
                    logger.info("Enseignant créé avec succès: ID = " + teacherId);
                    return teacherId;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création de l'enseignant", e);
        }
        return -1;
    }

    /**
     * Mettre à jour un enseignant existant
     */
    public boolean updateTeacher(Teacher teacher) {
        String sql = "UPDATE " + TABLE + " SET matricule = ?, specialization = ?, qualifications = ?, " +
                    "department = ?, phone = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, teacher.getMatricule());
            stmt.setString(2, teacher.getSpecialization());
            stmt.setString(3, teacher.getQualifications());
            stmt.setString(4, teacher.getDepartment());
            stmt.setString(5, teacher.getPhone());
            stmt.setInt(6, teacher.getId());
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Enseignant mis à jour: ID = " + teacher.getId());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour de l'enseignant", e);
        }
        return false;
    }

    /**
     * Convertir une ligne ResultSet en objet Teacher
     */
    private Teacher mapResultSetToTeacher(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setRole(rs.getString("role"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setActive(rs.getBoolean("is_active"));
        
        Teacher teacher = new Teacher();
        teacher.setId(rs.getInt("teacher_id"));
        teacher.setUser(user);
        teacher.setMatricule(rs.getString("matricule"));
        teacher.setSpecialization(rs.getString("specialization"));
        teacher.setQualifications(rs.getString("qualifications"));
        teacher.setDepartment(rs.getString("department"));
        teacher.setPhone(rs.getString("phone"));
        
        return teacher;
    }
}
