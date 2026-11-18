package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO pour les affectations enseignant -> classe -> matiere.
 */
public class TeacherAssignmentDAO {
    private static final Logger logger = Logger.getLogger(TeacherAssignmentDAO.class.getName());

    public List<AssignmentDetail> getAssignmentsByTeacher(int teacherId) {
        List<AssignmentDetail> assignments = new ArrayList<>();
        String sql = """
                SELECT tcs.teacher_id,
                       c.id AS class_id,
                       c.name AS class_name,
                       l.name AS level_name,
                       s.id AS subject_id,
                       s.name AS subject_name
                FROM teacher_class_subjects tcs
                JOIN classes c ON c.id = tcs.class_id
                JOIN levels l ON l.id = c.level_id
                JOIN subjects s ON s.id = tcs.subject_id
                WHERE tcs.teacher_id = ?
                ORDER BY l.number, c.name, s.name
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(new AssignmentDetail(
                            rs.getInt("teacher_id"),
                            rs.getInt("class_id"),
                            rs.getString("class_name"),
                            rs.getString("level_name"),
                            rs.getInt("subject_id"),
                            rs.getString("subject_name")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement des affectations enseignant", e);
        }
        return assignments;
    }

    public List<Integer> getSubjectIdsByTeacherAndClass(int teacherId, int classId) {
        List<Integer> subjectIds = new ArrayList<>();
        String sql = "SELECT subject_id FROM teacher_class_subjects WHERE teacher_id = ? AND class_id = ? ORDER BY subject_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            stmt.setInt(2, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subjectIds.add(rs.getInt("subject_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement des matieres affectees", e);
        }
        return subjectIds;
    }

    public boolean replaceAssignmentsForTeacherAndClass(int teacherId, int classId, List<Integer> subjectIds) {
        Set<Integer> uniqueSubjectIds = subjectIds == null ? Set.of() : new LinkedHashSet<>(subjectIds);
        String deleteSql = "DELETE FROM teacher_class_subjects WHERE teacher_id = ? AND class_id = ?";
        String insertSql = "INSERT INTO teacher_class_subjects (teacher_id, class_id, subject_id) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                deleteStmt.setInt(1, teacherId);
                deleteStmt.setInt(2, classId);
                deleteStmt.executeUpdate();

                for (Integer subjectId : uniqueSubjectIds) {
                    if (subjectId == null || subjectId <= 0) {
                        continue;
                    }
                    insertStmt.setInt(1, teacherId);
                    insertStmt.setInt(2, classId);
                    insertStmt.setInt(3, subjectId);
                    insertStmt.addBatch();
                }
                if (!uniqueSubjectIds.isEmpty()) {
                    insertStmt.executeBatch();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la sauvegarde des affectations enseignant", e);
            return false;
        }
    }

    public record AssignmentDetail(
            int teacherId,
            int classId,
            String className,
            String levelName,
            int subjectId,
            String subjectName
    ) {
    }
}
