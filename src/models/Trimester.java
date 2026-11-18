package models;

/**
 * Classe modèle pour les trimestres scolaires
 * L'année scolaire est divisée en 3 trimestres
 */
public class Trimester {
    private int id;
    private String name; // "T1", "T2", "T3"
    private int number; // 1, 2, 3
    private String startDate; // Format: "dd/MM/yyyy"
    private String endDate;   // Format: "dd/MM/yyyy"
    private String description;

    // Constructeurs
    public Trimester() {}

    public Trimester(int id, String name, int number, String startDate, String endDate) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Trimester(String name, int number, String startDate, String endDate) {
        this.name = name;
        this.number = number;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name + " (" + startDate + " - " + endDate + ")";
    }
}
