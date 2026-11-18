package services;

import dao.DatabaseConnection;
import models.Teacher;
import models.User;
import utils.Constants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service admin pour creer et modifier un enseignant avec son compte utilisateur.
 */
public class TeacherManagementService {
    private static final Logger logger = Logger.getLogger(TeacherManagementService.class.getName());

    public int createTeacher(User user, Teacher teacher) {
        if (user == null || teacher == null) {
            return -1;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId = insertUser(conn, user);
                if (userId <= 0) {
                    conn.rollback();
                    return -1;
                }
                user.setId(userId);

                int teacherId = insertTeacher(conn, teacher, userId);
                if (teacherId <= 0) {
                    conn.rollback();
                    return -1;
                }

                conn.commit();
                return teacherId;
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Erreur lors de la creation enseignant", e);
                return -1;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur connexion DB", e);
            return -1;
        }
    }

    public boolean updateTeacher(User user, Teacher teacher) {
        if (user == null || teacher == null || user.getId() <= 0 || teacher.getId() <= 0) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!updateUser(conn, user)) {
                    conn.rollback();
                    return false;
                }
                if (!updateTeacherRow(conn, teacher)) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Erreur lors de la mise a jour enseignant", e);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur connexion DB", e);
            return false;
        }
    }

    private int insertUser(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, first_name, last_name, email, phone, is_active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, Constants.ROLE_TEACHER);
            stmt.setString(4, user.getFirstName());
            stmt.setString(5, user.getLastName());
            stmt.setString(6, user.getEmail());
            stmt.setString(7, user.getPhone());
            stmt.setBoolean(8, user.isActive());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    private int insertTeacher(Connection conn, Teacher teacher, int userId) throws SQLException {
        String sql = "INSERT INTO teachers (user_id, matricule, specialization, qualifications, department, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, teacher.getMatricule());
            stmt.setString(3, teacher.getSpecialization());
            stmt.setString(4, teacher.getQualifications());
            stmt.setString(5, teacher.getDepartment());
            stmt.setString(6, teacher.getPhone());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    private boolean updateUser(Connection conn, User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, password = ?, role = ?, first_name = ?, last_name = ?, " +
                "email = ?, phone = ?, is_active = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, Constants.ROLE_TEACHER);
            stmt.setString(4, user.getFirstName());
            stmt.setString(5, user.getLastName());
            stmt.setString(6, user.getEmail());
            stmt.setString(7, user.getPhone());
            stmt.setBoolean(8, user.isActive());
            stmt.setInt(9, user.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    private boolean updateTeacherRow(Connection conn, Teacher teacher) throws SQLException {
        String sql = "UPDATE teachers SET matricule = ?, specialization = ?, qualifications = ?, department = ?, phone = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teacher.getMatricule());
            stmt.setString(2, teacher.getSpecialization());
            stmt.setString(3, teacher.getQualifications());
            stmt.setString(4, teacher.getDepartment());
            stmt.setString(5, teacher.getPhone());
            stmt.setInt(6, teacher.getId());
            return stmt.executeUpdate() > 0;
        }
    }
}
