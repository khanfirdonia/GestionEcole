package models;

/**
 * Classe modèle pour les notes (grades)
 * Représente la note d'un élève dans une matière pour un trimestre
 */
public class Grade {
    private int id;
    private Student student;
    private Subject subject;
    private Teacher teacher;
    private SchoolYear schoolYear;
    private Trimester trimester;
    private double value; // Note entre 0 et 20
    private String remarks; // Remarques de l'enseignant
    private String recordedDate; // Format: "dd/MM/yyyy HH:mm:ss"
    private boolean isValidated;

    // Constructeurs
    public Grade() {}

    public Grade(Student student, Subject subject, Teacher teacher, 
                 SchoolYear schoolYear, Trimester trimester, double value) {
        this.student = student;
        this.subject = subject;
        this.teacher = teacher;
        this.schoolYear = schoolYear;
        this.trimester = trimester;
        this.value = value;
        this.isValidated = false;
    }

    public Grade(int id, Student student, Subject subject, Teacher teacher,
                 SchoolYear schoolYear, Trimester trimester, double value, String remarks) {
        this.id = id;
        this.student = student;
        this.subject = subject;
        this.teacher = teacher;
        this.schoolYear = schoolYear;
        this.trimester = trimester;
        this.value = value;
        this.remarks = remarks;
        this.isValidated = false;
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

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
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

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        // Validation : la note doit être entre 0 et 20
        if (value >= 0 && value <= 20) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("La note doit être entre 0 et 20");
        }
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(String recordedDate) {
        this.recordedDate = recordedDate;
    }

    public boolean isValidated() {
        return isValidated;
    }

    public void setValidated(boolean validated) {
        isValidated = validated;
    }

    /**
     * Retourne une appréciation basée sur la valeur de la note
     */
    public String getAppreciation() {
        if (value >= 17) return "Excellent";
        if (value >= 14) return "Très bien";
        if (value >= 12) return "Bien";
        if (value >= 10) return "Satisfaisant";
        if (value >= 7) return "À améliorer";
        return "Insatisfaisant";
    }

    @Override
    public String toString() {
        return "Grade{" +
                "id=" + id +
                ", student=" + (student != null ? student.getFullName() : "N/A") +
                ", subject=" + (subject != null ? subject.getName() : "N/A") +
                ", value=" + value +
                ", trimester=" + (trimester != null ? trimester.getName() : "N/A") +
                '}';
    }
}
