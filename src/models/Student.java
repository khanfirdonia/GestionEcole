package models;

/**
 * Classe modèle pour les élèves
 * Étend la classe User avec des informations spécifiques aux élèves
 */
public class Student {
    private int id;
    private User user;
    private SchoolClass schoolClass;
    private Level level;
    private String matricule; // Numéro de matricule
    private String parentName;
    private String parentPhone;
    private String parentEmail;
    private String address;
    private String birthDate; // Format: "dd/MM/yyyy"
    private String status; // "active", "inactive", "waiting_list"
    private boolean isActive;

    // Constructeurs
    public Student() {}

    public Student(User user) {
        this.user = user;
        this.status = "waiting_list";
        this.isActive = true;
    }

    public Student(int id, User user, SchoolClass schoolClass, Level level, 
                   String matricule, String status) {
        this.id = id;
        this.user = user;
        this.schoolClass = schoolClass;
        this.level = level;
        this.matricule = matricule;
        this.status = status;
        this.isActive = true;
    }

    public Student(int id, User user, SchoolClass schoolClass, Level level, String matricule,
                   String parentName, String parentPhone, String address, String birthDate, String status) {
        this.id = id;
        this.user = user;
        this.schoolClass = schoolClass;
        this.level = level;
        this.matricule = matricule;
        this.parentName = parentName;
        this.parentPhone = parentPhone;
        this.address = address;
        this.birthDate = birthDate;
        this.status = status;
        this.isActive = true;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public void setSchoolClass(SchoolClass schoolClass) {
        this.schoolClass = schoolClass;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public String getParentEmail() {
        return parentEmail;
    }

    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getFullName() {
        return user != null ? user.getFullName() : "N/A";
    }

    public String getClassName() {
        return schoolClass != null ? schoolClass.getName() : "Non assigné";
    }

    @Override
    public String toString() {
        return getFullName() + " (" + matricule + ")";
    }
}
