package services;

import dao.PreEnrollmentDAO;
import dao.UserDAO;
import models.PreEnrollment;
import models.Student;
import models.User;
import utils.Constants;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service admin: valide une pre-inscription en creant le compte eleve + dossier + inscription.
 */
public class PreEnrollmentAdminService {
    private static final Logger logger = Logger.getLogger(PreEnrollmentAdminService.class.getName());

    public record Credentials(String username, String password) {}

    private final PreEnrollmentDAO preEnrollmentDAO = new PreEnrollmentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final StudentManagementService studentManagementService = new StudentManagementService();

    public Credentials approve(PreEnrollment pe) {
        if (pe == null || pe.getRequestedLevel() == null) {
            return null;
        }

        try {
            String username = generateUniqueUsername(pe.getFirstName(), pe.getLastName());
            String password = generateTempPassword();

            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(Constants.ROLE_STUDENT);
            user.setFirstName(pe.getFirstName());
            user.setLastName(pe.getLastName());
            user.setEmail(null);
            user.setPhone(null);
            user.setActive(true);

            Student student = new Student();
            student.setUser(user);
            student.setLevel(pe.getRequestedLevel());
            student.setSchoolClass(null);
            student.setMatricule("PE" + pe.getId());
            student.setParentName(pe.getParentName());
            student.setParentPhone(pe.getParentPhone());
            student.setParentEmail(pe.getParentEmail());
            student.setAddress(pe.getAddress());
            student.setBirthDate(pe.getBirthDate());
            student.setStatus(Constants.STUDENT_STATUS_WAITING_LIST);

            int studentId = studentManagementService.createStudent(user, student, true);
            if (studentId <= 0) {
                return null;
            }

            preEnrollmentDAO.updateStatus(pe.getId(), "approved");
            return new Credentials(username, password);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de l'approbation pre-inscription", e);
            return null;
        }
    }

    public boolean reject(int preEnrollmentId) {
        return preEnrollmentDAO.updateStatus(preEnrollmentId, "rejected");
    }

    private String generateUniqueUsername(String firstName, String lastName) {
        String base = buildUsernameBase(firstName, lastName);
        if (base.isBlank()) {
            base = "student";
        }

        String candidate = base;
        int suffix = 1;
        while (userDAO.usernameExists(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }

    private static String buildUsernameBase(String firstName, String lastName) {
        String fn = firstName != null ? firstName.trim() : "";
        String ln = lastName != null ? lastName.trim() : "";
        if (fn.isEmpty() && ln.isEmpty()) {
            return "";
        }
        String initial = fn.isEmpty() ? "" : fn.substring(0, 1);
        String raw = (initial + ln).toLowerCase(Locale.ROOT).replace(" ", "");
        raw = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        raw = raw.replaceAll("[^a-z0-9_]", "");
        if (raw.length() < 3) {
            raw = (raw + "student").substring(0, Math.min(12, raw.length() + 6));
        }
        if (raw.length() > 20) {
            raw = raw.substring(0, 20);
        }
        return raw;
    }

    private static String generateTempPassword() {
        int n = new Random().nextInt(9000) + 1000;
        return "Eleve@" + n;
    }
}
