package dao;

import models.*;
import utils.Constants;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO pour la gestion des classes (groupes d'élèves)
 */
public class ClassDAO {
    private static final Logger logger = Logger.getLogger(ClassDAO.class.getName());
    private static final String TABLE = "classes";

    public List<SchoolClass> getAllClasses() {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, c.id AS class_id, l.id as level_id, l.name as level_name, l.number as level_number, " +
                    "t.id as teacher_id, u.id AS user_id, u.* FROM " + TABLE + " c " +
                    "JOIN levels l ON c.level_id = l.id " +
                    "JOIN teachers t ON c.teacher_id = t.id " +
                    "JOIN users u ON t.user_id = u.id " +
                    "ORDER BY l.number, c.name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                classes.add(mapResultSetToClass(rs));
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la récupération des classes", e);
        }
        return classes;
    }

    public SchoolClass getClassById(int id) {
        String sql = "SELECT c.*, c.id AS class_id, l.id as level_id, l.name as level_name, l.number as level_number, " +
                    "t.id as teacher_id, u.id AS user_id, u.* FROM " + TABLE + " c " +
                    "JOIN levels l ON c.level_id = l.id " +
                    "JOIN teachers t ON c.teacher_id = t.id " +
                    "JOIN users u ON t.user_id = u.id WHERE c.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToClass(rs);
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la récupération de la classe", e);
        }
        return null;
    }

    public List<SchoolClass> getClassesByLevel(int levelId) {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, c.id AS class_id, l.id as level_id, l.name as level_name, l.number as level_number, " +
                    "t.id as teacher_id, u.id AS user_id, u.* FROM " + TABLE + " c " +
                    "JOIN levels l ON c.level_id = l.id " +
                    "JOIN teachers t ON c.teacher_id = t.id " +
                    "JOIN users u ON t.user_id = u.id " +
                    "WHERE c.level_id = ? AND c.is_active = 1 ORDER BY c.name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, levelId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(mapResultSetToClass(rs));
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la récupération des classes par niveau", e);
        }
        return classes;
    }

    /**
     * Récupérer les classes d'un enseignant spécifique
     */
    public List<SchoolClass> getClassesByTeacher(int teacherId) {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT c.*, c.id AS class_id, l.id as level_id, l.name as level_name, l.number as level_number, " +
                    "t.id as teacher_id, u.id AS user_id, u.* FROM " + TABLE + " c " +
                    "JOIN levels l ON c.level_id = l.id " +
                    "JOIN teachers t ON c.teacher_id = t.id " +
                    "JOIN users u ON t.user_id = u.id " +
                    "WHERE c.is_active = 1 AND (" +
                    "c.teacher_id = ? OR EXISTS (" +
                    "SELECT 1 FROM teacher_class_subjects tcs " +
                    "WHERE tcs.class_id = c.id AND tcs.teacher_id = ?" +
                    ")) ORDER BY l.number, c.name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            stmt.setInt(2, teacherId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classes.add(mapResultSetToClass(rs));
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la récupération des classes de l'enseignant", e);
        }
        return classes;
    }

    public int saveClass(SchoolClass schoolClass) {
        String sql = "INSERT INTO " + TABLE + " (name, level_id, teacher_id, capacity, current_students, is_active) " +
                    "VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, schoolClass.getName());
            stmt.setInt(2, schoolClass.getLevel().getId());
            stmt.setInt(3, schoolClass.getTeacher().getId());
            stmt.setInt(4, Math.min(schoolClass.getCapacity(), Constants.MAX_CLASS_CAPACITY));
            stmt.setInt(5, schoolClass.getCurrentStudents());
            
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la création de la classe", e);
        }
        return -1;
    }

    public boolean updateClass(SchoolClass schoolClass) {
        String sql = "UPDATE " + TABLE + " SET name = ?, level_id = ?, teacher_id = ?, " +
                    "capacity = ?, current_students = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, schoolClass.getName());
            stmt.setInt(2, schoolClass.getLevel().getId());
            stmt.setInt(3, schoolClass.getTeacher().getId());
            stmt.setInt(4, Math.min(schoolClass.getCapacity(), Constants.MAX_CLASS_CAPACITY));
            stmt.setInt(5, schoolClass.getCurrentStudents());
            stmt.setInt(6, schoolClass.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la mise à jour de la classe", e);
        }
        return false;
    }

    /**
     * Cree des classes manquantes pour un niveau (A, B, C, ...), jusqu'au nombre requis.
     * Les classes sont creees actives, avec une capacite limitee par Constants.MAX_CLASS_CAPACITY.
     */
    public int ensureClassCountForLevel(int levelId, int levelNumber, int requiredCount) {
        int safeRequired = Math.max(1, requiredCount);
        List<SchoolClass> existing = getClassesByLevel(levelId);
        int existingCount = existing.size();
        if (existingCount >= safeRequired) {
            return 0;
        }

        List<Integer> teacherIds = loadTeacherIds();
        if (teacherIds.isEmpty()) {
            logger.warning("Aucun enseignant disponible pour generer des classes.");
            return 0;
        }

        int created = 0;
        int teacherIndex = 0;
        String sql = "INSERT INTO " + TABLE + " (name, level_id, teacher_id, capacity, current_students, is_active) " +
                "VALUES (?, ?, ?, ?, 0, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = existingCount; i < safeRequired; i++) {
                String className = levelNumber + alphaSuffix(i);
                stmt.setString(1, className);
                stmt.setInt(2, levelId);
                stmt.setInt(3, teacherIds.get(teacherIndex % teacherIds.size()));
                stmt.setInt(4, Math.min(Constants.DEFAULT_CLASS_CAPACITY, Constants.MAX_CLASS_CAPACITY));
                stmt.addBatch();
                teacherIndex++;
                created++;
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la generation des classes", e);
            return 0;
        }

        return created;
    }

    public void recomputeCurrentStudentsForLevel(int levelId) {
        String sql = """
                UPDATE classes c
                SET c.current_students = (
                    SELECT COUNT(*)
                    FROM students s
                    WHERE s.class_id = c.id AND s.status = 'active'
                )
                WHERE c.level_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, levelId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de la recomputation des effectifs de classes", e);
        }
    }

    private List<Integer> loadTeacherIds() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM teachers ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du chargement des enseignants", e);
        }
        return ids;
    }

    private static String alphaSuffix(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (n >= 0) {
            int r = n % 26;
            sb.insert(0, (char) ('A' + r));
            n = (n / 26) - 1;
        }
        return sb.toString();
    }

    private SchoolClass mapResultSetToClass(ResultSet rs) throws SQLException {
        Level level = new Level();
        level.setId(rs.getInt("level_id"));
        level.setName(rs.getString("level_name"));
        level.setNumber(rs.getInt("level_number"));
        
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        
        Teacher teacher = new Teacher();
        teacher.setId(rs.getInt("teacher_id"));
        teacher.setUser(user);
        
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(rs.getInt("class_id"));
        schoolClass.setName(rs.getString("name"));
        schoolClass.setLevel(level);
        schoolClass.setTeacher(teacher);
        schoolClass.setCapacity(rs.getInt("capacity"));
        schoolClass.setCurrentStudents(rs.getInt("current_students"));
        
        return schoolClass;
    }
}
