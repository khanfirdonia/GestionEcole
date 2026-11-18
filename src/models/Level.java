package models;

/**
 * Classe modèle pour les niveaux scolaires
 * Représente les 6 niveaux du système : 1ère à 6ème année
 */
public class Level {
    private int id;
    private String name; // "1ère année", "2ème année", etc.
    private int number; // 1, 2, 3, 4, 5, 6
    private String description;

    // Constructeurs
    public Level() {}

    public Level(int id, String name, int number) {
        this.id = id;
        this.name = name;
        this.number = number;
    }

    public Level(String name, int number) {
        this.name = name;
        this.number = number;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name;
    }
}
