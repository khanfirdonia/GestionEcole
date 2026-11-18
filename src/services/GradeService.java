package services;

import dao.GradeDAO;
import models.Grade;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour la gestion des notes (grades)
 */
public class GradeService {
    private static final Logger logger = Logger.getLogger(GradeService.class.getName());
    private final GradeDAO gradeDAO = new GradeDAO();

    /**
     * Recuperer les notes d'un eleve pour un trimestre.
     */
    public List<Grade> getGradesByStudentAndTrimester(int studentId, int trimesterId) {
        try {
            return gradeDAO.getGradesByStudentAndTrimester(studentId, trimesterId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des notes", e);
            return List.of();
        }
    }

    public List<Grade> getGradesByStudentAndPeriod(int studentId, int schoolYearId, int trimesterId) {
        try {
            return gradeDAO.getGradesByStudentAndPeriod(studentId, schoolYearId, trimesterId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des notes par periode", e);
            return List.of();
        }
    }

    /**
     * Recuperer une note specifique.
     */
    public Grade getGrade(int studentId, int subjectId, int trimesterId) {
        try {
            return gradeDAO.getGrade(studentId, subjectId, trimesterId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation de la note", e);
        }
        return null;
    }

    /**
     * Sauvegarder une note.
     */
    public int saveGrade(Grade grade) {
        try {
            if (grade.getValue() < 0 || grade.getValue() > 20) {
                logger.warning("Valeur de note invalide: " + grade.getValue());
                return -1;
            }
            return gradeDAO.saveGrade(grade);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la sauvegarde de la note", e);
        }
        return -1;
    }

    /**
     * Mettre a jour une note.
     */
    public boolean updateGrade(Grade grade) {
        try {
            if (grade.getValue() < 0 || grade.getValue() > 20) {
                logger.warning("Valeur de note invalide: " + grade.getValue());
                return false;
            }
            return gradeDAO.updateGrade(grade);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise a jour de la note", e);
        }
        return false;
    }

    /**
     * Recuperer les notes d'une classe pour une matiere et un trimestre.
     */
    public List<Grade> getGradesByClassAndSubject(int classId, int subjectId, int trimesterId) {
        try {
            return gradeDAO.getGradesByClassAndSubject(classId, subjectId, trimesterId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des notes", e);
            return List.of();
        }
    }

    public List<Grade> getGradesByClassAndSubject(int classId, int subjectId, int schoolYearId, int trimesterId) {
        try {
            return gradeDAO.getGradesByClassAndSubject(classId, subjectId, schoolYearId, trimesterId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des notes par periode", e);
            return List.of();
        }
    }

    /**
     * Calculer la moyenne d'un eleve pour un trimestre.
     */
    public double calculateStudentAverage(int studentId, int trimesterId) {
        try {
            List<Grade> grades = gradeDAO.getGradesByStudentAndTrimester(studentId, trimesterId);
            if (grades.isEmpty()) {
                return 0.0;
            }

            double sum = 0;
            int count = 0;
            for (Grade grade : grades) {
                sum += grade.getValue();
                count++;
            }
            return sum / count;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du calcul de la moyenne", e);
        }
        return 0.0;
    }
}
