package models;

/**
 * Classe modèle pour les enseignants
 * Étend la classe User avec des informations spécifiques aux enseignants
 */
public class Teacher {
    private int id;
    private User user;
    private String matricule; // Numéro d'identification unique
    private String specialization; // Spécialisation/expertise
    private String qualifications; // Diplômes et certifications
    private String department; // Département/Section
    private String phone;
    private String email;
    private boolean isActive;

    // Constructeurs
    public Teacher() {}

    public Teacher(User user) {
        this.user = user;
    }

    public Teacher(int id, User user, String matricule, String specialization) {
        this.id = id;
        this.user = user;
        this.matricule = matricule;
        this.specialization = specialization;
        this.isActive = true;
    }

    public Teacher(int id, User user, String matricule, String specialization, 
                   String qualifications, String department, String phone, String email) {
        this.id = id;
        this.user = user;
        this.matricule = matricule;
        this.specialization = specialization;
        this.qualifications = qualifications;
        this.department = department;
        this.phone = phone;
        this.email = email;
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

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getQualifications() {
        return qualifications;
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    @Override
    public String toString() {
        return getFullName() + " (" + matricule + ")";
    }
}
