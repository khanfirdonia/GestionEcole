package dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static final int ADMIN_MAX_STUDENTS_PER_CLASS = 20;
    private static final int TARGET_STUDENTS_PER_CLASS = 15;
    private static final String DB_HOST = getConfig("DB_HOST", "localhost");
    private static final String DB_PORT = getConfig("DB_PORT", "3306");
    private static final String DB_NAME = getConfig("DB_NAME", "db_school_management");
    private static final String URL = buildJdbcUrl(DB_NAME);
    private static final String SERVER_URL = buildJdbcUrl(null);
    private static final String USER = getConfig("DB_USER", "root");
    private static final String PASSWORD = getConfig("DB_PASSWORD", "");
    private static volatile boolean bootstrapChecked;
    private static volatile boolean connectionLogged;

    private record SubjectRef(int id, String name, String code, int minLevel, int maxLevel) {}

    private DatabaseConnection() {}

    public static Connection getConnection() {
        try {
            ensureMySqlDriverLoaded();
            logConnectionTargetOnce();
            Connection connection = openApplicationConnectionWithRetry();
            ensureBootstrapData(connection);
            logger.info("Database connection established");
            return connection;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error connecting to database", e);
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private static void logConnectionTargetOnce() {
        if (connectionLogged) {
            return;
        }
        synchronized (DatabaseConnection.class) {
            if (connectionLogged) {
                return;
            }
            logger.info("Database target: host=" + DB_HOST + " port=" + DB_PORT + " db=" + DB_NAME + " user=" + USER);
            connectionLogged = true;
        }
    }

    private static Connection openApplicationConnectionWithRetry() throws SQLException {
        try {
            return openApplicationConnection();
        } catch (SQLException firstError) {
            logger.warning("First database connection attempt failed, retrying once...");
            try {
                Thread.sleep(400);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            return openApplicationConnection();
        }
    }

    public static void closeConnection() {
        // No-op: each DAO manages its own connection lifecycle with try-with-resources.
    }

    private static Connection openApplicationConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            if (isUnknownDatabase(e)) {
                logger.warning("Database '" + DB_NAME + "' not found. Attempting initialization from database_init.sql");
                initializeDatabaseFromScript();
                return DriverManager.getConnection(URL, USER, PASSWORD);
            }
            throw e;
        }
    }

    private static void ensureBootstrapData(Connection connection) throws SQLException {
        if (bootstrapChecked) {
            return;
        }

        synchronized (DatabaseConnection.class) {
            if (bootstrapChecked) {
                return;
            }

            boolean initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ensureTeacherClassSubjectsTable(connection);
                ensurePreEnrollmentsTable(connection);
                seedTeacherSpecializations(connection);
                seedMissingClasses(connection);
                ensureMinimumClassesPerLevel(connection);
                normalizeClassCapacityLimit(connection);
                seedTeacherClassSubjectAssignments(connection);
                seedMissingStudents(connection);
                normalizePendingStudentsToWaitingList(connection);
                normalizeDemoStudentNames(connection);
                repairSparseDemoStudentAssignments(connection);
                connection.commit();
                bootstrapChecked = true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(initialAutoCommit);
            }
        }
    }

    private static void ensureTeacherClassSubjectsTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS teacher_class_subjects (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    teacher_id INT NOT NULL,
                    class_id INT NOT NULL,
                    subject_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
                    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
                    UNIQUE KEY unique_teacher_class_subject (teacher_id, class_id, subject_id),
                    INDEX idx_tcs_teacher (teacher_id),
                    INDEX idx_tcs_class (class_id),
                    INDEX idx_tcs_subject (subject_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private static void ensurePreEnrollmentsTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS pre_enrollments (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  school_year_id INT NOT NULL,
                  requested_level_id INT NOT NULL,
                  first_name VARCHAR(100) NOT NULL,
                  last_name VARCHAR(100) NOT NULL,
                  birth_date VARCHAR(10),
                  address TEXT,
                  parent_name VARCHAR(100),
                  parent_phone VARCHAR(20),
                  parent_email VARCHAR(100),
                  status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY (school_year_id) REFERENCES school_years(id) ON DELETE CASCADE,
                  FOREIGN KEY (requested_level_id) REFERENCES levels(id),
                  UNIQUE KEY unique_pre_enrollment (school_year_id, last_name, first_name, birth_date),
                  INDEX idx_pre_enroll_status (status),
                  INDEX idx_pre_enroll_year (school_year_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private static void seedMissingClasses(Connection connection) throws SQLException {
        if (countRows(connection, "classes") > 0) {
            return;
        }

        java.util.List<LevelRef> levels = loadLevels(connection);
        java.util.List<Integer> teacherIds = loadIds(connection, "SELECT id FROM teachers ORDER BY id");
        if (levels.isEmpty() || teacherIds.isEmpty()) {
            logger.warning("Skipping class bootstrap because levels or teachers are missing.");
            return;
        }

        String insertSql = "INSERT INTO classes (name, level_id, teacher_id, capacity, current_students, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        int teacherIndex = 0;

        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            for (LevelRef level : levels) {
                int levelId = level.id();
                int requiredClasses = requiredClassesForLevel(connection, levelId);
                java.util.List<String> suffixes = buildClassSuffixes(requiredClasses);
                for (String suffix : suffixes) {
                    stmt.setString(1, level.number() + suffix);
                    stmt.setInt(2, levelId);
                    stmt.setInt(3, teacherIds.get(teacherIndex % teacherIds.size()));
                    stmt.setInt(4, ADMIN_MAX_STUDENTS_PER_CLASS);
                    stmt.setInt(5, 0);
                    stmt.setBoolean(6, true);
                    stmt.addBatch();
                    teacherIndex++;
                }
            }
            stmt.executeBatch();
        }

        logger.info("Default classes inserted for levels: " + levels.size());
    }

    private static void ensureMinimumClassesPerLevel(Connection connection) throws SQLException {
        java.util.List<LevelRef> levels = loadLevels(connection);
        java.util.List<Integer> teacherIds = loadIds(connection, "SELECT id FROM teachers ORDER BY id");
        if (levels.isEmpty() || teacherIds.isEmpty()) {
            return;
        }

        String countSql = "SELECT COUNT(*) FROM classes WHERE level_id = ? AND is_active = 1";
        String insertSql = "INSERT INTO classes (name, level_id, teacher_id, capacity, current_students, is_active) " +
                "VALUES (?, ?, ?, ?, 0, 1)";

        int teacherIndex = 0;
        try (PreparedStatement countStmt = connection.prepareStatement(countSql);
             PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            for (LevelRef level : levels) {
                int levelId = level.id();
                countStmt.setInt(1, levelId);
                int existingCount;
                try (ResultSet rs = countStmt.executeQuery()) {
                    existingCount = rs.next() ? rs.getInt(1) : 0;
                }

                int requiredClasses = requiredClassesForLevel(connection, levelId);
                java.util.List<String> suffixes = buildClassSuffixes(requiredClasses);

                for (int i = existingCount; i < suffixes.size(); i++) {
                    insertStmt.setString(1, level.number() + suffixes.get(i));
                    insertStmt.setInt(2, levelId);
                    insertStmt.setInt(3, teacherIds.get(teacherIndex % teacherIds.size()));
                    insertStmt.setInt(4, ADMIN_MAX_STUDENTS_PER_CLASS);
                    insertStmt.addBatch();
                    teacherIndex++;
                }
            }
            insertStmt.executeBatch();
        }
    }

    private static int requiredClassesForLevel(Connection connection, int levelId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE level_id = ? AND status IN ('pending','waiting_list','active')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, levelId);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = rs.next() ? rs.getInt(1) : 0;
                int perClass = Math.max(1, ADMIN_MAX_STUDENTS_PER_CLASS);
                int required = (count + perClass - 1) / perClass;
                return Math.max(1, required);
            }
        }
    }

    private static java.util.List<String> buildClassSuffixes(int requiredCount) {
        int safeCount = Math.max(1, requiredCount);
        java.util.List<String> suffixes = new java.util.ArrayList<>(safeCount);
        for (int i = 0; i < safeCount; i++) {
            suffixes.add(alphaSuffix(i));
        }
        return suffixes;
    }

    private static String alphaSuffix(int index) {
        // 0 -> A, 1 -> B, ..., 25 -> Z, 26 -> AA, ...
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (n >= 0) {
            int r = n % 26;
            sb.insert(0, (char) ('A' + r));
            n = (n / 26) - 1;
        }
        return sb.toString();
    }

    private static java.util.List<LevelRef> loadLevels(Connection connection) throws SQLException {
        java.util.List<LevelRef> levels = new java.util.ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, number FROM levels ORDER BY number")) {
            while (rs.next()) {
                levels.add(new LevelRef(rs.getInt("id"), rs.getInt("number")));
            }
        }
        return levels;
    }

    private record LevelRef(int id, int number) {}

    private static void normalizeClassCapacityLimit(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE classes SET capacity = ? WHERE capacity IS NULL OR capacity > ?"
        )) {
            stmt.setInt(1, ADMIN_MAX_STUDENTS_PER_CLASS);
            stmt.setInt(2, ADMIN_MAX_STUDENTS_PER_CLASS);
            stmt.executeUpdate();
        }
    }

    private static void seedTeacherSpecializations(Connection connection) throws SQLException {
        java.util.List<String> subjectNames = new java.util.ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM subjects WHERE is_active = 1 ORDER BY id")) {
            while (rs.next()) {
                subjectNames.add(rs.getString(1));
            }
        }
        if (subjectNames.isEmpty()) {
            return;
        }

        String selectSql = "SELECT id, specialization FROM teachers ORDER BY id";
        String updateSql = "UPDATE teachers SET specialization = ? WHERE id = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
            int index = 0;
            while (rs.next()) {
                String specialization = rs.getString("specialization");
                if (specialization != null && !specialization.isBlank()) {
                    String trimmed = specialization.trim();
                    if (!"Generaliste".equalsIgnoreCase(trimmed)) {
                        // Si l'enseignant a deja >= 4 matieres, on ne touche pas.
                        int commaCount = trimmed.length() - trimmed.replace(",", "").length();
                        if (commaCount >= 3) {
                            continue;
                        }
                    }
                }
                String s1 = subjectNames.get(index % subjectNames.size());
                String s2 = subjectNames.get((index + 1) % subjectNames.size());
                String s3 = subjectNames.get((index + 2) % subjectNames.size());
                String s4 = subjectNames.get((index + 3) % subjectNames.size());
                updateStmt.setString(1, s1 + ", " + s2 + ", " + s3 + ", " + s4);
                updateStmt.setInt(2, rs.getInt("id"));
                updateStmt.addBatch();
                index++;
            }
            updateStmt.executeBatch();
        }
    }

    private static void seedTeacherClassSubjectAssignments(Connection connection) throws SQLException {
        // Objectif: associer au prof seulement quelques matieres par classe (pas toutes),
        // pour que la liste deroulante de saisie des notes reste realiste.
        cleanupOverseededTeacherSubjects(connection);
        assignDefaultTeacherSubjectsPerClass(connection);
    }

    private static void cleanupOverseededTeacherSubjects(Connection connection) throws SQLException {
        int classCount = queryForInt(connection, "SELECT COUNT(*) FROM classes WHERE is_active = 1");
        if (classCount <= 0) {
            return;
        }
        int tcsCount = queryForInt(connection, "SELECT COUNT(*) FROM teacher_class_subjects");
        // Si on depasse 10 matieres par classe en moyenne, c'est probablement le seed "toutes matieres".
        if (tcsCount > classCount * 10) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM teacher_class_subjects");
            }
            logger.info("teacher_class_subjects cleaned up (overseeded).");
        }
    }

    private static void assignDefaultTeacherSubjectsPerClass(Connection connection) throws SQLException {
        // Derive "taught subjects" from teacher.specialization when possible (comma-separated),
        // otherwise fallback to a small rotating subset. No hard limit at 2: a teacher may teach many.

        java.util.List<SubjectRef> subjects = new java.util.ArrayList<>();
        String subjectsSql = "SELECT id, name, code, min_level, max_level FROM subjects WHERE is_active = 1 ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(subjectsSql)) {
            while (rs.next()) {
                subjects.add(new SubjectRef(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getInt("min_level"),
                        rs.getInt("max_level")
                ));
            }
        }

        java.util.Map<Integer, java.util.List<SubjectRef>> subjectsByLevel = new java.util.HashMap<>();
        for (SubjectRef s : subjects) {
            int min = Math.max(1, s.minLevel());
            int max = Math.max(min, s.maxLevel());
            for (int level = min; level <= max; level++) {
                subjectsByLevel.computeIfAbsent(level, k -> new java.util.ArrayList<>()).add(s);
            }
        }

        java.util.Map<Integer, String> specializationByTeacherId = new java.util.HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, specialization FROM teachers ORDER BY id")) {
            while (rs.next()) {
                specializationByTeacherId.put(rs.getInt("id"), rs.getString("specialization"));
            }
        }

        String classesSql = """
                SELECT c.id AS class_id, c.teacher_id, l.number AS level_number
                FROM classes c
                JOIN levels l ON l.id = c.level_id
                WHERE c.is_active = 1
                ORDER BY c.id
                """;

        String insertSql = "INSERT IGNORE INTO teacher_class_subjects (teacher_id, class_id, subject_id) VALUES (?, ?, ?)";
        try (PreparedStatement select = connection.prepareStatement(classesSql);
             ResultSet rs = select.executeQuery();
             PreparedStatement insert = connection.prepareStatement(insertSql)) {

            while (rs.next()) {
                int classId = rs.getInt("class_id");
                int teacherId = rs.getInt("teacher_id");
                int levelNumber = rs.getInt("level_number");

                java.util.List<SubjectRef> pool = subjectsByLevel.get(levelNumber);
                if (pool == null || pool.isEmpty()) {
                    continue;
                }

                java.util.List<SubjectRef> taught = deriveTaughtSubjects(pool, specializationByTeacherId.get(teacherId));
                if (taught.isEmpty()) {
                    // fallback: 4 rotating subjects max, not a hard "2"
                    int take = Math.min(4, pool.size());
                    int start = Math.floorMod(teacherId + classId, pool.size());
                    taught = new java.util.ArrayList<>();
                    for (int i = 0; i < take; i++) {
                        taught.add(pool.get((start + i) % pool.size()));
                    }
                }

                for (SubjectRef s : taught) {
                    insert.setInt(1, teacherId);
                    insert.setInt(2, classId);
                    insert.setInt(3, s.id());
                    insert.addBatch();
                }
            }

            insert.executeBatch();
        }
    }

    private static java.util.List<SubjectRef> deriveTaughtSubjects(java.util.List<SubjectRef> pool, String specialization) {
        if (specialization == null || specialization.isBlank()) {
            return java.util.List.of();
        }

        java.util.List<String> tokens = java.util.Arrays.stream(specialization.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(t -> t.toLowerCase(java.util.Locale.ROOT))
                .toList();

        if (tokens.isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<SubjectRef> matches = new java.util.ArrayList<>();
        for (SubjectRef s : pool) {
            String name = s.name() != null ? s.name().toLowerCase(java.util.Locale.ROOT) : "";
            String code = s.code() != null ? s.code().toLowerCase(java.util.Locale.ROOT) : "";
            for (String token : tokens) {
                if (!token.isEmpty() && (name.contains(token) || code.contains(token) || token.contains(name))) {
                    matches.add(s);
                    break;
                }
            }
        }
        return matches;
    }

    private static void normalizePendingStudentsToWaitingList(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE students SET status = 'waiting_list' WHERE status = 'pending'"
        )) {
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("Student statuses normalized from pending to waiting_list: " + updated);
            }
        }
    }

    private static void normalizeDemoStudentNames(Connection connection) throws SQLException {
        // Remplace les "Eleve TestXX" par des noms plus realistes pour les demos / screenshots.
        String selectSql = """
                SELECT id
                FROM users
                WHERE role = 'student'
                  AND (
                    LOWER(COALESCE(first_name, '')) = 'eleve'
                    OR LOWER(COALESCE(last_name, '')) LIKE 'test%'
                  )
                ORDER BY id
                """;

        java.util.List<String[]> names = java.util.List.of(
                new String[]{"Amine", "Ben Ali"},
                new String[]{"Yasmine", "Trabelsi"},
                new String[]{"Mohamed", "Salah"},
                new String[]{"Ines", "Mansouri"},
                new String[]{"Ahmed", "Jaballah"},
                new String[]{"Sara", "Boudhina"},
                new String[]{"Omar", "Khalfallah"},
                new String[]{"Mariem", "Haddad"},
                new String[]{"Walid", "Gharbi"},
                new String[]{"Rania", "Ayadi"},
                new String[]{"Houssem", "Sassi"},
                new String[]{"Nour", "Cherif"},
                new String[]{"Hamza", "Mejri"},
                new String[]{"Aya", "Zouari"},
                new String[]{"Seif", "Kammoun"},
                new String[]{"Salma", "Bensaid"},
                new String[]{"Rayen", "Laabidi"},
                new String[]{"Aziz", "Bouazizi"},
                new String[]{"Hiba", "Jlassi"},
                new String[]{"Sami", "Brahmi"},
                new String[]{"Lina", "Tounsi"},
                new String[]{"Kais", "Saidi"},
                new String[]{"Maha", "Ben Youssef"},
                new String[]{"Skander", "Fakhfakh"},
                new String[]{"Asma", "Jouini"},
                new String[]{"Adem", "Kefi"},
                new String[]{"Eya", "Boujemaa"},
                new String[]{"Malek", "Ghozzi"},
                new String[]{"Sarra", "Hamdi"},
                new String[]{"Marwen", "Kallel"},
                new String[]{"Rim", "Ben Romdhane"},
                new String[]{"Fares", "Karray"},
                new String[]{"Nadine", "Chammam"},
                new String[]{"Youssef", "Maazoun"},
                new String[]{"Mouhamed", "Ben Taher"}
        );

        java.util.List<Integer> ids = new java.util.ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        if (ids.isEmpty()) {
            return;
        }

        String updateSql = "UPDATE users SET first_name = ?, last_name = ? WHERE id = ?";
        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
            int idx = 0;
            for (Integer id : ids) {
                String[] pair = names.get(idx % names.size());
                update.setString(1, pair[0]);
                update.setString(2, pair[1]);
                update.setInt(3, id);
                update.addBatch();
                idx++;
            }
            update.executeBatch();
        }
    }

    private static void seedMissingStudents(Connection connection) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users u " +
                "LEFT JOIN students s ON s.user_id = u.id " +
                "WHERE u.role = 'student' AND s.id IS NULL";
        int missingStudents = queryForInt(connection, countSql);
        if (missingStudents <= 0) {
            return;
        }

        java.util.List<Integer> levelIds = loadIds(connection, "SELECT id FROM levels ORDER BY number");
        if (levelIds.isEmpty()) {
            logger.warning("Skipping student bootstrap because levels are missing.");
            return;
        }

        String selectUsersSql = "SELECT u.id, u.last_name FROM users u " +
                "LEFT JOIN students s ON s.user_id = u.id " +
                "WHERE u.role = 'student' AND s.id IS NULL ORDER BY u.id";
        String insertStudentSql = "INSERT INTO students " +
                "(user_id, class_id, level_id, matricule, parent_name, parent_phone, parent_email, address, birth_date, status) " +
                "VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement selectStmt = connection.prepareStatement(selectUsersSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement insertStmt = connection.prepareStatement(insertStudentSql)) {

            int index = 0;
            while (rs.next()) {
                int userId = rs.getInt("id");
                String lastName = rs.getString("last_name");
                int levelId = levelIds.get(index % levelIds.size());

                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, levelId);
                insertStmt.setString(3, String.format("ELV%03d", userId));
                insertStmt.setString(4, "Parent " + (lastName != null ? lastName : userId));
                insertStmt.setString(5, "0000000000");
                insertStmt.setString(6, "parent" + userId + "@school.com");
                insertStmt.setString(7, "Adresse non renseignee");
                insertStmt.setString(8, "01/01/2015");
                insertStmt.setString(9, "waiting_list");
                insertStmt.addBatch();
                index++;
            }

            insertStmt.executeBatch();
            logger.info("Student records inserted for existing student users: " + missingStudents);
        }
    }

    private static void repairSparseDemoStudentAssignments(Connection connection) throws SQLException {
        int totalStudents = queryForInt(connection, "SELECT COUNT(*) FROM students");
        if (totalStudents == 0) {
            return;
        }

        int activeStudents = queryForInt(connection, "SELECT COUNT(*) FROM students WHERE status = 'active'");
        int assignedActiveStudents = queryForInt(
                connection,
                "SELECT COUNT(*) FROM students WHERE status = 'active' AND class_id IS NOT NULL"
        );

        if (!shouldRepairDemoStudents(connection, totalStudents, activeStudents, assignedActiveStudents)) {
            return;
        }

        java.util.Map<Integer, java.util.List<Integer>> classIdsByLevel = loadClassIdsByLevel(connection);
        if (classIdsByLevel.isEmpty()) {
            logger.warning("Skipping demo student repair because no active classes were found.");
            return;
        }

        String selectStudentsSql = "SELECT id, level_id FROM students ORDER BY id";
        String updateStudentSql = "UPDATE students SET status = ?, class_id = ? WHERE id = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectStudentsSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement updateStmt = connection.prepareStatement(updateStudentSql)) {

            java.util.Map<Integer, Integer> classLoad = new java.util.HashMap<>();
            int index = 0;
            while (rs.next()) {
                int studentId = rs.getInt("id");
                int levelId = rs.getInt("level_id");
                String status = computeDemoStatus(index++);
                Integer classId = null;

                if ("active".equals(status)) {
                    classId = pickPreferredClassId(classIdsByLevel, classLoad, levelId);
                }

                updateStmt.setString(1, status);
                updateStmt.setObject(2, classId);
                updateStmt.setInt(3, studentId);
                updateStmt.addBatch();
            }

            updateStmt.executeBatch();
        }

        refreshClassStudentCounts(connection);
        logger.info("Demo student assignments repaired automatically.");
    }

    private static boolean shouldRepairDemoStudents(Connection connection,
                                                    int totalStudents,
                                                    int activeStudents,
                                                    int assignedActiveStudents) throws SQLException {
        if (totalStudents < 10) {
            return false;
        }

        int minimumExpectedActiveStudents = Math.max(3, totalStudents / 5);
        if (activeStudents < minimumExpectedActiveStudents || assignedActiveStudents < minimumExpectedActiveStudents) {
            return true;
        }

        int classesWithActiveStudents = queryForInt(
                connection,
                "SELECT COUNT(*) FROM classes c WHERE EXISTS (" +
                        "SELECT 1 FROM students s WHERE s.class_id = c.id AND s.status = 'active'" +
                        ")"
        );
        if (classesWithActiveStudents == 0) {
            return true;
        }

        double averageActiveStudentsPerUsedClass = assignedActiveStudents / (double) classesWithActiveStudents;
        return averageActiveStudentsPerUsedClass < 2.0;
    }

    private static java.util.Map<Integer, java.util.List<Integer>> loadClassIdsByLevel(Connection connection) throws SQLException {
        java.util.Map<Integer, java.util.List<Integer>> classIdsByLevel = new java.util.LinkedHashMap<>();
        String sql = "SELECT id, level_id FROM classes WHERE is_active = 1 ORDER BY level_id, name, id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int levelId = rs.getInt("level_id");
                classIdsByLevel.computeIfAbsent(levelId, key -> new java.util.ArrayList<>()).add(rs.getInt("id"));
            }
        }

        return classIdsByLevel;
    }

    private static String computeDemoStatus(int index) {
        int bucket = index % 10;
        if (bucket <= 4) {
            return "active";
        }
        if (bucket <= 7) {
            return "waiting_list";
        }
        return "waiting_list";
    }

    private static Integer pickPreferredClassId(java.util.Map<Integer, java.util.List<Integer>> classIdsByLevel,
                                                java.util.Map<Integer, Integer> classLoad,
                                                int levelId) {
        java.util.List<Integer> classIds = classIdsByLevel.get(levelId);
        if (classIds == null || classIds.isEmpty()) {
            return null;
        }

        Integer selectedClassId = null;
        int minLoad = Integer.MAX_VALUE;
        for (Integer classId : classIds) {
            int currentLoad = classLoad.getOrDefault(classId, 0);
            if (currentLoad >= TARGET_STUDENTS_PER_CLASS) {
                continue;
            }
            if (currentLoad < minLoad) {
                minLoad = currentLoad;
                selectedClassId = classId;
            }
        }

        if (selectedClassId == null) {
            selectedClassId = classIds.get(0);
        }
        classLoad.put(selectedClassId, classLoad.getOrDefault(selectedClassId, 0) + 1);
        return selectedClassId;
    }

    private static void refreshClassStudentCounts(Connection connection) throws SQLException {
        String sql = "UPDATE classes c SET current_students = (" +
                "SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.status = 'active'" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private static int countRows(Connection connection, String tableName) throws SQLException {
        return queryForInt(connection, "SELECT COUNT(*) FROM " + tableName);
    }

    private static int queryForInt(Connection connection, String sql) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private static java.util.List<Integer> loadIds(Connection connection, String sql) throws SQLException {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private static boolean isUnknownDatabase(SQLException e) {
        return e.getErrorCode() == 1049
                || (e.getMessage() != null && e.getMessage().contains("Unknown database '" + DB_NAME + "'"));
    }

    private static void initializeDatabaseFromScript() {
        File scriptFile = findDatabaseScript();
        if (scriptFile == null) {
            throw new RuntimeException(
                    "Database missing and script database_init.sql not found in the project."
            );
        }

        try (Connection serverConnection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement statement = serverConnection.createStatement()) {
            for (String sqlStatement : parseSqlStatements(scriptFile)) {
                statement.execute(sqlStatement);
            }
            logger.info("Database initialized successfully from " + scriptFile.getAbsolutePath());
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Unable to initialize database from script", e);
        }
    }

    private static File findDatabaseScript() {
        String[] candidates = {
                "database_init.sql",
                "./database_init.sql"
        };

        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.isFile()) {
                return file.getAbsoluteFile();
            }
        }

        File current = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        for (int i = 0; i < 5 && current != null; i++) {
            File file = new File(current, "database_init.sql");
            if (file.isFile()) {
                return file.getAbsoluteFile();
            }
            current = current.getParentFile();
        }

        return null;
    }

    private static java.util.List<String> parseSqlStatements(File scriptFile) throws IOException {
        java.util.List<String> statements = new java.util.ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(scriptFile.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                    continue;
                }

                currentStatement.append(line).append('\n');

                if (trimmedLine.endsWith(";")) {
                    String statement = currentStatement.toString().trim();
                    if (!statement.isEmpty()) {
                        statements.add(statement.substring(0, statement.length() - 1));
                    }
                    currentStatement.setLength(0);
                }
            }
        }

        String trailingStatement = currentStatement.toString().trim();
        if (!trailingStatement.isEmpty()) {
            statements.add(trailingStatement);
        }

        return statements;
    }

    private static String getConfig(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return defaultValue;
    }

    private static String buildJdbcUrl(String databaseName) {
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(DB_HOST)
                .append(':')
                .append(DB_PORT)
                .append('/');

        if (databaseName != null && !databaseName.isBlank()) {
            url.append(databaseName);
        }

        url.append("?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
        return url.toString();
    }

    private static void ensureMySqlDriverLoaded() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return;
        } catch (ClassNotFoundException ignored) {
            logger.info("MySQL driver not found in runtime classpath, attempting local jar loading");
        }

        File mysqlJar = findMySqlJar();
        if (mysqlJar == null) {
            throw new RuntimeException(
                    "MySQL driver not found. Expected lib/mysql-connector-j-9.6.0.jar in the project."
            );
        }

        try {
            URL jarUrl = mysqlJar.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, DatabaseConnection.class.getClassLoader());
            Driver driver = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, loader)
                    .getDeclaredConstructor()
                    .newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
            logger.info("MySQL driver loaded from local jar: " + mysqlJar.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load MySQL driver from local jar", e);
        }
    }

    private static File findMySqlJar() {
        String[] candidates = {
                "lib/mysql-connector-j-9.6.0.jar",
                "./lib/mysql-connector-j-9.6.0.jar"
        };

        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.isFile()) {
                return file.getAbsoluteFile();
            }
        }

        File current = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        for (int i = 0; i < 5 && current != null; i++) {
            File file = new File(current, "lib/mysql-connector-j-9.6.0.jar");
            if (file.isFile()) {
                return file.getAbsoluteFile();
            }
            current = current.getParentFile();
        }

        return null;
    }

    private static final class DriverShim implements Driver {
        private final Driver driver;

        private DriverShim(Driver driver) {
            this.driver = driver;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }
}
