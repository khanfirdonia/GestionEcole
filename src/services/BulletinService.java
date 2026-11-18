package services;

import dao.BulletinDAO;
import models.Bulletin;
import models.Grade;
import models.SchoolYear;
import models.Student;
import models.Trimester;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour la gestion des bulletins.
 */
public class BulletinService {
    private static final Logger logger = Logger.getLogger(BulletinService.class.getName());

    private final BulletinDAO bulletinDAO = new BulletinDAO();
    private final GradeService gradeService = new GradeService();
    private final StudentService studentService = new StudentService();

    public List<Bulletin> getBulletinsByStudent(int studentId) {
        try {
            return bulletinDAO.getBulletinsByStudent(studentId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des bulletins", e);
            return List.of();
        }
    }

    public Bulletin getBulletin(int studentId, int schoolYearId, int trimesterId) {
        try {
            return bulletinDAO.getBulletin(studentId, schoolYearId, trimesterId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation du bulletin", e);
            return null;
        }
    }

    public int saveBulletin(Bulletin bulletin) {
        try {
            if (bulletin == null || bulletin.getStudent() == null || bulletin.getSchoolYear() == null || bulletin.getTrimester() == null) {
                return -1;
            }

            List<Grade> grades = gradeService.getGradesByStudentAndPeriod(
                    bulletin.getStudent().getId(),
                    bulletin.getSchoolYear().getId(),
                    bulletin.getTrimester().getId()
            );
            bulletin.setGrades(grades);
            bulletin.setAverage(calculateWeightedAverage(grades));
            bulletin.setAppreciation(grades.isEmpty() ? "Aucune note" : getAppreciation(bulletin.getAverage()));

            return bulletinDAO.saveBulletin(bulletin);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la sauvegarde du bulletin", e);
            return -1;
        }
    }

    public boolean updateBulletin(Bulletin bulletin) {
        try {
            return bulletinDAO.updateBulletin(bulletin);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise a jour du bulletin", e);
            return false;
        }
    }

    public List<Bulletin> getBulletinsByClass(int classId, int trimesterId) {
        try {
            return bulletinDAO.getBulletinsByClass(classId, trimesterId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la recuperation des bulletins de classe", e);
            return List.of();
        }
    }

    public List<Bulletin> buildClassBulletins(int classId, int schoolYearId, int trimesterId) {
        try {
            List<Student> students = studentService.getStudentsByClass(classId);
            SchoolYear schoolYear = new SchoolYear();
            schoolYear.setId(schoolYearId);
            Trimester trimester = new Trimester();
            trimester.setId(trimesterId);

            List<Bulletin> bulletins = new ArrayList<>();
            for (Student student : students) {
                List<Grade> grades = gradeService.getGradesByStudentAndPeriod(student.getId(), schoolYearId, trimesterId);
                Bulletin bulletin = new Bulletin(student, schoolYear, trimester);
                bulletin.setGrades(grades);
                bulletin.setAverage(calculateWeightedAverage(grades));
                bulletin.setAppreciation(grades.isEmpty() ? "Aucune note" : getAppreciation(bulletin.getAverage()));
                bulletin.setTeacherComment(grades.isEmpty()
                        ? "Aucune note disponible pour cette periode."
                        : "Bulletin genere automatiquement a partir des notes saisies.");
                bulletins.add(bulletin);
            }

            bulletins.sort(Comparator
                    .comparingDouble(Bulletin::getAverage).reversed()
                    .thenComparing(bulletin -> bulletin.getStudent() != null ? bulletin.getStudent().getFullName() : "", String.CASE_INSENSITIVE_ORDER));

            int rank = 1;
            for (Bulletin bulletin : bulletins) {
                bulletin.setRank(rank++);
            }

            return bulletins;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la construction des bulletins de classe", e);
            return List.of();
        }
    }

    public int synchronizeClassBulletins(int classId, int schoolYearId, int trimesterId) {
        int updatedCount = 0;

        try {
            List<Bulletin> bulletins = buildClassBulletins(classId, schoolYearId, trimesterId);
            for (Bulletin bulletin : bulletins) {
                if (bulletin.getGrades() == null || bulletin.getGrades().isEmpty()) {
                    continue;
                }

                Bulletin existing = bulletinDAO.getBulletin(bulletin.getStudent().getId(), schoolYearId, trimesterId);
                if (existing == null) {
                    if (bulletinDAO.saveBulletin(bulletin) > 0) {
                        updatedCount++;
                    }
                } else {
                    bulletin.setId(existing.getId());
                    bulletin.setFinalized(existing.isFinalized());
                    bulletin.setParentsComment(existing.getParentsComment());
                    if (bulletinDAO.updateBulletin(bulletin)) {
                        updatedCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la synchronisation des bulletins", e);
        }

        return updatedCount;
    }

    private double calculateWeightedAverage(List<Grade> grades) {
        if (grades == null || grades.isEmpty()) {
            return 0.0;
        }

        double weightedSum = 0.0;
        double totalCoefficient = 0.0;
        for (Grade grade : grades) {
            int coefficient = grade.getSubject() != null ? Math.max(1, grade.getSubject().getCoefficient()) : 1;
            weightedSum += grade.getValue() * coefficient;
            totalCoefficient += coefficient;
        }

        if (totalCoefficient == 0.0) {
            return 0.0;
        }

        double average = weightedSum / totalCoefficient;
        return Double.parseDouble(String.format(Locale.US, "%.2f", average));
    }

    private String getAppreciation(double average) {
        if (average >= 17) return "Excellent";
        if (average >= 14) return "Tres bien";
        if (average >= 12) return "Bien";
        if (average >= 10) return "Satisfaisant";
        if (average >= 7) return "A ameliorer";
        return "Insatisfaisant";
    }
}
