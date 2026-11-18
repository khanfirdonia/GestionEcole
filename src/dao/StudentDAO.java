package dao;

import models.Level;
import models.SchoolClass;
import models.Student;
import models.User;
import utils.DateUtils;
import utils.Constants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO pour la gestion des eleves.
 */
public class StudentDAO {
    private static final Logger logger = Logger.getLogger(StudentDAO.class.getName());
    private static final String TABLE = "students";

    public record ClassAssignment(int studentId, int classId) {}

    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = baseStudentQuery() + " ORDER BY u.last_name, u.first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }
            logger.info("Recuperation de " + students.size() + " eleves");
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation des eleves", e);
        }
        return students;
    }

    public Student getStudentById(int id) {
        String sql = baseStudentQuery() + " WHERE s.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation de l'eleve ID: " + id, e);
        }
        return null;
    }

    public Student getStudentByUserId(int userId) {
        String sql = baseStudentQuery() + " WHERE s.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStudent(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation de l'eleve par user ID: " + userId, e);
        }
        return null;
    }

    public List<Student> getStudentsByClass(int classId) {
        List<Student> students = new ArrayList<>();
        String sql = baseStudentQuery() + " WHERE s.class_id = ? AND s.status = 'active' ORDER BY u.last_name, u.first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation des eleves par classe", e);
        }
        return students;
    }

    /**
     * Recuperer les eleves d'une classe pour la saisie des notes.
     * Inclut les statuts pedagogiques valides et bascule sur toute la classe si aucun resultat.
     */
    public List<Student> getStudentsByClassForGradeEntry(int classId) {
        List<Student> students = new ArrayList<>();
        String sql = baseStudentQuery() +
                " WHERE s.class_id = ? AND s.status IN ('" + Constants.STUDENT_STATUS_ACTIVE + "', '" +
                Constants.STUDENT_STATUS_PENDING + "', '" + Constants.STUDENT_STATUS_WAITING_LIST +
                "') ORDER BY u.last_name, u.first_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation des eleves pour la saisie", e);
        }

        if (!students.isEmpty()) {
            return students;
        }

        String fallbackSql = baseStudentQuery() + " WHERE s.class_id = ? ORDER BY u.last_name, u.first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(fallbackSql)) {

            stmt.setInt(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du fallback des eleves par classe", e);
        }

        return students;
    }

    public List<Student> getStudentsByLevel(int levelId) {
        List<Student> students = new ArrayList<>();
        String sql = baseStudentQuery() + " WHERE s.level_id = ? AND s.status = 'active' ORDER BY u.last_name, u.first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, levelId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation des eleves par niveau", e);
        }
        return students;
    }

    public List<Student> getPendingStudents() {
        List<Student> students = new ArrayList<>();
        String sql = baseStudentQuery() +
                " WHERE s.status IN ('" + Constants.STUDENT_STATUS_PENDING + "', '" + Constants.STUDENT_STATUS_WAITING_LIST + "')" +
                " ORDER BY CASE " +
                "WHEN s.status = '" + Constants.STUDENT_STATUS_PENDING + "' THEN 0 " +
                "WHEN s.status = '" + Constants.STUDENT_STATUS_WAITING_LIST + "' THEN 1 " +
                "ELSE 2 END, s.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation des eleves en attente", e);
        }
        return students;
    }

    public int saveStudent(Student student) {
        String sql = "INSERT INTO " + TABLE + " (user_id, class_id, level_id, matricule, parent_name, parent_phone, " +
                "parent_email, address, birth_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, student.getUser().getId());
            stmt.setObject(2, student.getSchoolClass() != null ? student.getSchoolClass().getId() : null);
            stmt.setInt(3, student.getLevel().getId());
            stmt.setString(4, student.getMatricule());
            stmt.setString(5, student.getParentName());
            stmt.setString(6, student.getParentPhone());
            stmt.setString(7, student.getParentEmail());
            stmt.setString(8, student.getAddress());
            stmt.setString(9, student.getBirthDate());
            stmt.setString(10, student.getStatus());

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int studentId = generatedKeys.getInt(1);
                        logger.info("Eleve cree avec succes: ID = " + studentId);
                        return studentId;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la creation de l'eleve", e);
        }
        return -1;
    }

    public boolean updateStudent(Student student) {
        String sql = "UPDATE " + TABLE + " SET class_id = ?, level_id = ?, matricule = ?, parent_name = ?, " +
                "parent_phone = ?, parent_email = ?, address = ?, birth_date = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, student.getSchoolClass() != null ? student.getSchoolClass().getId() : null);
            stmt.setInt(2, student.getLevel().getId());
            stmt.setString(3, student.getMatricule());
            stmt.setString(4, student.getParentName());
            stmt.setString(5, student.getParentPhone());
            stmt.setString(6, student.getParentEmail());
            stmt.setString(7, student.getAddress());
            stmt.setString(8, student.getBirthDate());
            stmt.setString(9, student.getStatus());
            stmt.setInt(10, student.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Eleve mis a jour: ID = " + student.getId());
                return true;
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la mise a jour de l'eleve ID: " + student.getId(), e);
        }
        return false;
    }

    public boolean approveStudent(int studentId, int classId) {
        String selectClassSql = "SELECT current_students, capacity FROM classes WHERE id = ? FOR UPDATE";
        String approveStudentSql = "UPDATE " + TABLE + " SET status = 'active', class_id = ? " +
                "WHERE id = ? AND status IN ('" + Constants.STUDENT_STATUS_PENDING + "', '" + Constants.STUDENT_STATUS_WAITING_LIST + "')";
        String incrementClassSql = "UPDATE classes SET current_students = current_students + 1 WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement selectClassStmt = conn.prepareStatement(selectClassSql);
                 PreparedStatement approveStudentStmt = conn.prepareStatement(approveStudentSql);
                 PreparedStatement incrementClassStmt = conn.prepareStatement(incrementClassSql)) {

                selectClassStmt.setInt(1, classId);
                try (ResultSet classResult = selectClassStmt.executeQuery()) {
                    if (!classResult.next()) {
                        conn.rollback();
                        logger.warning("Classe introuvable pour approbation: ID = " + classId);
                        return false;
                    }

                    int currentStudents = classResult.getInt("current_students");
                    int capacity = Math.min(classResult.getInt("capacity"), Constants.MAX_CLASS_CAPACITY);
                    if (currentStudents >= capacity) {
                        conn.rollback();
                        logger.warning("Classe pleine, approbation impossible: ID = " + classId);
                        return false;
                    }
                }

                approveStudentStmt.setInt(1, classId);
                approveStudentStmt.setInt(2, studentId);
                int rowsUpdated = approveStudentStmt.executeUpdate();
                if (rowsUpdated == 0) {
                    conn.rollback();
                    logger.warning("Eleve introuvable ou deja traite: ID = " + studentId);
                    return false;
                }

                incrementClassStmt.setInt(1, classId);
                incrementClassStmt.executeUpdate();

                conn.commit();
                logger.info("Eleve approuve: ID = " + studentId + ", classe = " + classId);
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de l'approbation de l'eleve", e);
        }
        return false;
    }

    public boolean rejectStudent(int studentId) {
        String sql = "UPDATE " + TABLE + " SET status = 'inactive' WHERE id = ? " +
                "AND status IN ('" + Constants.STUDENT_STATUS_PENDING + "', '" + Constants.STUDENT_STATUS_WAITING_LIST + "')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Eleve rejete: ID = " + studentId);
                return true;
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du rejet de l'eleve", e);
        }
        return false;
    }

    /**
     * Affectation en lot: assigne une classe et active le statut pour chaque eleve.
     * Utilise une transaction unique pour limiter les incoherences.
     */
    public boolean bulkAssignStudentsToClasses(List<ClassAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return true;
        }

        String updateStudentSql = "UPDATE " + TABLE + " SET class_id = ?, status = '" + Constants.STUDENT_STATUS_ACTIVE + "' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateStudentSql)) {
            conn.setAutoCommit(false);
            try {
                for (ClassAssignment assignment : assignments) {
                    stmt.setInt(1, assignment.classId());
                    stmt.setInt(2, assignment.studentId());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de l'affectation en lot des eleves", e);
            return false;
        }
    }

    public int countStudentsByClass(int classId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE class_id = ? AND status = 'active'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du comptage des eleves", e);
        }
        return 0;
    }

    private String baseStudentQuery() {
        return "SELECT s.*, s.id AS student_id, u.id AS user_id, " +
                "l.id AS level_id_ref, l.name AS level_name, l.number AS level_number, " +
                "c.id AS class_id_ref, c.name AS class_name, " +
                "u.* FROM " + TABLE + " s " +
                "JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN levels l ON s.level_id = l.id " +
                "LEFT JOIN classes c ON s.class_id = c.id";
    }

    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
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

        Student student = new Student();
        student.setId(rs.getInt("student_id"));
        student.setUser(user);
        student.setMatricule(rs.getString("matricule"));
        student.setParentName(rs.getString("parent_name"));
        student.setParentPhone(rs.getString("parent_phone"));
        student.setParentEmail(rs.getString("parent_email"));
        student.setAddress(rs.getString("address"));
        student.setBirthDate(DateUtils.formatStoredDate(rs.getString("birth_date")));
        student.setStatus(rs.getString("status"));

        int levelId = rs.getInt("level_id_ref");
        if (!rs.wasNull()) {
            Level level = new Level();
            level.setId(levelId);
            level.setName(rs.getString("level_name"));
            level.setNumber(rs.getInt("level_number"));
            student.setLevel(level);
        }

        int classId = rs.getInt("class_id_ref");
        if (!rs.wasNull()) {
            SchoolClass schoolClass = new SchoolClass();
            schoolClass.setId(classId);
            schoolClass.setName(rs.getString("class_name"));
            student.setSchoolClass(schoolClass);
        }

        return student;
    }
}
