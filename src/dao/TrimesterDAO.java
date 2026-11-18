package dao;

import models.Trimester;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DAO pour la gestion des trimestres
 */
public class TrimesterDAO {
    private static final Logger logger = Logger.getLogger(TrimesterDAO.class.getName());
    private static final String TABLE = "trimesters";

    public List<Trimester> getAllTrimesters() {
        List<Trimester> trimesters = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " ORDER BY number";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                trimesters.add(mapResultSetToTrimester(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des trimestres", e);
        }
        return trimesters;
    }

    public Trimester getTrimesterById(int id) {
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToTrimester(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération du trimestre", e);
        }
        return null;
    }

    public Trimester getTrimesterByNumber(int number) {
        String sql = "SELECT * FROM " + TABLE + " WHERE number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToTrimester(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération du trimestre", e);
        }
        return null;
    }

    private Trimester mapResultSetToTrimester(ResultSet rs) throws SQLException {
        Trimester trimester = new Trimester();
        trimester.setId(rs.getInt("id"));
        trimester.setName(rs.getString("name"));
        trimester.setNumber(rs.getInt("number"));
        trimester.setStartDate(rs.getString("start_date"));
        trimester.setEndDate(rs.getString("end_date"));
        return trimester;
    }
}
