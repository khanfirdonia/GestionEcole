package services;

import dao.*;
import models.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour la gestion des classes
 */
public class ClassService {
    private static final Logger logger = Logger.getLogger(ClassService.class.getName());
    private ClassDAO classDAO = new ClassDAO();

    public List<SchoolClass> getAllClasses() {
        try {
            return classDAO.getAllClasses();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des classes", e);
            return List.of();
        }
    }

    public SchoolClass getClassById(int id) {
        try {
            return classDAO.getClassById(id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération de la classe", e);
        }
        return null;
    }

    public List<SchoolClass> getClassesByLevel(int levelId) {
        try {
            return classDAO.getClassesByLevel(levelId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des classes par niveau", e);
            return List.of();
        }
    }

    public List<SchoolClass> getClassesByTeacher(int teacherId) {
        try {
            return classDAO.getClassesByTeacher(teacherId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la récupération des classes de l'enseignant", e);
            return List.of();
        }
    }

    public int createClass(SchoolClass schoolClass) {
        try {
            return classDAO.saveClass(schoolClass);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la création de la classe", e);
        }
        return -1;
    }

    public boolean updateClass(SchoolClass schoolClass) {
        try {
            return classDAO.updateClass(schoolClass);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la mise à jour de la classe", e);
        }
        return false;
    }
}
