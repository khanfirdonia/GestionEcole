package dao;

import models.Inscription;
import models.Level;
import models.SchoolClass;
import models.SchoolYear;
import models.Student;
import models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO pour les inscriptions (une inscription par eleve et par annee scolaire).
 */
public class InscriptionDAO {
    private static final Logger logger = Logger.getLogger(InscriptionDAO.class.getName());
    private static final String TABLE = "inscriptions";

    public int upsertInscription(int studentId, int schoolYearId, int levelId, String status) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return upsertInscription(conn, studentId, schoolYearId, levelId, status);
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de l'upsert inscription", e);
        }
        return -1;
    }

    public int upsertInscription(Connection conn, int studentId, int schoolYearId, int levelId, String status) throws SQLException {
        String selectSql = "SELECT id, level_id FROM " + TABLE + " WHERE student_id = ? AND school_year_id = ? LIMIT 1";
        try (PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setInt(1, studentId);
            select.setInt(2, schoolYearId);

            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    int inscriptionId = rs.getInt("id");
                    int existingLevelId = rs.getInt("level_id");
                    if (existingLevelId != levelId) {
                        throw buildSingleLevelConflict();
                    }

                    String updateSql = "UPDATE " + TABLE + " SET status = ? WHERE id = ?";
                    try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                        update.setString(1, status);
                        update.setInt(2, inscriptionId);
                        update.executeUpdate();
                    }
                    return inscriptionId;
                }
            }
        }

        String insertSql = "INSERT INTO " + TABLE + " (student_id, school_year_id, level_id, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, schoolYearId);
            stmt.setInt(3, levelId);
            stmt.setString(4, status);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public int countInscriptionsByYearAndLevel(int schoolYearId, int levelId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE school_year_id = ? AND level_id = ? AND status IN ('pending','active')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, schoolYearId);
            stmt.setInt(2, levelId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du comptage des inscriptions", e);
        }
        return 0;
    }

    public List<Inscription> getInscriptionsByYearAndLevel(int schoolYearId, int levelId) {
        List<Inscription> results = new ArrayList<>();
        String sql = """
                SELECT i.id AS inscription_id, i.status AS inscription_status,
                       st.id AS student_id, st.level_id AS student_level_id, st.class_id AS student_class_id,
                       u.id AS user_id, u.first_name, u.last_name, u.username,
                       l.id AS level_id_ref, l.name AS level_name, l.number AS level_number,
                       c.id AS class_id_ref, c.name AS class_name,
                       sy.id AS school_year_id_ref, sy.label AS school_year_label
                FROM inscriptions i
                JOIN students st ON st.id = i.student_id
                JOIN users u ON u.id = st.user_id
                JOIN levels l ON l.id = i.level_id
                JOIN school_years sy ON sy.id = i.school_year_id
                LEFT JOIN classes c ON c.id = i.class_id
                WHERE i.school_year_id = ? AND i.level_id = ?
                ORDER BY u.last_name, u.first_name
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, schoolYearId);
            stmt.setInt(2, levelId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToInscription(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du chargement des inscriptions", e);
        }
        return results;
    }

    public boolean assignInscriptionToClass(int inscriptionId, int classId) {
        String sql = "UPDATE " + TABLE + " SET class_id = ?, status = 'active' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, classId);
            stmt.setInt(2, inscriptionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de l'affectation inscription->classe", e);
        }
        return false;
    }

    public boolean bulkAssignInscriptionsToClasses(List<Assignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return true;
        }

        String sql = "UPDATE " + TABLE + " SET class_id = ?, status = 'active' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (Assignment assignment : assignments) {
                    stmt.setInt(1, assignment.classId());
                    stmt.setInt(2, assignment.inscriptionId());
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
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de l'affectation en lot", e);
        }
        return false;
    }

    private Inscription mapResultSetToInscription(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setUsername(rs.getString("username"));

        Student student = new Student();
        student.setId(rs.getInt("student_id"));
        student.setUser(user);

        Level level = new Level();
        level.setId(rs.getInt("level_id_ref"));
        level.setName(rs.getString("level_name"));
        level.setNumber(rs.getInt("level_number"));

        SchoolYear year = new SchoolYear();
        year.setId(rs.getInt("school_year_id_ref"));
        year.setLabel(rs.getString("school_year_label"));

        SchoolClass schoolClass = null;
        int classId = rs.getInt("class_id_ref");
        if (!rs.wasNull()) {
            schoolClass = new SchoolClass();
            schoolClass.setId(classId);
            schoolClass.setName(rs.getString("class_name"));
        }

        Inscription inscription = new Inscription();
        inscription.setId(rs.getInt("inscription_id"));
        inscription.setStudent(student);
        inscription.setLevel(level);
        inscription.setSchoolYear(year);
        inscription.setSchoolClass(schoolClass);
        inscription.setStatus(rs.getString("inscription_status"));
        return inscription;
    }

    public record Assignment(int inscriptionId, int classId) {
    }

    private IllegalStateException buildSingleLevelConflict() {
        return new IllegalStateException(
                "Un eleve ne peut avoir qu'une seule inscription par annee scolaire et dans un seul niveau."
        );
    }
}
