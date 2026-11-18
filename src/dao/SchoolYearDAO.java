package dao;

import models.SchoolYear;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DAO pour la gestion des années scolaires
 */
public class SchoolYearDAO {
    private static final Logger logger = Logger.getLogger(SchoolYearDAO.class.getName());
    private static final String TABLE = "school_years";

    public List<SchoolYear> getAllSchoolYears() {
        List<SchoolYear> years = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " ORDER BY start_year DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                years.add(mapResultSetToSchoolYear(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des années scolaires", e);
        }
        return years;
    }

    public SchoolYear getSchoolYearById(int id) {
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToSchoolYear(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'année scolaire", e);
        }
        return null;
    }

    /**
     * Récupérer l'année scolaire active
     */
    public SchoolYear getActiveSchoolYear() {
        String sql = "SELECT * FROM " + TABLE + " WHERE is_active = 1 LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return mapResultSetToSchoolYear(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'année scolaire active", e);
        }
        return null;
    }

    public int saveSchoolYear(SchoolYear year) {
        String sql = "INSERT INTO " + TABLE + " (label, start_year, end_year, start_date, end_date, is_active) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, year.getLabel());
            stmt.setInt(2, year.getStartYear());
            stmt.setInt(3, year.getEndYear());
            stmt.setString(4, year.getStartDate());
            stmt.setString(5, year.getEndDate());
            stmt.setBoolean(6, year.isActive());
            
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création de l'année scolaire", e);
        }
        return -1;
    }

    private SchoolYear mapResultSetToSchoolYear(ResultSet rs) throws SQLException {
        SchoolYear year = new SchoolYear();
        year.setId(rs.getInt("id"));
        year.setLabel(rs.getString("label"));
        year.setStartYear(rs.getInt("start_year"));
        year.setEndYear(rs.getInt("end_year"));
        year.setStartDate(rs.getString("start_date"));
        year.setEndDate(rs.getString("end_date"));
        year.setActive(rs.getBoolean("is_active"));
        return year;
    }
}
