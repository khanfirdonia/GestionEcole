package dao;

import models.*;
import utils.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO pour la gestion des bulletins scolaires
 */
public class BulletinDAO {
    private static final Logger logger = Logger.getLogger(BulletinDAO.class.getName());
    private static final String TABLE = "bulletins";

    /**
     * Récupérer tous les bulletins d'un élève
     */
    public List<Bulletin> getBulletinsByStudent(int studentId) {
        List<Bulletin> bulletins = new ArrayList<>();
        String sql = "SELECT b.*, sy.label as school_year_label, t.name as trimester_name FROM " + TABLE + " b " +
                    "JOIN school_years sy ON b.school_year_id = sy.id " +
                    "JOIN trimesters t ON b.trimester_id = t.id " +
                    "WHERE b.student_id = ? ORDER BY sy.start_year DESC, t.number DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bulletins.add(mapResultSetToBulletin(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des bulletins", e);
        }
        return bulletins;
    }

    /**
     * Récupérer un bulletin spécifique
     */
    public Bulletin getBulletin(int studentId, int schoolYearId, int trimesterId) {
        String sql = "SELECT b.*, sy.label as school_year_label, t.name as trimester_name FROM " + TABLE + " b " +
                    "JOIN school_years sy ON b.school_year_id = sy.id " +
                    "JOIN trimesters t ON b.trimester_id = t.id " +
                    "WHERE b.student_id = ? AND b.school_year_id = ? AND b.trimester_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            stmt.setInt(2, schoolYearId);
            stmt.setInt(3, trimesterId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToBulletin(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération du bulletin", e);
        }
        return null;
    }

    /**
     * Sauvegarder un nouveau bulletin
     */
    public int saveBulletin(Bulletin bulletin) {
        String sql = "INSERT INTO " + TABLE + " (student_id, school_year_id, trimester_id, average, rank, appreciation, " +
                    "teacher_comment, is_finalized) VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, bulletin.getStudent().getId());
            stmt.setInt(2, bulletin.getSchoolYear().getId());
            stmt.setInt(3, bulletin.getTrimester().getId());
            stmt.setDouble(4, bulletin.getAverage());
            stmt.setInt(5, bulletin.getRank());
            stmt.setString(6, bulletin.getAppreciation());
            stmt.setString(7, bulletin.getTeacherComment());
            
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la sauvegarde du bulletin", e);
        }
        return -1;
    }

    /**
     * Mettre à jour un bulletin
     */
    public boolean updateBulletin(Bulletin bulletin) {
        String sql = "UPDATE " + TABLE + " SET average = ?, rank = ?, appreciation = ?, " +
                    "teacher_comment = ?, is_finalized = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDouble(1, bulletin.getAverage());
            stmt.setInt(2, bulletin.getRank());
            stmt.setString(3, bulletin.getAppreciation());
            stmt.setString(4, bulletin.getTeacherComment());
            stmt.setBoolean(5, bulletin.isFinalized());
            stmt.setInt(6, bulletin.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour du bulletin", e);
        }
        return false;
    }

    /**
     * Récupérer les bulletins d'une classe pour un trimestre
     */
    public List<Bulletin> getBulletinsByClass(int classId, int trimesterId) {
        List<Bulletin> bulletins = new ArrayList<>();
        String sql = "SELECT b.*, sy.label as school_year_label, t.name as trimester_name FROM " + TABLE + " b " +
                    "JOIN students st ON b.student_id = st.id " +
                    "JOIN school_years sy ON b.school_year_id = sy.id " +
                    "JOIN trimesters t ON b.trimester_id = t.id " +
                    "WHERE st.class_id = ? AND b.trimester_id = ? AND st.status = 'active' " +
                    "ORDER BY b.rank ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, classId);
            stmt.setInt(2, trimesterId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bulletins.add(mapResultSetToBulletin(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des bulletins de classe", e);
        }
        return bulletins;
    }

    private Bulletin mapResultSetToBulletin(ResultSet rs) throws SQLException {
        Bulletin bulletin = new Bulletin();
        bulletin.setId(rs.getInt("id"));
        bulletin.setAverage(rs.getDouble("average"));
        bulletin.setRank(rs.getInt("rank"));
        bulletin.setAppreciation(rs.getString("appreciation"));
        bulletin.setTeacherComment(rs.getString("teacher_comment"));
        bulletin.setParentsComment(rs.getString("parents_comment"));
        bulletin.setRecordedDate(DateUtils.formatStoredDateTime(rs.getString("recorded_date")));
        bulletin.setFinalized(rs.getBoolean("is_finalized"));

        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setId(rs.getInt("school_year_id"));
        schoolYear.setLabel(rs.getString("school_year_label"));
        bulletin.setSchoolYear(schoolYear);

        Trimester trimester = new Trimester();
        trimester.setId(rs.getInt("trimester_id"));
        trimester.setName(rs.getString("trimester_name"));
        bulletin.setTrimester(trimester);
        
        return bulletin;
    }
}
