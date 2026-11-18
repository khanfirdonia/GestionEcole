package services;

import dao.ClassDAO;
import dao.StudentDAO;
import models.SchoolClass;
import models.Student;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour la gestion des eleves
 */
public class StudentService {
    private static final Logger logger = Logger.getLogger(StudentService.class.getName());
    private final StudentDAO studentDAO = new StudentDAO();

    public List<Student> getAllStudents() {
        try {
            return studentDAO.getAllStudents();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des eleves", e);
            return List.of();
        }
    }

    public Student getStudentById(int id) {
        try {
            return studentDAO.getStudentById(id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation de l'eleve", e);
        }
        return null;
    }

    public Student getStudentByUserId(int userId) {
        try {
            return studentDAO.getStudentByUserId(userId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation de l'eleve par utilisateur", e);
        }
        return null;
    }

    public List<Student> getStudentsByClass(int classId) {
        try {
            return studentDAO.getStudentsByClass(classId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des eleves par classe", e);
            return List.of();
        }
    }

    public List<Student> getStudentsByClassForGradeEntry(int classId) {
        try {
            return studentDAO.getStudentsByClassForGradeEntry(classId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des eleves pour la saisie des notes", e);
            return List.of();
        }
    }

    public List<Student> getStudentsByLevel(int levelId) {
        try {
            return studentDAO.getStudentsByLevel(levelId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des eleves par niveau", e);
            return List.of();
        }
    }

    public List<Student> getPendingStudents() {
        try {
            return studentDAO.getPendingStudents();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des eleves en attente", e);
            return List.of();
        }
    }

    public int createStudent(Student student) {
        try {
            return studentDAO.saveStudent(student);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la creation de l'eleve", e);
        }
        return -1;
    }

    public boolean updateStudent(Student student) {
        try {
            return studentDAO.updateStudent(student);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise a jour de l'eleve", e);
        }
        return false;
    }

    /**
     * Approuver l'inscription d'un eleve.
     */
    public boolean approveStudent(int studentId, int classId) {
        return approveStudent(studentId, classId, null, null, null);
    }

    public boolean approveStudent(int studentId, int classId, Integer approvedByUserId,
                                  String approvedByUsername, String studentName) {
        try {
            boolean approved = studentDAO.approveStudent(studentId, classId);
            if (approved) {
                logger.info("Eleve approuve: ID = " + studentId);
                AuditService.logStudentApproval(
                        approvedByUserId != null ? approvedByUserId : 0,
                        approvedByUsername != null ? approvedByUsername : "system",
                        studentId,
                        studentName != null ? studentName : "Eleve " + studentId
                );
            }
            return approved;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de l'approbation de l'eleve", e);
        }
        return false;
    }

    /**
     * Rejeter l'inscription d'un eleve.
     */
    public boolean rejectStudent(int studentId) {
        try {
            return studentDAO.rejectStudent(studentId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du rejet de l'eleve", e);
        }
        return false;
    }

    /**
     * Retourne jusqu'a 3 classes disponibles pour le niveau donne.
     */
    public List<SchoolClass> getSuggestedClasses(int levelId) {
        try {
            ClassDAO classDAO = new ClassDAO();
            List<SchoolClass> classes = classDAO.getClassesByLevel(levelId);
            return classes.stream()
                    .sorted((c1, c2) -> {
                        if (c1.isFull() && !c2.isFull()) return 1;
                        if (!c1.isFull() && c2.isFull()) return -1;
                        return c1.getName().compareToIgnoreCase(c2.getName());
                    })
                    .toList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des classes suggerees", e);
            return List.of();
        }
    }
}
