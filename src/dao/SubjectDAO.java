package dao;

import models.Subject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DAO pour la gestion des matières
 */
public class SubjectDAO {
    private static final Logger logger = Logger.getLogger(SubjectDAO.class.getName());
    private static final String TABLE = "subjects";

    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE is_active = 1 ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des matières", e);
        }
        return subjects;
    }

    public Subject getSubjectById(int id) {
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToSubject(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de la matière", e);
        }
        return null;
    }

    /**
     * Récupérer les matières pour un niveau spécifique
     */
    public List<Subject> getSubjectsByLevel(int levelNumber) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE is_active = 1 AND min_level <= ? AND max_level >= ? ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, levelNumber);
            stmt.setInt(2, levelNumber);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des matières par niveau", e);
        }
        return subjects;
    }

    public List<Subject> getSubjectsByTeacherAndLevel(int teacherId, int levelNumber) {
        List<Subject> subjects = new ArrayList<>();
        String sql = """
                SELECT DISTINCT s.*
                FROM subjects s
                JOIN teacher_class_subjects tcs ON tcs.subject_id = s.id
                JOIN classes c ON c.id = tcs.class_id
                JOIN levels l ON l.id = c.level_id
                WHERE s.is_active = 1
                  AND tcs.teacher_id = ?
                  AND l.number = ?
                ORDER BY s.name
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            stmt.setInt(2, levelNumber);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des matières de l'enseignant", e);
        }
        return subjects;
    }

    public List<Subject> getSubjectsByTeacherAndClass(int teacherId, int classId) {
        List<Subject> subjects = new ArrayList<>();
        String sql = """
                SELECT DISTINCT s.*
                FROM subjects s
                WHERE s.is_active = 1
                  AND (
                    EXISTS (
                        SELECT 1
                        FROM teacher_class_subjects tcs
                        WHERE tcs.teacher_id = ?
                          AND tcs.class_id = ?
                          AND tcs.subject_id = s.id
                    )
                  )
                ORDER BY s.name
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            stmt.setInt(2, classId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des matières par classe enseignant", e);
        }
        return subjects;
    }

    private Subject mapResultSetToSubject(ResultSet rs) throws SQLException {
        Subject subject = new Subject();
        subject.setId(rs.getInt("id"));
        subject.setName(rs.getString("name"));
        subject.setCode(rs.getString("code"));
        subject.setCoefficient(rs.getInt("coefficient"));
        subject.setMinLevel(rs.getInt("min_level"));
        subject.setMaxLevel(rs.getInt("max_level"));
        return subject;
    }
}
