package utils;

/**
 * Classe utilitaire pour les constantes de l'application
 */
public class Constants {
    
    // Rôles utilisateurs
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";
    
    // Statuts d'élève
    public static final String STUDENT_STATUS_PENDING = "pending";
    public static final String STUDENT_STATUS_ACTIVE = "active";
    public static final String STUDENT_STATUS_INACTIVE = "inactive";
    public static final String STUDENT_STATUS_WAITING_LIST = "waiting_list";
    
    // Appréciations
    public static final String[] APPRECIATIONS = {
        "Excellent",
        "Très bien",
        "Bien",
        "Satisfaisant",
        "À améliorer",
        "Insatisfaisant"
    };
    
    // Notes
    public static final double MIN_GRADE = 0.0;
    public static final double MAX_GRADE = 20.0;
    public static final double PASSING_GRADE = 10.0;
    
    // Trimestres
    public static final int TRIMESTER_1 = 1;
    public static final int TRIMESTER_2 = 2;
    public static final int TRIMESTER_3 = 3;
    
    // Niveaux
    public static final int LEVEL_1 = 1;
    public static final int LEVEL_2 = 2;
    public static final int LEVEL_3 = 3;
    public static final int LEVEL_4 = 4;
    public static final int LEVEL_5 = 5;
    public static final int LEVEL_6 = 6;
    public static final int MAX_LEVELS = 6;
    
    // Capacité de classe
    public static final int MIN_CLASS_CAPACITY = 15;
    public static final int MAX_CLASS_CAPACITY = 20;
    public static final int DEFAULT_CLASS_CAPACITY = 20;
    
    // Coefficient des matières
    public static final int DEFAULT_COEFFICIENT = 1;
    
    // Matières
    public static final String[] SUBJECTS = {
        "Arabe",
        "Français",
        "Mathématiques",
        "Sciences",
        "Éducation Physique",
        "Arts Plastiques",
        "Islamique",
        "Informatique"
    };
    
    // Dates pour les trimestres (format JJ/MM)
    public static final String TRIMESTER_1_START = "01/09";
    public static final String TRIMESTER_1_END = "30/11";
    public static final String TRIMESTER_2_START = "01/12";
    public static final String TRIMESTER_2_END = "28/02";
    public static final String TRIMESTER_3_START = "01/03";
    public static final String TRIMESTER_3_END = "30/06";
    
    // Messages
    public static final String SUCCESS_MESSAGE = "Opération réussie";
    public static final String ERROR_MESSAGE = "Une erreur s'est produite";
    public static final String VALIDATION_ERROR = "Erreur de validation";
    
    // Application
    public static final String APP_NAME = "Gestion Scolaire";
    public static final String APP_VERSION = "1.0.0";
    public static final String WINDOW_TITLE = "Gestion Scolaire - v" + APP_VERSION;
}
