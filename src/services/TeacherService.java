package services;

import dao.*;
import models.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour la gestion des enseignants
 */
public class TeacherService {
    private static final Logger logger = Logger.getLogger(TeacherService.class.getName());
    private TeacherDAO teacherDAO = new TeacherDAO();
    private ClassDAO classDAO = new ClassDAO();

    public List<Teacher> getAllTeachers() {
        try {
            return teacherDAO.getAllTeachers();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des enseignants", e);
            return List.of();
        }
    }

    public Teacher getTeacherById(int id) {
        try {
            return teacherDAO.getTeacherById(id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'enseignant", e);
        }
        return null;
    }

    public Teacher getTeacherByUserId(int userId) {
        try {
            return teacherDAO.getTeacherByUserId(userId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de l'enseignant", e);
        }
        return null;
    }

    /**
     * Récupérer les classes d'un enseignant
     */
    public List<SchoolClass> getTeacherClasses(int teacherId) {
        try {
            return classDAO.getClassesByTeacher(teacherId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des classes", e);
            return List.of();
        }
    }

    public int createTeacher(Teacher teacher) {
        try {
            return teacherDAO.saveTeacher(teacher);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la création de l'enseignant", e);
        }
        return -1;
    }

    public boolean updateTeacher(Teacher teacher) {
        try {
            return teacherDAO.updateTeacher(teacher);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour de l'enseignant", e);
        }
        return false;
    }
}
