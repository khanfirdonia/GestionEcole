package services;

import dao.TeacherAssignmentDAO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service pour les affectations enseignant -> classes -> matieres.
 */
public class TeacherAssignmentService {
    private static final Logger logger = Logger.getLogger(TeacherAssignmentService.class.getName());

    private final TeacherAssignmentDAO teacherAssignmentDAO = new TeacherAssignmentDAO();

    public List<TeacherAssignmentDAO.AssignmentDetail> getAssignmentsByTeacher(int teacherId) {
        try {
            return teacherAssignmentDAO.getAssignmentsByTeacher(teacherId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement des affectations enseignant", e);
            return List.of();
        }
    }

    public List<Integer> getSubjectIdsByTeacherAndClass(int teacherId, int classId) {
        try {
            return teacherAssignmentDAO.getSubjectIdsByTeacherAndClass(teacherId, classId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement des matieres affectees", e);
            return List.of();
        }
    }

    public boolean replaceAssignmentsForTeacherAndClass(int teacherId, int classId, List<Integer> subjectIds) {
        try {
            return teacherAssignmentDAO.replaceAssignmentsForTeacherAndClass(teacherId, classId, subjectIds);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise a jour des affectations enseignant", e);
            return false;
        }
    }

    public String buildAssignmentSummary(int teacherId) {
        List<TeacherAssignmentDAO.AssignmentDetail> assignments = getAssignmentsByTeacher(teacherId);
        if (assignments.isEmpty()) {
            return "Aucune affectation";
        }

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (TeacherAssignmentDAO.AssignmentDetail assignment : assignments) {
            String key = assignment.className() + " (" + assignment.levelName() + ")";
            grouped.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(assignment.subjectName());
        }

        return grouped.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue().stream().distinct().collect(Collectors.joining(", ")))
                .collect(Collectors.joining(" | "));
    }
}
