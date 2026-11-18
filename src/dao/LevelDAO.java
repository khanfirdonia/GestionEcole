package dao;

import models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO pour la gestion des niveaux scolaires
 */
public class LevelDAO {
    private static final Logger logger = Logger.getLogger(LevelDAO.class.getName());
    private static final String TABLE = "levels";

    public List<Level> getAllLevels() {
        List<Level> levels = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " ORDER BY number";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                levels.add(mapResultSetToLevel(rs));
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la récupération des niveaux", e);
        }
        return levels;
    }

    public Level getLevelById(int id) {
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToLevel(rs);
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la récupération du niveau", e);
        }
        return null;
    }

    public Level getLevelByNumber(int number) {
        String sql = "SELECT * FROM " + TABLE + " WHERE number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToLevel(rs);
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la récupération du niveau", e);
        }
        return null;
    }

    private Level mapResultSetToLevel(ResultSet rs) throws SQLException {
        Level level = new Level();
        level.setId(rs.getInt("id"));
        level.setName(rs.getString("name"));
        level.setNumber(rs.getInt("number"));
        return level;
    }
}
