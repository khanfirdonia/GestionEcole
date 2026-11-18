package models;

/**
 * Classe modèle pour les classes (groupes d'élèves)
 * Une classe est associée à un niveau et un enseignant
 */
public class SchoolClass {
    private int id;
    private String name; // Ex: "1A", "2B", etc.
    private Level level;
    private Teacher teacher;
    private int capacity; // Capacité maximale
    private int currentStudents; // Nombre d'élèves actuels
    private boolean isActive;
    private String taughtSubjects;

    // Constructeurs
    public SchoolClass() {}

    public SchoolClass(String name, Level level, Teacher teacher, int capacity) {
        this.name = name;
        this.level = level;
        this.teacher = teacher;
        this.capacity = capacity;
        this.currentStudents = 0;
        this.isActive = true;
    }

    public SchoolClass(int id, String name, Level level, Teacher teacher, int capacity, int currentStudents) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.teacher = teacher;
        this.capacity = capacity;
        this.currentStudents = currentStudents;
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

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrentStudents() {
        return currentStudents;
    }

    public void setCurrentStudents(int currentStudents) {
        this.currentStudents = currentStudents;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isFull() {
        return currentStudents >= capacity;
    }

    public String getTaughtSubjects() {
        return taughtSubjects;
    }

    public void setTaughtSubjects(String taughtSubjects) {
        this.taughtSubjects = taughtSubjects;
    }

    public int getAvailableSeats() {
        return capacity - currentStudents;
    }

    @Override
    public String toString() {
        String levelName = level != null ? level.getName() : "N/A";
        return name + " (Niveau: " + levelName + ", places libres: " + getAvailableSeats() + ")";
    }
}
