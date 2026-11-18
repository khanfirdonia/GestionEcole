package services;

import dao.DatabaseConnection;
import dao.InscriptionDAO;
import models.SchoolClass;
import models.Student;
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
 * Service "admin" pour creer / modifier / desactiver un eleve
 * en gardant la coherence User <-> Student <-> Inscription (annee active).
 */
public class StudentManagementService {
    private static final Logger logger = Logger.getLogger(StudentManagementService.class.getName());
    private final InscriptionDAO inscriptionDAO = new InscriptionDAO();

    /**
     * Cree un eleve (user + student). Optionnellement, cree/met a jour l'inscription de l'annee active.
     *
     * @return l'id du student cree, ou -1
     */
    public int createStudent(User user, Student student, boolean createActiveYearInscription) {
        if (user == null || student == null || student.getLevel() == null) {
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

                int studentId = insertStudent(conn, student, userId);
                if (studentId <= 0) {
                    conn.rollback();
                    return -1;
                }

                if (createActiveYearInscription) {
                    upsertActiveYearInscription(conn, studentId, student.getLevel(), deriveInscriptionStatus(student.getStatus()));
                }

                conn.commit();
                return studentId;
            } catch (IllegalStateException e) {
                conn.rollback();
                throw e;
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Erreur lors de la creation eleve", e);
                return -1;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur connexion DB", e);
            return -1;
        }
    }

    /**
     * Met a jour un eleve (user + student). Optionnellement, synchronise l'inscription annee active.
     */
    public boolean updateStudent(User user, Student student, boolean syncActiveYearInscription) {
        if (user == null || student == null || user.getId() <= 0 || student.getId() <= 0 || student.getLevel() == null) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!updateUser(conn, user)) {
                    conn.rollback();
                    return false;
                }
                if (!updateStudentRow(conn, student)) {
                    conn.rollback();
                    return false;
                }

                if (syncActiveYearInscription) {
                    upsertActiveYearInscription(conn, student.getId(), student.getLevel(), deriveInscriptionStatus(student.getStatus()));
                }

                conn.commit();
                return true;
            } catch (IllegalStateException e) {
                conn.rollback();
                throw e;
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Erreur lors de la mise a jour eleve", e);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur connexion DB", e);
            return false;
        }
    }

    /**
     * "Suppression" soft: desactive le compte et passe le student en inactive.
     * On garde les lignes (notes, bulletins, inscriptions) pour l'historique.
     */
    public boolean deactivateStudent(int studentId) {
        if (studentId <= 0) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer userId = findUserIdByStudentId(conn, studentId);
                if (userId == null) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement st = conn.prepareStatement("UPDATE students SET status = ? WHERE id = ?")) {
                    st.setString(1, Constants.STUDENT_STATUS_INACTIVE);
                    st.setInt(2, studentId);
                    st.executeUpdate();
                }

                try (PreparedStatement st = conn.prepareStatement("UPDATE users SET is_active = 0 WHERE id = ?")) {
                    st.setInt(1, userId);
                    st.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Erreur lors de la desactivation eleve", e);
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
            stmt.setString(3, user.getRole());
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

    private int insertStudent(Connection conn, Student student, int userId) throws SQLException {
        String sql = "INSERT INTO students (user_id, class_id, level_id, matricule, parent_name, parent_phone, " +
                "parent_email, address, birth_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            SchoolClass sc = student.getSchoolClass();
            stmt.setObject(2, sc != null ? sc.getId() : null);
            stmt.setInt(3, student.getLevel().getId());
            stmt.setString(4, student.getMatricule());
            stmt.setString(5, student.getParentName());
            stmt.setString(6, student.getParentPhone());
            stmt.setString(7, student.getParentEmail());
            stmt.setString(8, student.getAddress());
            stmt.setString(9, student.getBirthDate());
            stmt.setString(10, student.getStatus());
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
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getFirstName());
            stmt.setString(5, user.getLastName());
            stmt.setString(6, user.getEmail());
            stmt.setString(7, user.getPhone());
            stmt.setBoolean(8, user.isActive());
            stmt.setInt(9, user.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    private boolean updateStudentRow(Connection conn, Student student) throws SQLException {
        String sql = "UPDATE students SET class_id = ?, level_id = ?, matricule = ?, parent_name = ?, parent_phone = ?, " +
                "parent_email = ?, address = ?, birth_date = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            SchoolClass sc = student.getSchoolClass();
            stmt.setObject(1, sc != null ? sc.getId() : null);
            stmt.setInt(2, student.getLevel().getId());
            stmt.setString(3, student.getMatricule());
            stmt.setString(4, student.getParentName());
            stmt.setString(5, student.getParentPhone());
            stmt.setString(6, student.getParentEmail());
            stmt.setString(7, student.getAddress());
            stmt.setString(8, student.getBirthDate());
            stmt.setString(9, student.getStatus());
            stmt.setInt(10, student.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    private Integer findUserIdByStudentId(Connection conn, int studentId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM students WHERE id = ?")) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private void upsertActiveYearInscription(Connection conn, int studentId, models.Level level, String status) throws SQLException {
        if (level == null) {
            return;
        }
        Integer activeYearId = findActiveSchoolYearId(conn);
        if (activeYearId == null) {
            return;
        }

        inscriptionDAO.upsertInscription(conn, studentId, activeYearId, level.getId(), status);
    }

    private Integer findActiveSchoolYearId(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM school_years WHERE is_active = 1 LIMIT 1")) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    private String deriveInscriptionStatus(String studentStatus) {
        if (Constants.STUDENT_STATUS_ACTIVE.equals(studentStatus)) {
            return "active";
        }
        if (Constants.STUDENT_STATUS_INACTIVE.equals(studentStatus)) {
            return "inactive";
        }
        return "pending";
    }
}
