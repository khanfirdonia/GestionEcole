package services;

import dao.InscriptionDAO;
import dao.SchoolYearDAO;
import dao.StudentDAO;
import models.SchoolYear;
import models.Student;
import utils.Constants;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour la gestion des inscriptions (une par eleve et par annee).
 */
public class InscriptionService {
    private static final Logger logger = Logger.getLogger(InscriptionService.class.getName());

    private final InscriptionDAO inscriptionDAO = new InscriptionDAO();
    private final SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    /**
     * Cree/met a jour l'inscription de l'annee active pour un eleve donne.
     * (Contrainte: une seule inscription par eleve et par annee, geree par UNIQUE + upsert.)
     */
    public boolean upsertActiveYearInscriptionForStudent(int studentId, int levelId, String status) {
        try {
            SchoolYear active = schoolYearDAO.getActiveSchoolYear();
            if (active == null) {
                logger.warning("Aucune annee scolaire active.");
                return false;
            }
            int id = inscriptionDAO.upsertInscription(studentId, active.getId(), levelId, status);
            return id > 0;
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Regle metier inscription refusee pour eleve ID: " + studentId, e);
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de l'upsert inscription (annee active) pour eleve", e);
            return false;
        }
    }

    /**
     * Cree/met a jour les inscriptions de l'annee active a partir des eleves existants.
     * Utile pour initialiser rapidement l'application sans ecran de saisie d'inscription.
     */
    public int ensureActiveYearInscriptions() {
        try {
            SchoolYear active = schoolYearDAO.getActiveSchoolYear();
            if (active == null) {
                logger.warning("Aucune annee scolaire active.");
                return 0;
            }

            List<Student> students = studentDAO.getAllStudents();
            int processed = 0;
            for (Student student : students) {
                if (student == null || student.getLevel() == null) {
                    continue;
                }
                String status = deriveInscriptionStatus(student.getStatus());
                try {
                    int inscriptionId = inscriptionDAO.upsertInscription(student.getId(), active.getId(), student.getLevel().getId(), status);
                    if (inscriptionId > 0) {
                        processed++;
                    }
                } catch (IllegalStateException e) {
                    logger.log(Level.WARNING,
                            "Synchronisation ignoree pour l'eleve ID " + student.getId() + " : " + e.getMessage());
                }
            }
            logger.info("Inscriptions (annee active) synchronisees: " + processed);
            return processed;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la synchronisation des inscriptions", e);
            return 0;
        }
    }

    private String deriveInscriptionStatus(String studentStatus) {
        if (Constants.STUDENT_STATUS_ACTIVE.equals(studentStatus)) {
            return "active";
        }
        if (Constants.STUDENT_STATUS_INACTIVE.equals(studentStatus)) {
            return "inactive";
        }
        return "pending";
    }
}
