package models;

/**
 * Classe modèle pour l'année scolaire
 * Une année scolaire est divisée en 3 trimestres
 */
public class SchoolYear {
    private int id;
    private String label; // Ex: "2023-2024"
    private int startYear; // 2023
    private int endYear;   // 2024
    private String startDate; // Format: "dd/MM/yyyy"
    private String endDate;   // Format: "dd/MM/yyyy"
    private boolean isActive;

    // Constructeurs
    public SchoolYear() {}

    public SchoolYear(String label, int startYear, int endYear, String startDate, String endDate) {
        this.label = label;
        this.startYear = startYear;
        this.endYear = endYear;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
    }

    public SchoolYear(int id, String label, int startYear, int endYear, String startDate, String endDate, boolean isActive) {
        this.id = id;
        this.label = label;
        this.startYear = startYear;
        this.endYear = endYear;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return label;
    }
}
