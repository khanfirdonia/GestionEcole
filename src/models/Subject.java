package models;

/**
 * Classe modèle pour les matières scolaires
 * Représente les matières enseignées dans l'école
 */
public class Subject {
    private int id;
    private String name; // "Arabe", "Français", "Maths", etc.
    private String code;
    private int coefficient; // Par défaut: 1
    private int minLevel; // Niveau minimum (ex: Français à partir de 3ème)
    private int maxLevel; // Niveau maximum
    private String description;
    private boolean isActive;

    // Constructeurs
    public Subject() {}

    public Subject(String name, String code, int minLevel, int maxLevel) {
        this.name = name;
        this.code = code;
        this.coefficient = 1;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.isActive = true;
    }

    public Subject(int id, String name, String code, int coefficient, int minLevel, int maxLevel) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.coefficient = coefficient;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.isActive = true;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(int coefficient) {
        this.coefficient = coefficient;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return name;
    }
}
