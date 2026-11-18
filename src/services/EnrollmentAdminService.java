package services;

import dao.ClassDAO;
import dao.InscriptionDAO;
import dao.LevelDAO;
import dao.SchoolYearDAO;
import dao.StudentDAO;
import models.Inscription;
import models.Level;
import models.SchoolClass;
import models.SchoolYear;
import utils.Constants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Actions admin: generation de classes et affectation automatique.
 */
public class EnrollmentAdminService {
    private static final Logger logger = Logger.getLogger(EnrollmentAdminService.class.getName());

    private final SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
    private final LevelDAO levelDAO = new LevelDAO();
    private final InscriptionDAO inscriptionDAO = new InscriptionDAO();
    private final ClassDAO classDAO = new ClassDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    public int generateClassesForActiveYear() {
        SchoolYear active = schoolYearDAO.getActiveSchoolYear();
        if (active == null) {
            return 0;
        }

        int created = 0;
        for (Level level : levelDAO.getAllLevels()) {
            int count = inscriptionDAO.countInscriptionsByYearAndLevel(active.getId(), level.getId());
            int required = Math.max(1, (count + Constants.MAX_CLASS_CAPACITY - 1) / Constants.MAX_CLASS_CAPACITY);
            created += classDAO.ensureClassCountForLevel(level.getId(), level.getNumber(), required);
        }
        return created;
    }

    /**
     * Affecte automatiquement les inscriptions "pending" aux classes du niveau,
     * en respectant la capacite maximale.
     */
    public int autoAssignActiveYearInscriptionsToClasses() {
        SchoolYear active = schoolYearDAO.getActiveSchoolYear();
        if (active == null) {
            return 0;
        }

        int assigned = 0;
        for (Level level : levelDAO.getAllLevels()) {
            List<Inscription> inscriptions = inscriptionDAO.getInscriptionsByYearAndLevel(active.getId(), level.getId());
            List<Inscription> pending = inscriptions.stream()
                    .filter(i -> i != null && "pending".equalsIgnoreCase(i.getStatus()))
                    .filter(i -> i.getSchoolClass() == null)
                    .toList();
            if (pending.isEmpty()) {
                continue;
            }

            List<SchoolClass> classes = classDAO.getClassesByLevel(level.getId());
            classes = classes.stream()
                    .sorted(Comparator.comparing(SchoolClass::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            if (classes.isEmpty()) {
                continue;
            }

            // Distribuer en round-robin avec respect de la capacite restante.
            List<InscriptionDAO.Assignment> inscriptionAssignments = new ArrayList<>();
            List<StudentDAO.ClassAssignment> studentAssignments = new ArrayList<>();

            int classIndex = 0;
            for (Inscription inscription : pending) {
                boolean placed = false;
                for (int attempts = 0; attempts < classes.size(); attempts++) {
                    SchoolClass target = classes.get(classIndex % classes.size());
                    classIndex++;
                    int capacity = Math.min(target.getCapacity(), Constants.MAX_CLASS_CAPACITY);
                    int current = target.getCurrentStudents();
                    if (current >= capacity) {
                        continue;
                    }

                    // occuper une place en memoire pour les prochains.
                    target.setCurrentStudents(current + 1);

                    inscriptionAssignments.add(new InscriptionDAO.Assignment(inscription.getId(), target.getId()));
                    int studentId = inscription.getStudent() != null ? inscription.getStudent().getId() : 0;
                    if (studentId > 0) {
                        studentAssignments.add(new StudentDAO.ClassAssignment(studentId, target.getId()));
                    }
                    placed = true;
                    assigned++;
                    break;
                }
                if (!placed) {
                    // plus de place dans les classes existantes
                    break;
                }
            }

            if (!inscriptionAssignments.isEmpty()) {
                inscriptionDAO.bulkAssignInscriptionsToClasses(inscriptionAssignments);
            }
            if (!studentAssignments.isEmpty()) {
                studentDAO.bulkAssignStudentsToClasses(studentAssignments);
                classDAO.recomputeCurrentStudentsForLevel(level.getId());
            }
        }

        return assigned;
    }
}
