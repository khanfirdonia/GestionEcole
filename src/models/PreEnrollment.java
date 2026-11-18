package models;

/**
 * Demande de pre-inscription (sans compte utilisateur).
 * Elle sera validee par l'admin qui creera ensuite le compte + dossier eleve.
 */
public class PreEnrollment {
    private int id;
    private SchoolYear schoolYear;
    private Level requestedLevel;
    private String firstName;
    private String lastName;
    private String birthDate; // dd/MM/yyyy
    private String address;
    private String parentName;
    private String parentPhone;
    private String parentEmail;
    private String status; // pending | approved | rejected

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(SchoolYear schoolYear) {
        this.schoolYear = schoolYear;
    }

    public Level getRequestedLevel() {
        return requestedLevel;
    }

    public void setRequestedLevel(Level requestedLevel) {
        this.requestedLevel = requestedLevel;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    @Override
    public String toString() {
        String year = schoolYear != null ? schoolYear.getLabel() : "N/A";
        String level = requestedLevel != null ? requestedLevel.getName() : "N/A";
        return getFullName() + " (Annee: " + year + ", Niveau: " + level + ")";
    }
}

