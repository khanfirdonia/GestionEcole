package dao;

import models.Level;
import models.PreEnrollment;
import models.SchoolYear;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO des pre-inscriptions (sans compte).
 */
public class PreEnrollmentDAO {
    private static final Logger logger = Logger.getLogger(PreEnrollmentDAO.class.getName());
    private static final String TABLE = "pre_enrollments";

    public int createPreEnrollment(int schoolYearId, int requestedLevelId,
                                   String firstName, String lastName, String birthDate,
                                   String address, String parentName, String parentPhone, String parentEmail) {
        String sql = "INSERT INTO " + TABLE + " (school_year_id, requested_level_id, first_name, last_name, birth_date, " +
                "address, parent_name, parent_phone, parent_email, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (hasExistingPreEnrollment(conn, schoolYearId, firstName, lastName, birthDate)) {
                throw new IllegalStateException(
                        "Cet eleve a deja une pre-inscription pour cette annee scolaire. Une seule demande par annee scolaire est autorisee."
                );
            }
            stmt.setInt(1, schoolYearId);
            stmt.setInt(2, requestedLevelId);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, birthDate);
            stmt.setString(6, address);
            stmt.setString(7, parentName);
            stmt.setString(8, parentPhone);
            stmt.setString(9, parentEmail);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la creation pre-inscription", e);
            return -1;
        }
    }

    private boolean hasExistingPreEnrollment(Connection conn, int schoolYearId, String firstName, String lastName, String birthDate)
            throws SQLException {
        String normalizedBirthDate = birthDate != null && !birthDate.isBlank() ? birthDate.trim() : null;
        String sql;
        if (normalizedBirthDate == null) {
            sql = "SELECT 1 FROM " + TABLE + " WHERE school_year_id = ? " +
                    "AND LOWER(TRIM(first_name)) = LOWER(TRIM(?)) " +
                    "AND LOWER(TRIM(last_name)) = LOWER(TRIM(?)) " +
                    "AND status IN ('pending', 'approved') LIMIT 1";
        } else {
            sql = "SELECT 1 FROM " + TABLE + " WHERE school_year_id = ? " +
                    "AND LOWER(TRIM(first_name)) = LOWER(TRIM(?)) " +
                    "AND LOWER(TRIM(last_name)) = LOWER(TRIM(?)) " +
                    "AND (birth_date = ? OR birth_date IS NULL OR birth_date = '') " +
                    "AND status IN ('pending', 'approved') LIMIT 1";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, schoolYearId);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            if (normalizedBirthDate != null) {
                stmt.setString(4, normalizedBirthDate);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<PreEnrollment> getPendingPreEnrollments() {
        List<PreEnrollment> list = new ArrayList<>();
        String sql = """
                SELECT pe.id AS pre_id, pe.status AS pre_status,
                       pe.first_name, pe.last_name, pe.birth_date, pe.address, pe.parent_name, pe.parent_phone, pe.parent_email,
                       sy.id AS school_year_id_ref, sy.label AS school_year_label,
                       l.id AS level_id_ref, l.name AS level_name, l.number AS level_number
                FROM pre_enrollments pe
                JOIN school_years sy ON sy.id = pe.school_year_id
                JOIN levels l ON l.id = pe.requested_level_id
                WHERE pe.status = 'pending'
                ORDER BY pe.id DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du chargement des pre-inscriptions", e);
        }
        return list;
    }

    public boolean updateStatus(int preEnrollmentId, String status) {
        String sql = "UPDATE " + TABLE + " SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, preEnrollmentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la mise a jour statut pre-inscription", e);
            return false;
        }
    }

    private PreEnrollment map(ResultSet rs) throws SQLException {
        SchoolYear year = new SchoolYear();
        year.setId(rs.getInt("school_year_id_ref"));
        year.setLabel(rs.getString("school_year_label"));

        Level level = new Level();
        level.setId(rs.getInt("level_id_ref"));
        level.setName(rs.getString("level_name"));
        level.setNumber(rs.getInt("level_number"));

        PreEnrollment pe = new PreEnrollment();
        pe.setId(rs.getInt("pre_id"));
        pe.setStatus(rs.getString("pre_status"));
        pe.setSchoolYear(year);
        pe.setRequestedLevel(level);
        pe.setFirstName(rs.getString("first_name"));
        pe.setLastName(rs.getString("last_name"));
        pe.setBirthDate(rs.getString("birth_date"));
        pe.setAddress(rs.getString("address"));
        pe.setParentName(rs.getString("parent_name"));
        pe.setParentPhone(rs.getString("parent_phone"));
        pe.setParentEmail(rs.getString("parent_email"));
        return pe;
    }
}
