package models;

import java.util.List;

/**
 * Classe modèle pour les bulletins scolaires
 * Le bulletin affiche les notes et la performance générale d'un élève pour un trimestre
 */
public class Bulletin {
    private int id;
    private Student student;
    private SchoolYear schoolYear;
    private Trimester trimester;
    private List<Grade> grades;
    private double average; // Moyenne générale du trimestre
    private int rank; // Classement dans la classe
    private String appreciation; // Appréciation générale
    private String teacherComment;
    private String parentsComment;
    private String recordedDate; // Format: "dd/MM/yyyy HH:mm:ss"
    private boolean isFinalized; // Le bulletin est-il finalisé?

    // Constructeurs
    public Bulletin() {}

    public Bulletin(Student student, SchoolYear schoolYear, Trimester trimester) {
        this.student = student;
        this.schoolYear = schoolYear;
        this.trimester = trimester;
        this.isFinalized = false;
    }

    public Bulletin(int id, Student student, SchoolYear schoolYear, Trimester trimester,
                    double average, int rank, String appreciation) {
        this.id = id;
        this.student = student;
        this.schoolYear = schoolYear;
        this.trimester = trimester;
        this.average = average;
        this.rank = rank;
        this.appreciation = appreciation;
        this.isFinalized = false;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public SchoolYear getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(SchoolYear schoolYear) {
        this.schoolYear = schoolYear;
    }

    public Trimester getTrimester() {
        return trimester;
    }

    public void setTrimester(Trimester trimester) {
        this.trimester = trimester;
    }

    public List<Grade> getGrades() {
        return grades;
    }

    public void setGrades(List<Grade> grades) {
        this.grades = grades;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getAppreciation() {
        return appreciation;
    }

    public void setAppreciation(String appreciation) {
        this.appreciation = appreciation;
    }

    public String getTeacherComment() {
        return teacherComment;
    }

    public void setTeacherComment(String teacherComment) {
        this.teacherComment = teacherComment;
    }

    public String getParentsComment() {
        return parentsComment;
    }

    public void setParentsComment(String parentsComment) {
        this.parentsComment = parentsComment;
    }

    public String getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(String recordedDate) {
        this.recordedDate = recordedDate;
    }

    public boolean isFinalized() {
        return isFinalized;
    }

    public void setFinalized(boolean finalized) {
        isFinalized = finalized;
    }

    /**
     * Calcule la moyenne basée sur les notes saisies
     */
    public double calculateAverage() {
        if (grades == null || grades.isEmpty()) {
            return 0.0;
        }

        double sum = 0;
        double totalCoefficient = 0;
        for (Grade grade : grades) {
            int coefficient = grade.getSubject() != null ? Math.max(1, grade.getSubject().getCoefficient()) : 1;
            sum += grade.getValue() * coefficient;
            totalCoefficient += coefficient;
        }
        if (totalCoefficient == 0) {
            return 0.0;
        }
        return sum / totalCoefficient;
    }

    @Override
    public String toString() {
        return "Bulletin{" +
                "id=" + id +
                ", student=" + (student != null ? student.getFullName() : "N/A") +
                ", trimester=" + (trimester != null ? trimester.getName() : "N/A") +
                ", average=" + average +
                ", appreciation='" + appreciation + '\'' +
                '}';
    }
}
