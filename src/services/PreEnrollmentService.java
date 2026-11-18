package services;

import dao.PreEnrollmentDAO;
import dao.SchoolYearDAO;
import models.PreEnrollment;
import models.SchoolYear;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour les pre-inscriptions (sans compte).
 */
public class PreEnrollmentService {
    private static final Logger logger = Logger.getLogger(PreEnrollmentService.class.getName());

    private final PreEnrollmentDAO preEnrollmentDAO = new PreEnrollmentDAO();
    private final SchoolYearDAO schoolYearDAO = new SchoolYearDAO();

    public int createPreEnrollment(int requestedLevelId,
                                   String firstName, String lastName, String birthDate,
                                   String address, String parentName, String parentPhone, String parentEmail) {
        try {
            SchoolYear active = schoolYearDAO.getActiveSchoolYear();
            if (active == null) {
                logger.warning("Aucune annee scolaire active.");
                return -1;
            }
            return preEnrollmentDAO.createPreEnrollment(
                    active.getId(),
                    requestedLevelId,
                    firstName,
                    lastName,
                    birthDate,
                    address,
                    parentName,
                    parentPhone,
                    parentEmail
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de la creation pre-inscription", e);
            return -1;
        }
    }

    public List<PreEnrollment> getPendingPreEnrollments() {
        return preEnrollmentDAO.getPendingPreEnrollments();
    }

    public boolean markApproved(int id) {
        return preEnrollmentDAO.updateStatus(id, "approved");
    }

    public boolean markRejected(int id) {
        return preEnrollmentDAO.updateStatus(id, "rejected");
    }
}
