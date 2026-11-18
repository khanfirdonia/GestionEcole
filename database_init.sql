-- Initialisation de la base MySQL (XAMPP) pour l'application GestionEcoleE
-- Base: db_school_management

CREATE DATABASE IF NOT EXISTS db_school_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE db_school_management;

-- Tables de configuration
CREATE TABLE IF NOT EXISTS levels (
  id INT PRIMARY KEY AUTO_INCREMENT,
  number INT UNIQUE,
  name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subjects (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(20) UNIQUE,
  coefficient INT DEFAULT 1,
  min_level INT,
  max_level INT,
  is_active BOOLEAN DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trimesters (
  id INT PRIMARY KEY AUTO_INCREMENT,
  number INT UNIQUE,
  name VARCHAR(20) NOT NULL,
  start_date VARCHAR(10),
  end_date VARCHAR(10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS school_years (
  id INT PRIMARY KEY AUTO_INCREMENT,
  label VARCHAR(50) NOT NULL,
  start_year INT,
  end_year INT,
  start_date VARCHAR(10),
  end_date VARCHAR(10),
  is_active BOOLEAN DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Utilisateurs
CREATE TABLE IF NOT EXISTS users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role ENUM('admin', 'teacher', 'student') NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  email VARCHAR(100),
  phone VARCHAR(20),
  is_active BOOLEAN DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Metier: enseignants / classes / eleves
CREATE TABLE IF NOT EXISTS teachers (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT UNIQUE NOT NULL,
  matricule VARCHAR(20) UNIQUE,
  specialization VARCHAR(100),
  qualifications TEXT,
  department VARCHAR(50),
  phone VARCHAR(20),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS classes (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  level_id INT NOT NULL,
  teacher_id INT NOT NULL,
  capacity INT DEFAULT 20,
  current_students INT DEFAULT 0,
  is_active BOOLEAN DEFAULT 1,
  FOREIGN KEY (level_id) REFERENCES levels(id),
  FOREIGN KEY (teacher_id) REFERENCES teachers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS students (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT UNIQUE NOT NULL,
  class_id INT,
  level_id INT NOT NULL,
  matricule VARCHAR(20) UNIQUE,
  parent_name VARCHAR(100),
  parent_phone VARCHAR(20),
  parent_email VARCHAR(100),
  address TEXT,
  birth_date VARCHAR(10),
  status ENUM('pending', 'active', 'inactive', 'waiting_list') DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (class_id) REFERENCES classes(id),
  FOREIGN KEY (level_id) REFERENCES levels(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Inscriptions annuelles
CREATE TABLE IF NOT EXISTS inscriptions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  student_id INT NOT NULL,
  school_year_id INT NOT NULL,
  level_id INT NOT NULL,
  class_id INT,
  status ENUM('pending', 'active', 'inactive') DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  FOREIGN KEY (school_year_id) REFERENCES school_years(id) ON DELETE CASCADE,
  FOREIGN KEY (level_id) REFERENCES levels(id),
  FOREIGN KEY (class_id) REFERENCES classes(id),
  UNIQUE KEY unique_inscription (student_id, school_year_id),
  INDEX idx_inscriptions_student (student_id),
  INDEX idx_inscriptions_year (school_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Pre-inscriptions (sans compte)
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notes / bulletins / audit
CREATE TABLE IF NOT EXISTS grades (
  id INT PRIMARY KEY AUTO_INCREMENT,
  student_id INT NOT NULL,
  subject_id INT NOT NULL,
  teacher_id INT NOT NULL,
  school_year_id INT NOT NULL,
  trimester_id INT NOT NULL,
  value DECIMAL(5,2),
  remarks TEXT,
  recorded_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  is_validated BOOLEAN DEFAULT 0,
  FOREIGN KEY (student_id) REFERENCES students(id),
  FOREIGN KEY (subject_id) REFERENCES subjects(id),
  FOREIGN KEY (teacher_id) REFERENCES teachers(id),
  FOREIGN KEY (school_year_id) REFERENCES school_years(id),
  FOREIGN KEY (trimester_id) REFERENCES trimesters(id),
  UNIQUE KEY unique_grade (student_id, subject_id, trimester_id, school_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bulletins (
  id INT PRIMARY KEY AUTO_INCREMENT,
  student_id INT NOT NULL,
  school_year_id INT NOT NULL,
  trimester_id INT NOT NULL,
  average DECIMAL(5,2),
  rank INT,
  appreciation VARCHAR(50),
  teacher_comment TEXT,
  parents_comment TEXT,
  recorded_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  is_finalized BOOLEAN DEFAULT 0,
  FOREIGN KEY (student_id) REFERENCES students(id),
  FOREIGN KEY (school_year_id) REFERENCES school_years(id),
  FOREIGN KEY (trimester_id) REFERENCES trimesters(id),
  UNIQUE KEY unique_bulletin (student_id, school_year_id, trimester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT,
  username VARCHAR(50),
  action_type VARCHAR(50),
  details TEXT,
  action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Donnees initiales (idempotent via INSERT IGNORE)
INSERT IGNORE INTO levels (id, number, name) VALUES
  (1, 1, '1ere annee'), (2, 2, '2eme annee'), (3, 3, '3eme annee'),
  (4, 4, '4eme annee'), (5, 5, '5eme annee'), (6, 6, '6eme annee');

INSERT IGNORE INTO subjects (id, name, code, coefficient, min_level, max_level, is_active) VALUES
  (1, 'Arabe', 'AR', 1, 1, 6, 1),
  (2, 'Francais', 'FR', 1, 3, 6, 1),
  (3, 'Mathematiques', 'MATH', 1, 1, 6, 1),
  (4, 'Sciences', 'SCI', 1, 1, 6, 1),
  (5, 'Education Physique', 'EPS', 1, 1, 6, 1),
  (6, 'Arts Plastiques', 'ART', 1, 1, 6, 1),
  (7, 'Islamique', 'ISL', 1, 1, 6, 1),
  (8, 'Informatique', 'INFO', 1, 5, 6, 1);

INSERT IGNORE INTO trimesters (id, number, name, start_date, end_date) VALUES
  (1, 1, 'T1', '01/09', '30/11'),
  (2, 2, 'T2', '01/12', '28/02'),
  (3, 3, 'T3', '01/03', '30/06');

INSERT IGNORE INTO school_years (id, label, start_year, end_year, start_date, end_date, is_active) VALUES
  (1, '2025-2026', 2025, 2026, '01/09/2025', '30/06/2026', 1);

-- Comptes demo
INSERT IGNORE INTO users (id, username, password, role, first_name, last_name, email, phone, is_active)
VALUES (1, 'admin', 'admin123', 'admin', 'Super', 'Admin', 'admin@school.com', NULL, 1);

INSERT IGNORE INTO users (id, username, password, role, first_name, last_name, email, phone, is_active)
VALUES (2, 'teacher01', 'pass123', 'teacher', 'Demo', 'Teacher', 'teacher@school.com', NULL, 1);

INSERT IGNORE INTO users (id, username, password, role, first_name, last_name, email, phone, is_active)
VALUES (3, 'student01', 'pass123', 'student', 'Demo', 'Student', 'student@school.com', NULL, 1);

-- Assurer que l'auto-increment ne va pas re-utiliser des ids manuels.
ALTER TABLE users AUTO_INCREMENT = 200;
ALTER TABLE students AUTO_INCREMENT = 200;
ALTER TABLE inscriptions AUTO_INCREMENT = 200;

INSERT IGNORE INTO teachers (id, user_id, matricule, specialization, qualifications, department, phone)
VALUES (1, 2, 'T001', 'Mathematiques', NULL, 'Sciences', NULL);

-- 2 classes demo pour niveau 1
INSERT IGNORE INTO classes (id, name, level_id, teacher_id, capacity, current_students, is_active)
VALUES (1, '1A', 1, 1, 20, 0, 1);
INSERT IGNORE INTO classes (id, name, level_id, teacher_id, capacity, current_students, is_active)
VALUES (2, '1B', 1, 1, 20, 0, 1);

INSERT IGNORE INTO students (id, user_id, class_id, level_id, matricule, parent_name, parent_phone, parent_email, address, birth_date, status)
VALUES (1, 3, NULL, 1, 'S001', 'Parent Demo', '00000000', NULL, 'Adresse demo', '01/01/2015', 'waiting_list');

-- 80 eleves en liste d'attente (status = waiting_list, comptes desactives)
INSERT IGNORE INTO users (id, username, password, role, first_name, last_name, email, phone, is_active)
VALUES
  (100, 's_att_01', 'temp123', 'student', 'Amine', 'Ben Ali', NULL, NULL, 0),
  (101, 's_att_02', 'temp123', 'student', 'Yasmine', 'Trabelsi', NULL, NULL, 0),
  (102, 's_att_03', 'temp123', 'student', 'Mohamed', 'Salah', NULL, NULL, 0),
  (103, 's_att_04', 'temp123', 'student', 'Ines', 'Mansouri', NULL, NULL, 0),
  (104, 's_att_05', 'temp123', 'student', 'Ahmed', 'Jaballah', NULL, NULL, 0),
  (105, 's_att_06', 'temp123', 'student', 'Sara', 'Boudhina', NULL, NULL, 0),
  (106, 's_att_07', 'temp123', 'student', 'Omar', 'Khalfallah', NULL, NULL, 0),
  (107, 's_att_08', 'temp123', 'student', 'Mariem', 'Haddad', NULL, NULL, 0),
  (108, 's_att_09', 'temp123', 'student', 'Walid', 'Gharbi', NULL, NULL, 0),
  (109, 's_att_10', 'temp123', 'student', 'Rania', 'Ayadi', NULL, NULL, 0),
  (110, 's_att_11', 'temp123', 'student', 'Houssem', 'Sassi', NULL, NULL, 0),
  (111, 's_att_12', 'temp123', 'student', 'Nour', 'Cherif', NULL, NULL, 0),
  (112, 's_att_13', 'temp123', 'student', 'Hamza', 'Mejri', NULL, NULL, 0),
  (113, 's_att_14', 'temp123', 'student', 'Aya', 'Zouari', NULL, NULL, 0),
  (114, 's_att_15', 'temp123', 'student', 'Seif', 'Kammoun', NULL, NULL, 0),
  (115, 's_att_16', 'temp123', 'student', 'Salma', 'Bensaid', NULL, NULL, 0),
  (116, 's_att_17', 'temp123', 'student', 'Rayen', 'Laabidi', NULL, NULL, 0),
  (117, 's_att_18', 'temp123', 'student', 'Aziz', 'Bouazizi', NULL, NULL, 0),
  (118, 's_att_19', 'temp123', 'student', 'Hiba', 'Jlassi', NULL, NULL, 0),
  (119, 's_att_20', 'temp123', 'student', 'Sami', 'Brahmi', NULL, NULL, 0),
  (120, 's_att_21', 'temp123', 'student', 'Lina', 'Tounsi', NULL, NULL, 0),
  (121, 's_att_22', 'temp123', 'student', 'Kais', 'Saidi', NULL, NULL, 0),
  (122, 's_att_23', 'temp123', 'student', 'Maha', 'Ben Youssef', NULL, NULL, 0),
  (123, 's_att_24', 'temp123', 'student', 'Nadia', 'Mekki', NULL, NULL, 0),
  (124, 's_att_25', 'temp123', 'student', 'Karim', 'Ghannouchi', NULL, NULL, 0),
  (125, 's_att_26', 'temp123', 'student', 'Leila', 'Baccouche', NULL, NULL, 0),
  (126, 's_att_27', 'temp123', 'student', 'Youssef', 'Masmoudi', NULL, NULL, 0),
  (127, 's_att_28', 'temp123', 'student', 'Meriem', 'Abid', NULL, NULL, 0),
  (128, 's_att_29', 'temp123', 'student', 'Zakaria', 'Ben Amor', NULL, NULL, 0),
  (129, 's_att_30', 'temp123', 'student', 'Aicha', 'Khelifi', NULL, NULL, 0),
  (130, 's_att_31', 'temp123', 'student', 'Tarek', 'Mabrouk', NULL, NULL, 0),
  (131, 's_att_32', 'temp123', 'student', 'Imen', 'Dridi', NULL, NULL, 0),
  (132, 's_att_33', 'temp123', 'student', 'Bilel', 'Chakroun', NULL, NULL, 0),
  (133, 's_att_34', 'temp123', 'student', 'Sarra', 'Ghedira', NULL, NULL, 0),
  (134, 's_att_35', 'temp123', 'student', 'Adem', 'Ben Salem', NULL, NULL, 0),
  (135, 's_att_36', 'temp123', 'student', 'Rahma', 'Jedidi', NULL, NULL, 0),
  (136, 's_att_37', 'temp123', 'student', 'Oussama', 'Kchaou', NULL, NULL, 0),
  (137, 's_att_38', 'temp123', 'student', 'Hanen', 'Mrad', NULL, NULL, 0),
  (138, 's_att_39', 'temp123', 'student', 'Skander', 'Bouhlel', NULL, NULL, 0),
  (139, 's_att_40', 'temp123', 'student', 'Rim', 'Bouaziz', NULL, NULL, 0),
  (140, 's_att_41', 'temp123', 'student', 'Fares', 'Mnif', NULL, NULL, 0),
  (141, 's_att_42', 'temp123', 'student', 'Asma', 'Charfi', NULL, NULL, 0),
  (142, 's_att_43', 'temp123', 'student', 'Mohamed Ali', 'Ayari', NULL, NULL, 0),
  (143, 's_att_44', 'temp123', 'student', 'Nesrine', 'Brahim', NULL, NULL, 0),
  (144, 's_att_45', 'temp123', 'student', 'Walid', 'Rekik', NULL, NULL, 0),
  (145, 's_att_46', 'temp123', 'student', 'Chaima', 'Mhamdi', NULL, NULL, 0),
  (146, 's_att_47', 'temp123', 'student', 'Mohamed', 'Khemiri', NULL, NULL, 0),
  (147, 's_att_48', 'temp123', 'student', 'Eya', 'Ben Othman', NULL, NULL, 0),
  (148, 's_att_49', 'temp123', 'student', 'Sofiene', 'Jaziri', NULL, NULL, 0),
  (149, 's_att_50', 'temp123', 'student', 'Manel', 'Triki', NULL, NULL, 0),
  (150, 's_att_51', 'temp123', 'student', 'Yassin', 'Karray', NULL, NULL, 0),
  (151, 's_att_52', 'temp123', 'student', 'Ikram', 'Belhaj', NULL, NULL, 0),
  (152, 's_att_53', 'temp123', 'student', 'Nidhal', 'Hamza', NULL, NULL, 0),
  (153, 's_att_54', 'temp123', 'student', 'Rania', 'Boussetta', NULL, NULL, 0),
  (154, 's_att_55', 'temp123', 'student', 'Mahdi', 'Kooli', NULL, NULL, 0),
  (155, 's_att_56', 'temp123', 'student', 'Alaa', 'Bennour', NULL, NULL, 0),
  (156, 's_att_57', 'temp123', 'student', 'Selem', 'Tlili', NULL, NULL, 0),
  (157, 's_att_58', 'temp123', 'student', 'Yosra', 'Aouadi', NULL, NULL, 0),
  (158, 's_att_59', 'temp123', 'student', 'Nourhene', 'Dhaoui', NULL, NULL, 0),
  (159, 's_att_60', 'temp123', 'student', 'Omar', 'Ghribi', NULL, NULL, 0),
  (160, 's_att_61', 'temp123', 'student', 'Lina', 'Masmoudi', NULL, NULL, 0),
  (161, 's_att_62', 'temp123', 'student', 'Anis', 'Belaid', NULL, NULL, 0),
  (162, 's_att_63', 'temp123', 'student', 'Sabrine', 'Heni', NULL, NULL, 0),
  (163, 's_att_64', 'temp123', 'student', 'Mehdi', 'Baccar', NULL, NULL, 0),
  (164, 's_att_65', 'temp123', 'student', 'Hela', 'Kouki', NULL, NULL, 0),
  (165, 's_att_66', 'temp123', 'student', 'Bassem', 'Gdira', NULL, NULL, 0),
  (166, 's_att_67', 'temp123', 'student', 'Afef', 'Mekni', NULL, NULL, 0),
  (167, 's_att_68', 'temp123', 'student', 'Khalil', 'Sfar', NULL, NULL, 0),
  (168, 's_att_69', 'temp123', 'student', 'Mouna', 'Zribi', NULL, NULL, 0),
  (169, 's_att_70', 'temp123', 'student', 'Zied', 'Khlifi', NULL, NULL, 0),
  (170, 's_att_71', 'temp123', 'student', 'Amira', 'Khelil', NULL, NULL, 0),
  (171, 's_att_72', 'temp123', 'student', 'Hamza', 'Mrad', NULL, NULL, 0),
  (172, 's_att_73', 'temp123', 'student', 'Tasnim', 'Ghariani', NULL, NULL, 0),
  (173, 's_att_74', 'temp123', 'student', 'Skander', 'Abidi', NULL, NULL, 0),
  (174, 's_att_75', 'temp123', 'student', 'Wiem', 'Khadhraoui', NULL, NULL, 0),
  (175, 's_att_76', 'temp123', 'student', 'Bilel', 'Mekki', NULL, NULL, 0),
  (176, 's_att_77', 'temp123', 'student', 'Ameni', 'Jlassi', NULL, NULL, 0),
  (177, 's_att_78', 'temp123', 'student', 'Fedi', 'Chakroun', NULL, NULL, 0),
  (178, 's_att_79', 'temp123', 'student', 'Meriem', 'Ben Romdhane', NULL, NULL, 0),
  (179, 's_att_80', 'temp123', 'student', 'Oussama', 'Dhouib', NULL, NULL, 0);

INSERT IGNORE INTO students (id, user_id, class_id, level_id, matricule, parent_name, parent_phone, parent_email, address, birth_date, status)
VALUES
  (100, 100, NULL, 1, 'WL001', 'Parent Amine', '11111111', NULL, 'Adresse 1', '10/02/2015', 'waiting_list'),
  (101, 101, NULL, 1, 'WL002', 'Parent Yasmine', '11111112', NULL, 'Adresse 2', '22/11/2015', 'waiting_list'),
  (102, 102, NULL, 1, 'WL003', 'Parent Mohamed', '11111113', NULL, 'Adresse 3', '05/05/2015', 'waiting_list'),
  (103, 103, NULL, 1, 'WL004', 'Parent Ines', '11111114', NULL, 'Adresse 4', '19/03/2015', 'waiting_list'),
  (104, 104, NULL, 1, 'WL005', 'Parent Ahmed', '11111115', NULL, 'Adresse 5', '29/08/2015', 'waiting_list'),
  (105, 105, NULL, 1, 'WL006', 'Parent Sara', '11111116', NULL, 'Adresse 6', '14/09/2015', 'waiting_list'),
  (106, 106, NULL, 1, 'WL007', 'Parent Omar', '11111117', NULL, 'Adresse 7', '07/07/2015', 'waiting_list'),
  (107, 107, NULL, 1, 'WL008', 'Parent Mariem', '11111118', NULL, 'Adresse 8', '11/12/2015', 'waiting_list'),
  (108, 108, NULL, 2, 'WL009', 'Parent Walid', '11111119', NULL, 'Adresse 9', '17/01/2014', 'waiting_list'),
  (109, 109, NULL, 2, 'WL010', 'Parent Rania', '11111120', NULL, 'Adresse 10', '03/06/2014', 'waiting_list'),
  (110, 110, NULL, 2, 'WL011', 'Parent Houssem', '11111121', NULL, 'Adresse 11', '25/04/2014', 'waiting_list'),
  (111, 111, NULL, 2, 'WL012', 'Parent Nour', '11111122', NULL, 'Adresse 12', '30/10/2014', 'waiting_list'),
  (112, 112, NULL, 3, 'WL013', 'Parent Hamza', '11111123', NULL, 'Adresse 13', '09/02/2013', 'waiting_list'),
  (113, 113, NULL, 3, 'WL014', 'Parent Aya', '11111124', NULL, 'Adresse 14', '16/06/2013', 'waiting_list'),
  (114, 114, NULL, 3, 'WL015', 'Parent Seif', '11111125', NULL, 'Adresse 15', '01/09/2013', 'waiting_list'),
  (115, 115, NULL, 3, 'WL016', 'Parent Salma', '11111126', NULL, 'Adresse 16', '12/12/2013', 'waiting_list'),
  (116, 116, NULL, 4, 'WL017', 'Parent Rayen', '11111127', NULL, 'Adresse 17', '08/03/2012', 'waiting_list'),
  (117, 117, NULL, 4, 'WL018', 'Parent Aziz', '11111128', NULL, 'Adresse 18', '21/05/2012', 'waiting_list'),
  (118, 118, NULL, 4, 'WL019', 'Parent Hiba', '11111129', NULL, 'Adresse 19', '28/07/2012', 'waiting_list'),
  (119, 119, NULL, 5, 'WL020', 'Parent Sami', '11111130', NULL, 'Adresse 20', '18/01/2011', 'waiting_list'),
  (120, 120, NULL, 5, 'WL021', 'Parent Lina', '11111131', NULL, 'Adresse 21', '02/04/2011', 'waiting_list'),
  (121, 121, NULL, 6, 'WL022', 'Parent Kais', '11111132', NULL, 'Adresse 22', '27/09/2010', 'waiting_list'),
  (122, 122, NULL, 6, 'WL023', 'Parent Maha', '11111133', NULL, 'Adresse 23', '15/12/2010', 'waiting_list'),
  (123, 123, NULL, 1, 'WL024', 'Parent Nadia', '11111134', NULL, 'Adresse 24', '04/01/2016', 'waiting_list'),
  (124, 124, NULL, 1, 'WL025', 'Parent Karim', '11111135', NULL, 'Adresse 25', '15/03/2016', 'waiting_list'),
  (125, 125, NULL, 1, 'WL026', 'Parent Leila', '11111136', NULL, 'Adresse 26', '28/05/2016', 'waiting_list'),
  (126, 126, NULL, 1, 'WL027', 'Parent Youssef', '11111137', NULL, 'Adresse 27', '09/07/2016', 'waiting_list'),
  (127, 127, NULL, 1, 'WL028', 'Parent Meriem', '11111138', NULL, 'Adresse 28', '17/09/2015', 'waiting_list'),
  (128, 128, NULL, 1, 'WL029', 'Parent Zakaria', '11111139', NULL, 'Adresse 29', '24/11/2015', 'waiting_list'),
  (129, 129, NULL, 1, 'WL030', 'Parent Aicha', '11111140', NULL, 'Adresse 30', '02/02/2016', 'waiting_list'),
  (130, 130, NULL, 1, 'WL031', 'Parent Tarek', '11111141', NULL, 'Adresse 31', '12/04/2016', 'waiting_list'),
  (131, 131, NULL, 1, 'WL032', 'Parent Imen', '11111142', NULL, 'Adresse 32', '19/06/2016', 'waiting_list'),
  (132, 132, NULL, 1, 'WL033', 'Parent Bilel', '11111143', NULL, 'Adresse 33', '30/08/2015', 'waiting_list'),
  (133, 133, NULL, 1, 'WL034', 'Parent Sarra', '11111144', NULL, 'Adresse 34', '06/10/2015', 'waiting_list'),
  (134, 134, NULL, 1, 'WL035', 'Parent Adem', '11111145', NULL, 'Adresse 35', '21/12/2015', 'waiting_list'),
  (135, 135, NULL, 1, 'WL036', 'Parent Rahma', '11111146', NULL, 'Adresse 36', '14/01/2016', 'waiting_list'),
  (136, 136, NULL, 1, 'WL037', 'Parent Oussama', '11111147', NULL, 'Adresse 37', '26/03/2016', 'waiting_list'),
  (137, 137, NULL, 1, 'WL038', 'Parent Hanen', '11111148', NULL, 'Adresse 38', '11/05/2016', 'waiting_list'),
  (138, 138, NULL, 1, 'WL039', 'Parent Skander', '11111149', NULL, 'Adresse 39', '23/07/2016', 'waiting_list'),
  (139, 139, NULL, 1, 'WL040', 'Parent Rim', '11111150', NULL, 'Adresse 40', '05/09/2015', 'waiting_list'),
  (140, 140, NULL, 1, 'WL041', 'Parent Fares', '11111151', NULL, 'Adresse 41', '18/11/2015', 'waiting_list'),
  (141, 141, NULL, 1, 'WL042', 'Parent Asma', '11111152', NULL, 'Adresse 42', '07/02/2016', 'waiting_list'),
  (142, 142, NULL, 1, 'WL043', 'Parent Mohamed Ali', '11111153', NULL, 'Adresse 43', '29/04/2016', 'waiting_list'),
  (143, 143, NULL, 2, 'WL044', 'Parent Nesrine', '11111154', NULL, 'Adresse 44', '14/01/2015', 'waiting_list'),
  (144, 144, NULL, 2, 'WL045', 'Parent Walid', '11111155', NULL, 'Adresse 45', '28/02/2015', 'waiting_list'),
  (145, 145, NULL, 2, 'WL046', 'Parent Chaima', '11111156', NULL, 'Adresse 46', '09/03/2015', 'waiting_list'),
  (146, 146, NULL, 2, 'WL047', 'Parent Mohamed', '11111157', NULL, 'Adresse 47', '21/04/2015', 'waiting_list'),
  (147, 147, NULL, 2, 'WL048', 'Parent Eya', '11111158', NULL, 'Adresse 48', '06/05/2015', 'waiting_list'),
  (148, 148, NULL, 2, 'WL049', 'Parent Sofiene', '11111159', NULL, 'Adresse 49', '19/06/2015', 'waiting_list'),
  (149, 149, NULL, 2, 'WL050', 'Parent Manel', '11111160', NULL, 'Adresse 50', '02/07/2015', 'waiting_list'),
  (150, 150, NULL, 2, 'WL051', 'Parent Yassin', '11111161', NULL, 'Adresse 51', '15/08/2015', 'waiting_list'),
  (151, 151, NULL, 2, 'WL052', 'Parent Ikram', '11111162', NULL, 'Adresse 52', '27/09/2014', 'waiting_list'),
  (152, 152, NULL, 2, 'WL053', 'Parent Nidhal', '11111163', NULL, 'Adresse 53', '11/10/2014', 'waiting_list'),
  (153, 153, NULL, 2, 'WL054', 'Parent Rania', '11111164', NULL, 'Adresse 54', '24/11/2014', 'waiting_list'),
  (154, 154, NULL, 2, 'WL055', 'Parent Mahdi', '11111165', NULL, 'Adresse 55', '05/12/2014', 'waiting_list'),
  (155, 155, NULL, 2, 'WL056', 'Parent Alaa', '11111166', NULL, 'Adresse 56', '18/01/2015', 'waiting_list'),
  (156, 156, NULL, 2, 'WL057', 'Parent Selem', '11111167', NULL, 'Adresse 57', '03/02/2015', 'waiting_list'),
  (157, 157, NULL, 2, 'WL058', 'Parent Yosra', '11111168', NULL, 'Adresse 58', '16/03/2015', 'waiting_list'),
  (158, 158, NULL, 2, 'WL059', 'Parent Nourhene', '11111169', NULL, 'Adresse 59', '29/04/2015', 'waiting_list'),
  (159, 159, NULL, 2, 'WL060', 'Parent Omar', '11111170', NULL, 'Adresse 60', '10/05/2015', 'waiting_list'),
  (160, 160, NULL, 2, 'WL061', 'Parent Lina', '11111171', NULL, 'Adresse 61', '22/06/2015', 'waiting_list'),
  (161, 161, NULL, 2, 'WL062', 'Parent Anis', '11111172', NULL, 'Adresse 62', '04/07/2015', 'waiting_list'),
  (162, 162, NULL, 2, 'WL063', 'Parent Sabrine', '11111173', NULL, 'Adresse 63', '17/08/2015', 'waiting_list'),
  (163, 163, NULL, 2, 'WL064', 'Parent Mehdi', '11111174', NULL, 'Adresse 64', '30/09/2014', 'waiting_list'),
  (164, 164, NULL, 2, 'WL065', 'Parent Hela', '11111175', NULL, 'Adresse 65', '12/10/2014', 'waiting_list'),
  (165, 165, NULL, 2, 'WL066', 'Parent Bassem', '11111176', NULL, 'Adresse 66', '25/11/2014', 'waiting_list'),
  (166, 166, NULL, 2, 'WL067', 'Parent Afef', '11111177', NULL, 'Adresse 67', '07/12/2014', 'waiting_list'),
  (167, 167, NULL, 2, 'WL068', 'Parent Khalil', '11111178', NULL, 'Adresse 68', '19/01/2015', 'waiting_list'),
  (168, 168, NULL, 2, 'WL069', 'Parent Mouna', '11111179', NULL, 'Adresse 69', '02/02/2015', 'waiting_list'),
  (169, 169, NULL, 2, 'WL070', 'Parent Zied', '11111180', NULL, 'Adresse 70', '14/03/2015', 'waiting_list'),
  (170, 170, NULL, 2, 'WL071', 'Parent Amira', '11111181', NULL, 'Adresse 71', '26/04/2015', 'waiting_list'),
  (171, 171, NULL, 2, 'WL072', 'Parent Hamza', '11111182', NULL, 'Adresse 72', '09/05/2015', 'waiting_list'),
  (172, 172, NULL, 2, 'WL073', 'Parent Tasnim', '11111183', NULL, 'Adresse 73', '21/06/2015', 'waiting_list'),
  (173, 173, NULL, 2, 'WL074', 'Parent Skander', '11111184', NULL, 'Adresse 74', '03/07/2015', 'waiting_list'),
  (174, 174, NULL, 2, 'WL075', 'Parent Wiem', '11111185', NULL, 'Adresse 75', '16/08/2015', 'waiting_list'),
  (175, 175, NULL, 2, 'WL076', 'Parent Bilel', '11111186', NULL, 'Adresse 76', '28/09/2014', 'waiting_list'),
  (176, 176, NULL, 2, 'WL077', 'Parent Ameni', '11111187', NULL, 'Adresse 77', '10/10/2014', 'waiting_list'),
  (177, 177, NULL, 2, 'WL078', 'Parent Fedi', '11111188', NULL, 'Adresse 78', '23/11/2014', 'waiting_list'),
  (178, 178, NULL, 2, 'WL079', 'Parent Meriem', '11111189', NULL, 'Adresse 79', '05/12/2014', 'waiting_list'),
  (179, 179, NULL, 2, 'WL080', 'Parent Oussama', '11111190', NULL, 'Adresse 80', '18/01/2015', 'waiting_list');

-- Inscription (annee active) pour ces eleves: une seule par eleve/an
INSERT IGNORE INTO inscriptions (id, student_id, school_year_id, level_id, class_id, status)
VALUES
  (100, 100, 1, 1, NULL, 'pending'),
  (101, 101, 1, 1, NULL, 'pending'),
  (102, 102, 1, 1, NULL, 'pending'),
  (103, 103, 1, 1, NULL, 'pending'),
  (104, 104, 1, 1, NULL, 'pending'),
  (105, 105, 1, 1, NULL, 'pending'),
  (106, 106, 1, 1, NULL, 'pending'),
  (107, 107, 1, 1, NULL, 'pending'),
  (108, 108, 1, 2, NULL, 'pending'),
  (109, 109, 1, 2, NULL, 'pending'),
  (110, 110, 1, 2, NULL, 'pending'),
  (111, 111, 1, 2, NULL, 'pending'),
  (112, 112, 1, 3, NULL, 'pending'),
  (113, 113, 1, 3, NULL, 'pending'),
  (114, 114, 1, 3, NULL, 'pending'),
  (115, 115, 1, 3, NULL, 'pending'),
  (116, 116, 1, 4, NULL, 'pending'),
  (117, 117, 1, 4, NULL, 'pending'),
  (118, 118, 1, 4, NULL, 'pending'),
  (119, 119, 1, 5, NULL, 'pending'),
  (120, 120, 1, 5, NULL, 'pending'),
  (121, 121, 1, 6, NULL, 'pending'),
  (122, 122, 1, 6, NULL, 'pending'),
  (123, 123, 1, 1, NULL, 'pending'),
  (124, 124, 1, 1, NULL, 'pending'),
  (125, 125, 1, 1, NULL, 'pending'),
  (126, 126, 1, 1, NULL, 'pending'),
  (127, 127, 1, 1, NULL, 'pending'),
  (128, 128, 1, 1, NULL, 'pending'),
  (129, 129, 1, 1, NULL, 'pending'),
  (130, 130, 1, 1, NULL, 'pending'),
  (131, 131, 1, 1, NULL, 'pending'),
  (132, 132, 1, 1, NULL, 'pending'),
  (133, 133, 1, 1, NULL, 'pending'),
  (134, 134, 1, 1, NULL, 'pending'),
  (135, 135, 1, 1, NULL, 'pending'),
  (136, 136, 1, 1, NULL, 'pending'),
  (137, 137, 1, 1, NULL, 'pending'),
  (138, 138, 1, 1, NULL, 'pending'),
  (139, 139, 1, 1, NULL, 'pending'),
  (140, 140, 1, 1, NULL, 'pending'),
  (141, 141, 1, 1, NULL, 'pending'),
  (142, 142, 1, 1, NULL, 'pending'),
  (143, 143, 1, 2, NULL, 'pending'),
  (144, 144, 1, 2, NULL, 'pending'),
  (145, 145, 1, 2, NULL, 'pending'),
  (146, 146, 1, 2, NULL, 'pending'),
  (147, 147, 1, 2, NULL, 'pending'),
  (148, 148, 1, 2, NULL, 'pending'),
  (149, 149, 1, 2, NULL, 'pending'),
  (150, 150, 1, 2, NULL, 'pending'),
  (151, 151, 1, 2, NULL, 'pending'),
  (152, 152, 1, 2, NULL, 'pending'),
  (153, 153, 1, 2, NULL, 'pending'),
  (154, 154, 1, 2, NULL, 'pending'),
  (155, 155, 1, 2, NULL, 'pending'),
  (156, 156, 1, 2, NULL, 'pending'),
  (157, 157, 1, 2, NULL, 'pending'),
  (158, 158, 1, 2, NULL, 'pending'),
  (159, 159, 1, 2, NULL, 'pending'),
  (160, 160, 1, 2, NULL, 'pending'),
  (161, 161, 1, 2, NULL, 'pending'),
  (162, 162, 1, 2, NULL, 'pending'),
  (163, 163, 1, 2, NULL, 'pending'),
  (164, 164, 1, 2, NULL, 'pending'),
  (165, 165, 1, 2, NULL, 'pending'),
  (166, 166, 1, 2, NULL, 'pending'),
  (167, 167, 1, 2, NULL, 'pending'),
  (168, 168, 1, 2, NULL, 'pending'),
  (169, 169, 1, 2, NULL, 'pending'),
  (170, 170, 1, 2, NULL, 'pending'),
  (171, 171, 1, 2, NULL, 'pending'),
  (172, 172, 1, 2, NULL, 'pending'),
  (173, 173, 1, 2, NULL, 'pending'),
  (174, 174, 1, 2, NULL, 'pending'),
  (175, 175, 1, 2, NULL, 'pending'),
  (176, 176, 1, 2, NULL, 'pending'),
  (177, 177, 1, 2, NULL, 'pending'),
  (178, 178, 1, 2, NULL, 'pending'),
  (179, 179, 1, 2, NULL, 'pending');
