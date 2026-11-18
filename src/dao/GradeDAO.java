package dao;

import models.Grade;
import models.SchoolYear;
import models.Student;
import models.Subject;
import models.Trimester;
import models.User;
import utils.DateUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO pour la gestion des notes (grades)
 */
public class GradeDAO {
    private static final Logger logger = Logger.getLogger(GradeDAO.class.getName());
    private static final String TABLE = "grades";

    /**
     * Recuperer toutes les notes d'un eleve pour un trimestre.
     */
    public List<Grade> getGradesByStudentAndTrimester(int studentId, int trimesterId) {
        return getGradesByStudentAndPeriod(studentId, null, trimesterId);
    }

    public List<Grade> getGradesByStudentAndPeriod(int studentId, Integer schoolYearId, int trimesterId) {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT g.*, s.name AS subject_name, s.coefficient FROM " + TABLE + " g " +
                "JOIN subjects s ON g.subject_id = s.id " +
                "WHERE g.student_id = ? " +
                (schoolYearId != null ? "AND g.school_year_id = ? " : "") +
                "AND g.trimester_id = ? ORDER BY s.name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            int parameterIndex = 2;
            if (schoolYearId != null) {
                stmt.setInt(parameterIndex++, schoolYearId);
            }
            stmt.setInt(parameterIndex, trimesterId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    grades.add(mapResultSetToGrade(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des notes", e);
        }
        return grades;
    }

    /**
     * Recuperer une note specifique (etudiant + matiere + trimestre).
     */
    public Grade getGrade(int studentId, int subjectId, int trimesterId) {
        String sql = "SELECT g.*, s.name AS subject_name, s.coefficient FROM " + TABLE + " g " +
                "JOIN subjects s ON g.subject_id = s.id " +
                "WHERE g.student_id = ? AND g.subject_id = ? AND g.trimester_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, subjectId);
            stmt.setInt(3, trimesterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGrade(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation de la note", e);
        }
        return null;
    }

    /**
     * Sauvegarder une nouvelle note.
     */
    public int saveGrade(Grade grade) {
        String sql = "INSERT INTO " + TABLE + " (student_id, subject_id, teacher_id, school_year_id, trimester_id, value, remarks, recorded_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, grade.getStudent().getId());
            stmt.setInt(2, grade.getSubject().getId());
            stmt.setInt(3, grade.getTeacher().getId());
            stmt.setInt(4, grade.getSchoolYear().getId());
            stmt.setInt(5, grade.getTrimester().getId());
            stmt.setDouble(6, grade.getValue());
            stmt.setString(7, grade.getRemarks());

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la sauvegarde de la note", e);
        }
        return -1;
    }

    /**
     * Mettre a jour une note existante.
     */
    public boolean updateGrade(Grade grade) {
        String sql = "UPDATE " + TABLE + " SET value = ?, remarks = ?, teacher_id = ?, recorded_date = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, grade.getValue());
            stmt.setString(2, grade.getRemarks());
            stmt.setInt(3, grade.getTeacher().getId());
            stmt.setInt(4, grade.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise a jour de la note", e);
        }
        return false;
    }

    /**
     * Recuperer les notes d'une classe pour un trimestre et une matiere.
     */
    public List<Grade> getGradesByClassAndSubject(int classId, int subjectId, int trimesterId) {
        return getGradesByClassAndSubject(classId, subjectId, null, trimesterId);
    }

    public List<Grade> getGradesByClassAndSubject(int classId, int subjectId, Integer schoolYearId, int trimesterId) {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT g.*, s.name AS subject_name, s.coefficient, st.id AS student_ref_id, st.user_id, " +
                "u.first_name, u.last_name FROM " + TABLE + " g " +
                "JOIN students st ON g.student_id = st.id " +
                "JOIN users u ON st.user_id = u.id " +
                "JOIN subjects s ON g.subject_id = s.id " +
                "WHERE st.class_id = ? AND g.subject_id = ? " +
                (schoolYearId != null ? "AND g.school_year_id = ? " : "") +
                "AND g.trimester_id = ? ORDER BY u.last_name, u.first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            stmt.setInt(2, subjectId);
            int parameterIndex = 3;
            if (schoolYearId != null) {
                stmt.setInt(parameterIndex++, schoolYearId);
            }
            stmt.setInt(parameterIndex, trimesterId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    grades.add(mapResultSetToGrade(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des notes", e);
        }
        return grades;
    }

    private Grade mapResultSetToGrade(ResultSet rs) throws SQLException {
        Grade grade = new Grade();
        grade.setId(rs.getInt("id"));
        grade.setValue(rs.getDouble("value"));
        grade.setRemarks(rs.getString("remarks"));
        grade.setRecordedDate(DateUtils.formatStoredDateTime(rs.getString("recorded_date")));
        grade.setValidated(rs.getBoolean("is_validated"));

        Subject subject = new Subject();
        subject.setId(rs.getInt("subject_id"));
        subject.setName(rs.getString("subject_name"));
        subject.setCoefficient(rs.getInt("coefficient"));
        grade.setSubject(subject);

        if (hasColumn(rs, "student_ref_id")) {
            Student student = new Student();
            student.setId(rs.getInt("student_ref_id"));
            if (hasColumn(rs, "first_name") && hasColumn(rs, "last_name")) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                student.setUser(user);
            }
            grade.setStudent(student);
        }

        if (hasColumn(rs, "school_year_id")) {
            SchoolYear schoolYear = new SchoolYear();
            schoolYear.setId(rs.getInt("school_year_id"));
            grade.setSchoolYear(schoolYear);
        }

        if (hasColumn(rs, "trimester_id")) {
            Trimester trimester = new Trimester();
            trimester.setId(rs.getInt("trimester_id"));
            grade.setTrimester(trimester);
        }

        return grade;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }
}
