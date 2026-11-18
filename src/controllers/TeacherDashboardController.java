package controllers;

import dao.SchoolYearDAO;
import dao.SubjectDAO;
import dao.TrimesterDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.Bulletin;
import models.Grade;
import models.Level;
import models.SchoolClass;
import models.SchoolYear;
import models.Student;
import models.Subject;
import models.Teacher;
import models.Trimester;
import models.User;
import services.AuditService;
import services.BulletinService;
import services.GradeService;
import services.StudentService;
import services.TeacherService;
import utils.AlertUtils;
import utils.AnimationUtils;
import utils.NavigationUtils;
import utils.ThemeManager;
import utils.TableViewUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Controleur pour le dashboard enseignant.
 */
public class TeacherDashboardController implements DashboardController {
    private static final Logger logger = Logger.getLogger(TeacherDashboardController.class.getName());

    // ── Topbar ──────────────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private ToggleButton themeToggle;

    // ── Tab pane & tabs ──────────────────────────────────────────────────────
    @FXML private TabPane teacherTabPane;
    @FXML private Tab classesTab;
    @FXML private Tab gradeEntryTab;
    @FXML private Tab consultationTab;

    // ── Onglet "Mes Classes" ─────────────────────────────────────────────────
    @FXML private TableView<SchoolClass> classesTableView;

    // ── Onglet "Saisir des Notes" ────────────────────────────────────────────
    @FXML private Label entryTeacherLabel;
    @FXML private ComboBox<SchoolYear> schoolYearCombo;
    @FXML private ComboBox<Level> levelCombo;
    @FXML private ComboBox<SchoolClass> classCombo;
    @FXML private ComboBox<Subject> subjectCombo;
    @FXML private ComboBox<Trimester> trimesterCombo;
    @FXML private Button loadGradesButton;
    @FXML private Label gradeEntryFeedbackLabel;
    @FXML private TableView<TeacherGradeRow> gradesTableView;
    @FXML private Button saveGradesButton;
    @FXML private Button cancelGradesButton;

    // ── Onglet "Consulter Notes" ─────────────────────────────────────────────
    @FXML private ComboBox<SchoolYear> consultationSchoolYearCombo;
    @FXML private ComboBox<SchoolClass> consultationClassCombo;
    @FXML private ComboBox<Trimester> consultationTrimesterCombo;
    @FXML private TableView<BulletinRow> bulletinsTableView;
    @FXML private VBox bulletinPreviewContainer;

    // ── État interne ─────────────────────────────────────────────────────────
    private User currentUser;
    private Teacher currentTeacher;
    private boolean autoRefreshBound;
    private boolean bulletinsLoadScheduled;
    /**
     * true dès qu'au moins une note de la table a été modifiée depuis le dernier
     * chargement ou enregistrement.
     */
    private boolean gradesModified = false;
    /**
     * Snapshot des valeurs originales (avant modification) pour le bouton Annuler.
     * Clé = index de la ligne dans gradesTableView.
     */
    private List<String> originalGradeValues = new ArrayList<>();

    // ── Services / DAO ───────────────────────────────────────────────────────
    private final TeacherService teacherService = new TeacherService();
    private final StudentService studentService = new StudentService();
    private final GradeService gradeService = new GradeService();
    private final BulletinService bulletinService = new BulletinService();
    private final SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
    private final TrimesterDAO trimesterDAO = new TrimesterDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();

    // ════════════════════════════════════════════════════════════════════════
    // Initialisation
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        logger.info("Initialisation du dashboard enseignant");
        setupClassesTable();
        setupGradesTable();
        setupBulletinsTable();
        setupPlaceholders();
        setupComboBoxes();
        setupListeners();
        setupButtonStates();
        setupDynamicEffects();
        setupAutoRefresh();
        setupThemeToggle();
    }

    private void setupDynamicEffects() {
        welcomeLabel.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) return;
            AnimationUtils.playSceneEntrance(newScene.getRoot());
        });
    }

    private void setupAutoRefresh() {
        welcomeLabel.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            newScene.windowProperty().addListener((windowObservable, oldWindow, newWindow) -> {
                if (newWindow == null || autoRefreshBound) {
                    return;
                }
                autoRefreshBound = true;
                newWindow.focusedProperty().addListener((focusObservable, wasFocused, isFocused) -> {
                    if (isFocused && currentUser != null) {
                        Platform.runLater(this::loadDashboardData);
                    }
                });
            });
        });
    }

    private void setupThemeToggle() {
        if (themeToggle == null) return;
        themeToggle.selectedProperty().addListener((obs, wasSelected, selected) -> {
            if (themeToggle.getScene() != null) {
                ThemeManager.setDark(themeToggle.getScene(), selected);
            }
        });
        themeToggle.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            themeToggle.setSelected(ThemeManager.isDarkPreferred());
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // DashboardController impl
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void setUser(User user) {
        currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getFirstName() + " " + user.getLastName());
        loadDashboardData();
    }

    @Override
    public void loadDashboardData() {
        try {
            logger.info("Chargement des donnees du dashboard enseignant");
            currentTeacher = currentUser != null ? teacherService.getTeacherByUserId(currentUser.getId()) : null;
            updateTeacherLabel();
            loadTeacherClasses();
            loadGradeEntryReferenceCombos();
            loadConsultationReferenceCombos();
            resetGradeEntryTable(true);
            scheduleBulletinsLoad();
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du chargement des donnees", e);
            AlertUtils.showError("Erreur", "Impossible de charger le dashboard enseignant.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handlers sidebar (navigation par onglets uniquement désormais)
    // Ces méthodes sont conservées pour la compatibilité mais ne sont plus
    // liées à des boutons sidebar supprimés.
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleShowClasses() {
        selectTab(classesTab);
        loadTeacherClasses();
    }

    @FXML
    private void handleGradeEntry() {
        selectTab(gradeEntryTab);
        syncGradeSelectionFromConsultation();
        resetGradeEntryTable(true);
    }

    @FXML
    private void handleViewGrades() {
        selectTab(consultationTab);
        syncConsultationSelectionFromEntry();
        scheduleBulletinsLoad();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handlers "Saisir des Notes"
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleLoadGrades() {
        // Validation des champs obligatoires
        if (!isGradeEntrySelectionValid()) {
            showEntryFeedback("Veuillez completer tous les champs.", false);
            return;
        }

        if (currentTeacher == null) {
            resetGradeEntryTable(true);
            return;
        }

        SchoolYear selectedSchoolYear = schoolYearCombo.getSelectionModel().getSelectedItem();
        SchoolClass selectedClass = classCombo.getSelectionModel().getSelectedItem();
        Subject selectedSubject = subjectCombo.getSelectionModel().getSelectedItem();
        Trimester selectedTrimester = trimesterCombo.getSelectionModel().getSelectedItem();

        // État loading
        setLoadingState(true);
        showEntryFeedback("Chargement...", true);

        // Chargement synchrone (JavaFX desktop — pas de thread BG requis ici)
        try {
            List<Student> students = studentService.getStudentsByClassForGradeEntry(selectedClass.getId());
            List<Grade> grades = gradeService.getGradesByClassAndSubject(
                    selectedClass.getId(),
                    selectedSubject.getId(),
                    selectedSchoolYear.getId(),
                    selectedTrimester.getId()
            );

            Map<Integer, Grade> gradesByStudentId = new LinkedHashMap<>();
            for (Grade grade : grades) {
                if (grade.getStudent() != null) {
                    gradesByStudentId.put(grade.getStudent().getId(), grade);
                }
            }

            List<TeacherGradeRow> rows = new ArrayList<>();
            for (Student student : students) {
                Grade grade = gradesByStudentId.get(student.getId());
                TeacherGradeRow row = new TeacherGradeRow(student);
                row.setTeacherName(currentTeacherName());
                row.setGradeValue("");
                row.setRemarks("La remarque sera generee automatiquement.");
                if (grade != null) {
                    row.setGradeId(grade.getId());
                }
                rows.add(row);
            }

            gradesTableView.getItems().setAll(rows);

            // Snapshot des valeurs originales pour Annuler
            originalGradeValues = new ArrayList<>();
            for (TeacherGradeRow r : rows) {
                originalGradeValues.add(r.getGradeValue());
            }

            gradesModified = false;
            setGradeTableVisible(true);
            updateSaveAndCancelButtons();

            if (students.isEmpty()) {
                showEntryFeedback("Aucun eleve trouve.", false);
            } else {
                showEntryFeedback(students.size() + " eleve(s) charge(s).", true);
            }

            logger.info("Chargement notes: classe=" + selectedClass.getName()
                    + ", matiere=" + selectedSubject.getName()
                    + ", trimestre=" + selectedTrimester.getName()
                    + ", eleves=" + students.size()
                    + ", notes=" + grades.size());
            Platform.runLater(gradesTableView::refresh);
        } finally {
            setLoadingState(false);
        }
    }

    @FXML
    private void handleSaveGrades() {
        if (currentTeacher == null) {
            AlertUtils.showError("Erreur", "Aucun enseignant connecte.");
            return;
        }

        if (!isGradeEntrySelectionValid()) {
            AlertUtils.showWarning("Attention", "Selectionnez une annee, une classe, une matiere et un trimestre.");
            return;
        }

        SchoolYear selectedSchoolYear = schoolYearCombo.getSelectionModel().getSelectedItem();
        SchoolClass selectedClass = classCombo.getSelectionModel().getSelectedItem();
        Subject selectedSubject = subjectCombo.getSelectionModel().getSelectedItem();
        Trimester selectedTrimester = trimesterCombo.getSelectionModel().getSelectedItem();

        int savedCount = 0;
        for (TeacherGradeRow row : gradesTableView.getItems()) {
            if (row.getGradeValue() == null || row.getGradeValue().isBlank()) {
                continue;
            }

            Double value = parseGradeValue(row.getGradeValue());
            if (value == null) {
                AlertUtils.showWarning("Note invalide",
                        "La note de " + row.getStudentName() + " doit etre comprise entre 0 et 20.");
                return;
            }

            Grade grade = new Grade();
            grade.setStudent(row.getStudent());
            grade.setSubject(selectedSubject);
            grade.setTeacher(currentTeacher);
            grade.setSchoolYear(selectedSchoolYear);
            grade.setTrimester(selectedTrimester);
            grade.setValue(value);
            grade.setRemarks(buildAutomaticRemark(value));

            if (row.getGradeId() > 0) {
                grade.setId(row.getGradeId());
                if (!gradeService.updateGrade(grade)) {
                    AlertUtils.showError("Erreur",
                            "Impossible de mettre a jour la note de " + row.getStudentName() + ".");
                    return;
                }
            } else {
                int newId = gradeService.saveGrade(grade);
                if (newId <= 0) {
                    AlertUtils.showError("Erreur",
                            "Impossible d'enregistrer la note de " + row.getStudentName() + ".");
                    return;
                }
                row.setGradeId(newId);
            }

            row.setTeacherName(currentTeacherName());
            row.setGradeValue(formatGradeValue(value));
            row.setRemarks(grade.getRemarks());
            savedCount++;
        }

        int bulletinCount = bulletinService.synchronizeClassBulletins(
                selectedClass.getId(),
                selectedSchoolYear.getId(),
                selectedTrimester.getId()
        );
        syncConsultationSelectionFromEntry();
        scheduleBulletinsLoad();
        Platform.runLater(gradesTableView::refresh);

        if (savedCount == 0) {
            AlertUtils.showInfo("Information", "Aucune note a enregistrer.");
            return;
        }

        // Reset état modifié après succès
        gradesModified = false;
        // Mettre à jour le snapshot
        originalGradeValues = new ArrayList<>();
        for (TeacherGradeRow r : gradesTableView.getItems()) {
            originalGradeValues.add(r.getGradeValue());
        }
        updateSaveAndCancelButtons();

        showEntryFeedback("Notes enregistrees avec succes.", true);
        AlertUtils.showSuccess(
                "Succes",
                savedCount + " note(s) enregistree(s). " + bulletinCount + " bulletin(s) synchronise(s)."
        );
    }

    @FXML
    private void handleCancelGrades() {
        if (gradesModified) {
            boolean confirmed = AlertUtils.showConfirmation(
                    "Modifications non enregistrees",
                    "Des modifications n'ont pas ete enregistrees. Voulez-vous annuler ?"
            );
            if (!confirmed) {
                return;
            }
            // Restaurer les valeurs originales
            restoreOriginalGradeValues();
        }
        // Si aucune modification : vider la table / revenir à l'état vide
        gradesModified = false;
        originalGradeValues = new ArrayList<>();
        resetGradeEntryTable(true);
        showEntryFeedback("", true);
        updateSaveAndCancelButtons();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Handler "Consulter Notes"
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleLoadBulletins() {
        ensureComboSelection(consultationSchoolYearCombo);
        ensureComboSelection(consultationClassCombo);
        ensureComboSelection(consultationTrimesterCombo);

        SchoolYear schoolYear = consultationSchoolYearCombo.getSelectionModel().getSelectedItem();
        SchoolClass schoolClass = consultationClassCombo.getSelectionModel().getSelectedItem();
        Trimester trimester = consultationTrimesterCombo.getSelectionModel().getSelectedItem();

        if (schoolYear == null || schoolClass == null || trimester == null) {
            bulletinsTableView.getItems().clear();
            renderBulletinPreview(null);
            logger.info("Consultation bulletins ignoree: selection incomplete");
            return;
        }

        List<Bulletin> bulletins = bulletinService.buildClassBulletins(
                schoolClass.getId(), schoolYear.getId(), trimester.getId());
        List<BulletinRow> rows = bulletins.stream()
                .map(BulletinRow::new)
                .toList();

        bulletinsTableView.getItems().setAll(rows);
        if (!rows.isEmpty()) {
            bulletinsTableView.getSelectionModel().selectFirst();
            renderBulletinPreview(rows.get(0));
        } else {
            renderBulletinPreview(null);
        }

        logger.info("Consultation bulletins: classe=" + schoolClass.getName()
                + ", trimestre=" + trimester.getName()
                + ", eleves=" + rows.size());
        Platform.runLater(bulletinsTableView::refresh);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Logout
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void handleLogout() {
        if (!AlertUtils.showConfirmation("Deconnexion", "Etes-vous sur ?")) {
            return;
        }
        try {
            if (currentUser != null) {
                AuditService.logLogout(currentUser.getId(), currentUser.getUsername());
            }
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            NavigationUtils.showLoginScreen(stage, getClass());
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du retour a la connexion", e);
            AlertUtils.showError("Erreur", "Impossible de recharger l'ecran de connexion.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // État des boutons
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Configure les bindings initiaux des boutons et les listeners de combo
     * pour mettre à jour l'état disabled du bouton Charger.
     */
    private void setupButtonStates() {
        // Charger : disabled si les champs obligatoires ne sont pas tous remplis
        updateLoadButton();

        // Enregistrer / Annuler : initialement disabled (rien de chargé)
        updateSaveAndCancelButtons();
    }

    /**
     * Retourne true si tous les champs obligatoires pour charger des notes sont
     * sélectionnés.
     */
    private boolean isGradeEntrySelectionValid() {
        return schoolYearCombo.getSelectionModel().getSelectedItem() != null
                && classCombo.getSelectionModel().getSelectedItem() != null
                && subjectCombo.getSelectionModel().getSelectedItem() != null
                && trimesterCombo.getSelectionModel().getSelectedItem() != null;
    }

    private void updateLoadButton() {
        if (loadGradesButton == null) return;
        loadGradesButton.setDisable(!isGradeEntrySelectionValid());
    }

    private void updateSaveAndCancelButtons() {
        if (saveGradesButton == null || cancelGradesButton == null) return;
        boolean hasData = !gradesTableView.getItems().isEmpty();
        saveGradesButton.setDisable(!hasData || !gradesModified);
        // Annuler est accessible dès qu'il y a des données chargées
        cancelGradesButton.setDisable(!hasData && !gradesModified);
    }

    /** Active/désactive l'état de chargement du bouton Charger. */
    private void setLoadingState(boolean loading) {
        if (loadGradesButton == null) return;
        if (loading) {
            loadGradesButton.setText("Chargement...");
            loadGradesButton.setDisable(true);
        } else {
            loadGradesButton.setText("Charger");
            loadGradesButton.setDisable(!isGradeEntrySelectionValid());
        }
    }

    /** Affiche un message de feedback dans l'onglet saisie. */
    private void showEntryFeedback(String message, boolean ok) {
        if (gradeEntryFeedbackLabel == null) return;
        gradeEntryFeedbackLabel.setText(message);
        gradeEntryFeedbackLabel.setStyle(ok
                ? "-fx-text-fill: -app-success; -fx-padding: 0 12 0 12;"
                : "-fx-text-fill: -app-danger; -fx-padding: 0 12 0 12;");
    }

    /** Restaure les valeurs de notes au snapshot original (avant modification). */
    private void restoreOriginalGradeValues() {
        List<TeacherGradeRow> rows = gradesTableView.getItems();
        for (int i = 0; i < rows.size() && i < originalGradeValues.size(); i++) {
            rows.get(i).setGradeValue(originalGradeValues.get(i));
            String val = originalGradeValues.get(i);
            if (val == null || val.isBlank()) {
                rows.get(i).setRemarks("La remarque sera generee automatiquement.");
            } else {
                Double parsed = parseGradeValue(val);
                rows.get(i).setRemarks(parsed != null ? buildAutomaticRemark(parsed) : "La remarque sera generee automatiquement.");
            }
        }
        Platform.runLater(gradesTableView::refresh);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Listeners de combo + table
    // ════════════════════════════════════════════════════════════════════════

    private void setupListeners() {
        levelCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            refreshClassOptions();
            refreshSubjectOptions();
            resetGradeEntryTable(true);
            updateLoadButton();
            updateSaveAndCancelButtons();
        });
        schoolYearCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            resetGradeEntryTable(true);
            updateLoadButton();
            updateSaveAndCancelButtons();
        });
        classCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            refreshSubjectOptions();
            resetGradeEntryTable(true);
            updateLoadButton();
            updateSaveAndCancelButtons();
        });
        subjectCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            resetGradeEntryTable(true);
            updateLoadButton();
            updateSaveAndCancelButtons();
        });
        trimesterCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            resetGradeEntryTable(true);
            updateLoadButton();
            updateSaveAndCancelButtons();
        });

        consultationSchoolYearCombo.valueProperty().addListener((observable, oldValue, newValue) -> scheduleBulletinsLoad());
        consultationClassCombo.valueProperty().addListener((observable, oldValue, newValue) -> scheduleBulletinsLoad());
        consultationTrimesterCombo.valueProperty().addListener((observable, oldValue, newValue) -> scheduleBulletinsLoad());

        bulletinsTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> renderBulletinPreview(newValue));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Setup tables
    // ════════════════════════════════════════════════════════════════════════

    private void setupClassesTable() {
        if (classesTableView.getColumns().size() < 6) return;
        classesTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableViewUtils.bindAutoFitHeight(classesTableView, 12);

        TableColumn<SchoolClass, String> classColumn = castClassColumn(0);
        classColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<SchoolClass, String> levelColumn = castClassColumn(1);
        levelColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLevel() != null
                        ? cellData.getValue().getLevel().getName() : "N/A"));

        TableColumn<SchoolClass, String> studentsColumn = castClassColumn(2);
        studentsColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getCurrentStudents())));

        TableColumn<SchoolClass, String> capacityColumn = castClassColumn(3);
        capacityColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getCapacity())));

        TableColumn<SchoolClass, String> rateColumn = castClassColumn(4);
        rateColumn.setCellValueFactory(cellData -> {
            SchoolClass sc = cellData.getValue();
            double rate = sc.getCapacity() == 0 ? 0.0
                    : (sc.getCurrentStudents() * 100.0) / sc.getCapacity();
            return new SimpleStringProperty(String.format(Locale.US, "%.0f%%", rate));
        });

        TableColumn<SchoolClass, String> subjectsColumn = castClassColumn(5);
        subjectsColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(valueOrDefault(cellData.getValue().getTaughtSubjects(), "Aucune matiere")));
    }

    private void setupGradesTable() {
        gradesTableView.setEditable(true);
        if (gradesTableView.getColumns().size() < 4) return;

        gradesTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableViewUtils.bindAutoFitHeight(gradesTableView, 12);

        TableColumn<TeacherGradeRow, String> studentColumn = castGradeColumn(0);
        studentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStudentName()));

        TableColumn<TeacherGradeRow, String> gradeColumn = castGradeColumn(1);
        gradeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGradeValue()));
        gradeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        gradeColumn.setOnEditCommit(event -> {
            TeacherGradeRow row = event.getRowValue();
            String newValue = event.getNewValue();

            if (newValue == null || newValue.isBlank()) {
                row.setGradeValue("");
                row.setRemarks("La remarque sera generee automatiquement.");
                row.setTeacherName(currentTeacherName());
                gradesModified = true;
                updateSaveAndCancelButtons();
                Platform.runLater(gradesTableView::refresh);
                return;
            }

            Double parsedValue = parseGradeValue(newValue);
            if (parsedValue == null) {
                AlertUtils.showWarning("Note invalide", "La note doit etre comprise entre 0 et 20.");
                Platform.runLater(gradesTableView::refresh);
                return;
            }

            row.setGradeValue(formatGradeValue(parsedValue));
            row.setTeacherName(currentTeacherName());
            row.setRemarks(buildAutomaticRemark(parsedValue));
            gradesModified = true;
            updateSaveAndCancelButtons();
            Platform.runLater(gradesTableView::refresh);
        });

        TableColumn<TeacherGradeRow, String> teacherColumn = castGradeColumn(2);
        teacherColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTeacherName()));

        TableColumn<TeacherGradeRow, String> remarksColumn = castGradeColumn(3);
        remarksColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRemarks()));
    }

    private void setupBulletinsTable() {
        if (bulletinsTableView.getColumns().size() < 4) return;
        bulletinsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableViewUtils.bindAutoFitHeight(bulletinsTableView, 10);

        TableColumn<BulletinRow, String> studentColumn = castBulletinColumn(0);
        studentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStudentName()));

        TableColumn<BulletinRow, String> averageColumn = castBulletinColumn(1);
        averageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAverageValue()));

        TableColumn<BulletinRow, String> rankColumn = castBulletinColumn(2);
        rankColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRankValue()));

        TableColumn<BulletinRow, String> appreciationColumn = castBulletinColumn(3);
        appreciationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAppreciation()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Cast helpers
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private TableColumn<SchoolClass, String> castClassColumn(int index) {
        return (TableColumn<SchoolClass, String>) classesTableView.getColumns().get(index);
    }

    @SuppressWarnings("unchecked")
    private TableColumn<TeacherGradeRow, String> castGradeColumn(int index) {
        return (TableColumn<TeacherGradeRow, String>) gradesTableView.getColumns().get(index);
    }

    @SuppressWarnings("unchecked")
    private TableColumn<BulletinRow, String> castBulletinColumn(int index) {
        return (TableColumn<BulletinRow, String>) bulletinsTableView.getColumns().get(index);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Chargement des données
    // ════════════════════════════════════════════════════════════════════════

    private void loadTeacherClasses() {
        if (currentTeacher == null) {
            classesTableView.getItems().clear();
            return;
        }
        List<SchoolClass> teacherClasses = teacherService.getTeacherClasses(currentTeacher.getId());
        for (SchoolClass schoolClass : teacherClasses) {
            List<Subject> classSubjects = subjectDAO.getSubjectsByTeacherAndClass(
                    currentTeacher.getId(), schoolClass.getId());
            if (classSubjects.isEmpty()) {
                schoolClass.setTaughtSubjects("Aucune matiere saisie");
                continue;
            }
            String summary = classSubjects.stream()
                    .map(Subject::getName)
                    .sorted(String::compareToIgnoreCase)
                    .toList()
                    .stream()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Aucune matiere");
            schoolClass.setTaughtSubjects(summary);
        }
        classesTableView.getItems().setAll(teacherClasses);
        logger.info("Classes chargees pour l'enseignant: " + teacherClasses.size());
        Platform.runLater(classesTableView::refresh);
    }

    private void loadGradeEntryReferenceCombos() {
        List<SchoolClass> teacherClasses = List.copyOf(classesTableView.getItems());
        SchoolYear previousSchoolYear = schoolYearCombo.getSelectionModel().getSelectedItem();
        Trimester previousTrimester = trimesterCombo.getSelectionModel().getSelectedItem();
        Level previousLevel = levelCombo.getSelectionModel().getSelectedItem();

        schoolYearCombo.getItems().setAll(schoolYearDAO.getAllSchoolYears());
        restoreSchoolYearSelection(schoolYearCombo, previousSchoolYear);

        trimesterCombo.getItems().setAll(trimesterDAO.getAllTrimesters());
        restoreTrimesterSelection(trimesterCombo, previousTrimester);

        Map<Integer, Level> levelsById = new LinkedHashMap<>();
        for (SchoolClass schoolClass : teacherClasses) {
            if (schoolClass.getLevel() != null) {
                levelsById.putIfAbsent(schoolClass.getLevel().getId(), schoolClass.getLevel());
            }
        }
        List<Level> levels = levelsById.values().stream()
                .sorted(Comparator.comparingInt(Level::getNumber))
                .toList();
        levelCombo.getItems().setAll(levels);
        restoreLevelSelection(previousLevel);

        refreshClassOptions();
        refreshSubjectOptions();
        updateLoadButton();
        Platform.runLater(() -> {
            classesTableView.refresh();
            gradesTableView.refresh();
        });
    }

    private void loadConsultationReferenceCombos() {
        List<SchoolClass> teacherClasses = List.copyOf(classesTableView.getItems());
        SchoolYear previousSchoolYear = consultationSchoolYearCombo.getSelectionModel().getSelectedItem();
        SchoolClass previousClass = consultationClassCombo.getSelectionModel().getSelectedItem();
        Trimester previousTrimester = consultationTrimesterCombo.getSelectionModel().getSelectedItem();

        consultationSchoolYearCombo.getItems().setAll(schoolYearDAO.getAllSchoolYears());
        restoreSchoolYearSelection(consultationSchoolYearCombo, previousSchoolYear);

        consultationClassCombo.getItems().setAll(teacherClasses);
        restoreClassSelection(consultationClassCombo, previousClass);

        consultationTrimesterCombo.getItems().setAll(trimesterDAO.getAllTrimesters());
        restoreTrimesterSelection(consultationTrimesterCombo, previousTrimester);

        Platform.runLater(bulletinsTableView::refresh);
    }

    private void refreshClassOptions() {
        Level selectedLevel = levelCombo.getSelectionModel().getSelectedItem();
        SchoolClass previousClass = classCombo.getSelectionModel().getSelectedItem();
        List<SchoolClass> filteredClasses = classesTableView.getItems().stream()
                .filter(sc -> selectedLevel == null
                        || (sc.getLevel() != null && sc.getLevel().getId() == selectedLevel.getId()))
                .toList();
        classCombo.getItems().setAll(filteredClasses);
        restoreClassSelection(classCombo, previousClass);
    }

    private void refreshSubjectOptions() {
        Level selectedLevel = levelCombo.getSelectionModel().getSelectedItem();
        SchoolClass selectedClass = classCombo.getSelectionModel().getSelectedItem();
        Subject previousSubject = subjectCombo.getSelectionModel().getSelectedItem();
        if (selectedLevel == null || currentTeacher == null) {
            subjectCombo.getItems().clear();
            return;
        }
        List<Subject> subjects = selectedClass != null
                ? subjectDAO.getSubjectsByTeacherAndClass(currentTeacher.getId(), selectedClass.getId())
                : subjectDAO.getSubjectsByTeacherAndLevel(currentTeacher.getId(), selectedLevel.getNumber());
        subjectCombo.getItems().setAll(subjects);
        restoreSubjectSelection(previousSubject);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Placeholders & visibilité de la table
    // ════════════════════════════════════════════════════════════════════════

    private void setupPlaceholders() {
        classesTableView.setPlaceholder(new Label("Aucune classe assignee a cet enseignant."));
        gradesTableView.setPlaceholder(new Label("Choisissez les filtres puis cliquez sur Charger."));
        bulletinsTableView.setPlaceholder(new Label("Aucun bulletin a afficher pour la selection courante."));
        setGradeTableVisible(false);
        renderBulletinPreview(null);
    }

    private void setGradeTableVisible(boolean visible) {
        gradesTableView.setManaged(visible);
        gradesTableView.setVisible(visible);
    }

    private void resetGradeEntryTable(boolean hideTable) {
        gradesTableView.getItems().clear();
        setGradeTableVisible(!hideTable);
        gradesModified = false;
        originalGradeValues = new ArrayList<>();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Combo boxes setup & restore
    // ════════════════════════════════════════════════════════════════════════

    private void setupComboBoxes() {
        configureComboBox(schoolYearCombo, SchoolYear::getLabel);
        configureComboBox(levelCombo, Level::getName);
        configureComboBox(classCombo, SchoolClass::getName);
        configureComboBox(subjectCombo, Subject::getName);
        configureComboBox(trimesterCombo, Trimester::getName);
        configureComboBox(consultationSchoolYearCombo, SchoolYear::getLabel);
        configureComboBox(consultationClassCombo, SchoolClass::getName);
        configureComboBox(consultationTrimesterCombo, Trimester::getName);
    }

    private <T> void configureComboBox(ComboBox<T> comboBox, Function<T, String> formatter) {
        comboBox.setConverter(new StringConverter<>() {
            @Override public String toString(T item) {
                return item == null ? "" : safeText(formatter.apply(item));
            }
            @Override public T fromString(String string) { return null; }
        });
        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : safeText(formatter.apply(item)));
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : safeText(formatter.apply(item)));
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // Bulletin preview
    // ════════════════════════════════════════════════════════════════════════

    private void renderBulletinPreview(BulletinRow row) {
        if (bulletinPreviewContainer == null) return;

        if (row == null) {
            bulletinPreviewContainer.getChildren().setAll(
                    new Label("Selectionnez un eleve pour afficher son bulletin."));
            return;
        }

        Bulletin bulletin = row.getBulletin();
        SchoolYear schoolYear = consultationSchoolYearCombo.getSelectionModel().getSelectedItem();
        Trimester trimester = consultationTrimesterCombo.getSelectionModel().getSelectedItem();

        List<Grade> grades = bulletin != null ? bulletin.getGrades() : null;
        if ((grades == null || grades.isEmpty())
                && bulletin != null
                && bulletin.getStudent() != null
                && schoolYear != null
                && trimester != null) {
            grades = gradeService.getGradesByStudentAndPeriod(
                    bulletin.getStudent().getId(), schoolYear.getId(), trimester.getId());
            bulletin.setGrades(grades);
        }

        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-card");

        Label title = new Label("Bulletin de " + row.getStudentName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TableView<BulletinPreviewLine> previewTable = new TableView<>();
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<BulletinPreviewLine, String> subjectColumn = new TableColumn<>("Matiere");
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        TableColumn<BulletinPreviewLine, String> coefficientColumn = new TableColumn<>("Coef");
        coefficientColumn.setCellValueFactory(new PropertyValueFactory<>("coefficient"));
        TableColumn<BulletinPreviewLine, String> gradeColumn = new TableColumn<>("Note");
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        TableColumn<BulletinPreviewLine, String> remarkColumn = new TableColumn<>("Remarque");
        remarkColumn.setCellValueFactory(new PropertyValueFactory<>("remark"));
        TableColumn<BulletinPreviewLine, String> averageColumn = new TableColumn<>("Moyenne");
        averageColumn.setCellValueFactory(new PropertyValueFactory<>("average"));
        TableColumn<BulletinPreviewLine, String> rankColumn = new TableColumn<>("Rang");
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        TableColumn<BulletinPreviewLine, String> appreciationColumn = new TableColumn<>("Appreciation");
        appreciationColumn.setCellValueFactory(new PropertyValueFactory<>("appreciation"));

        previewTable.getColumns().setAll(
                subjectColumn, coefficientColumn, gradeColumn, remarkColumn,
                averageColumn, rankColumn, appreciationColumn);
        TableViewUtils.bindAutoFitHeight(previewTable, 14);

        if (grades == null || grades.isEmpty()) {
            previewTable.setPlaceholder(new Label("Aucune note disponible pour ce bulletin."));
        } else {
            // ── Gestion 0.00 dans l'aperçu ───────────────────────────────────
            boolean hasRealGrades = bulletin != null && bulletin.getAverage() > 0.0;
            String averageValue = hasRealGrades
                    ? String.format(Locale.US, "%.2f", bulletin.getAverage())
                    : "\u2014";
            String rankValue = bulletin != null && bulletin.getRank() > 0
                    ? String.valueOf(bulletin.getRank()) : "N/A";
            String appreciationValue = bulletin != null
                    ? valueOrDefault(bulletin.getAppreciation(), "Aucune appreciation")
                    : "Aucune appreciation";

            List<BulletinPreviewLine> lines = grades.stream()
                    .sorted(Comparator.comparing(
                            grade -> grade.getSubject() != null ? grade.getSubject().getName() : ""))
                    .map(grade -> new BulletinPreviewLine(
                            grade.getSubject() != null ? grade.getSubject().getName() : "N/A",
                            String.valueOf(grade.getSubject() != null
                                    ? Math.max(1, grade.getSubject().getCoefficient()) : 1),
                            formatGradeValue(grade.getValue()),
                            valueOrDefault(grade.getRemarks(), buildAutomaticRemark(grade.getValue())),
                            averageValue,
                            rankValue,
                            appreciationValue
                    ))
                    .toList();
            previewTable.getItems().setAll(lines);
        }

        card.getChildren().setAll(title, previewTable);
        bulletinPreviewContainer.getChildren().setAll(card);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers divers
    // ════════════════════════════════════════════════════════════════════════

    private void updateTeacherLabel() {
        if (entryTeacherLabel != null) {
            entryTeacherLabel.setText(currentTeacherName());
        }
    }

    private String currentTeacherName() {
        return currentTeacher != null ? safeText(currentTeacher.getFullName()) : "N/A";
    }

    private String buildAutomaticRemark(double value) {
        if (value >= 17) return "Excellent";
        if (value >= 14) return "Tres bien";
        if (value >= 12) return "Bien";
        if (value >= 10) return "Satisfaisant";
        if (value >= 7)  return "A ameliorer";
        return "Insuffisant";
    }

    private Double parseGradeValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return null;
        try {
            double value = Double.parseDouble(rawValue.trim().replace(',', '.'));
            if (value < 0 || value > 20) return null;
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatGradeValue(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void selectTab(Tab tab) {
        if (teacherTabPane != null && tab != null) {
            teacherTabPane.getSelectionModel().select(tab);
        }
    }

    private void ensureComboSelection(ComboBox<?> comboBox) {
        if (comboBox != null && comboBox.getSelectionModel().isEmpty()
                && !comboBox.getItems().isEmpty()) {
            comboBox.getSelectionModel().selectFirst();
        }
    }

    // ── Restore selections ───────────────────────────────────────────────────

    private void restoreSchoolYearSelection(ComboBox<SchoolYear> comboBox, SchoolYear previous) {
        if (previous != null) {
            for (SchoolYear sy : comboBox.getItems()) {
                if (sy.getId() == previous.getId()) {
                    comboBox.getSelectionModel().select(sy);
                    return;
                }
            }
        }
        SchoolYear activeSchoolYear = schoolYearDAO.getActiveSchoolYear();
        if (activeSchoolYear != null) {
            for (SchoolYear sy : comboBox.getItems()) {
                if (sy.getId() == activeSchoolYear.getId()) {
                    comboBox.getSelectionModel().select(sy);
                    return;
                }
            }
        }
        ensureComboSelection(comboBox);
    }

    private void restoreTrimesterSelection(ComboBox<Trimester> comboBox, Trimester previous) {
        if (previous != null) {
            for (Trimester t : comboBox.getItems()) {
                if (t.getId() == previous.getId()) {
                    comboBox.getSelectionModel().select(t);
                    return;
                }
            }
        }
        ensureComboSelection(comboBox);
    }

    private void restoreLevelSelection(Level previousLevel) {
        if (previousLevel != null) {
            for (Level l : levelCombo.getItems()) {
                if (l.getId() == previousLevel.getId()) {
                    levelCombo.getSelectionModel().select(l);
                    return;
                }
            }
        }
        ensureComboSelection(levelCombo);
    }

    private void restoreClassSelection(ComboBox<SchoolClass> comboBox, SchoolClass previousClass) {
        if (previousClass != null) {
            for (SchoolClass sc : comboBox.getItems()) {
                if (sc.getId() == previousClass.getId()) {
                    comboBox.getSelectionModel().select(sc);
                    return;
                }
            }
        }
        ensureComboSelection(comboBox);
    }

    private void restoreSubjectSelection(Subject previousSubject) {
        if (previousSubject != null) {
            for (Subject s : subjectCombo.getItems()) {
                if (s.getId() == previousSubject.getId()) {
                    subjectCombo.getSelectionModel().select(s);
                    return;
                }
            }
        }
        ensureComboSelection(subjectCombo);
    }

    // ── Synchronisation entre onglets ────────────────────────────────────────

    private void syncConsultationSelectionFromEntry() {
        SchoolYear selectedSchoolYear = schoolYearCombo.getSelectionModel().getSelectedItem();
        SchoolClass selectedClass = classCombo.getSelectionModel().getSelectedItem();
        Trimester selectedTrimester = trimesterCombo.getSelectionModel().getSelectedItem();
        if (selectedSchoolYear != null) restoreSchoolYearSelection(consultationSchoolYearCombo, selectedSchoolYear);
        if (selectedClass != null) restoreClassSelection(consultationClassCombo, selectedClass);
        if (selectedTrimester != null) restoreTrimesterSelection(consultationTrimesterCombo, selectedTrimester);
    }

    private void syncGradeSelectionFromConsultation() {
        SchoolYear selectedSchoolYear = consultationSchoolYearCombo.getSelectionModel().getSelectedItem();
        SchoolClass selectedClass = consultationClassCombo.getSelectionModel().getSelectedItem();
        Trimester selectedTrimester = consultationTrimesterCombo.getSelectionModel().getSelectedItem();
        if (selectedSchoolYear != null) restoreSchoolYearSelection(schoolYearCombo, selectedSchoolYear);
        if (selectedTrimester != null) restoreTrimesterSelection(trimesterCombo, selectedTrimester);
        if (selectedClass != null && selectedClass.getLevel() != null) {
            restoreLevelSelection(selectedClass.getLevel());
            refreshClassOptions();
            restoreClassSelection(classCombo, selectedClass);
            refreshSubjectOptions();
        }
    }

    private void scheduleBulletinsLoad() {
        if (bulletinsLoadScheduled) return;
        bulletinsLoadScheduled = true;
        Platform.runLater(() -> {
            bulletinsLoadScheduled = false;
            handleLoadBulletins();
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // Inner classes (modèles de lignes de table)
    // ════════════════════════════════════════════════════════════════════════

    private static final class TeacherGradeRow {
        private final Student student;
        private final String studentName;
        private int gradeId;
        private String gradeValue;
        private String teacherName;
        private String remarks;

        private TeacherGradeRow(Student student) {
            this.student = student;
            this.studentName = student != null ? student.getFullName() : "N/A";
        }

        public Student getStudent()            { return student; }
        public String  getStudentName()        { return studentName; }
        public int     getGradeId()            { return gradeId; }
        public void    setGradeId(int id)      { this.gradeId = id; }
        public String  getGradeValue()         { return gradeValue; }
        public void    setGradeValue(String v) { this.gradeValue = v; }
        public String  getTeacherName()        { return teacherName; }
        public void    setTeacherName(String n){ this.teacherName = n; }
        public String  getRemarks()            { return remarks; }
        public void    setRemarks(String r)    { this.remarks = r; }
    }

    private static final class BulletinRow {
        private final Bulletin bulletin;

        private BulletinRow(Bulletin bulletin) {
            this.bulletin = bulletin;
        }

        public Bulletin getBulletin() { return bulletin; }

        public String getStudentName() {
            return bulletin.getStudent() != null ? bulletin.getStudent().getFullName() : "N/A";
        }

        /**
         * Retourne "—" si l'élève n'a aucune note (moyenne == 0 et pas de notes),
         * sinon la moyenne formatée sur 2 décimales.
         */
        public String getAverageValue() {
            if (bulletin == null) return "\u2014";
            List<Grade> grades = bulletin.getGrades();
            boolean hasGrades = grades != null && !grades.isEmpty();
            if (!hasGrades && bulletin.getAverage() == 0.0) {
                return "\u2014";
            }
            return String.format(Locale.US, "%.2f", bulletin.getAverage());
        }

        public String getRankValue() {
            return bulletin.getRank() > 0 ? String.valueOf(bulletin.getRank()) : "N/A";
        }

        public String getAppreciation() {
            if (bulletin == null) return "Aucune note";
            List<Grade> grades = bulletin.getGrades();
            boolean hasGrades = grades != null && !grades.isEmpty();
            if (!hasGrades && bulletin.getAverage() == 0.0) {
                return "Aucune note";
            }
            return bulletin.getAppreciation() != null ? bulletin.getAppreciation() : "Aucune appreciation";
        }
    }

    public static final class BulletinInfoLine {
        private final String field;
        private final String value;

        public BulletinInfoLine(String field, String value) {
            this.field = field;
            this.value = value;
        }

        public String getField() { return field; }
        public String getValue() { return value; }
    }

    public static final class BulletinGradeLine {
        private final String subject;
        private final String coefficient;
        private final String value;
        private final String remark;

        public BulletinGradeLine(String subject, String coefficient, String value, String remark) {
            this.subject = subject;
            this.coefficient = coefficient;
            this.value = value;
            this.remark = remark;
        }

        public String getSubject()     { return subject; }
        public String getCoefficient() { return coefficient; }
        public String getValue()       { return value; }
        public String getRemark()      { return remark; }
    }

    public static final class BulletinPreviewLine {
        private final String subject;
        private final String coefficient;
        private final String value;
        private final String remark;
        private final String average;
        private final String rank;
        private final String appreciation;

        public BulletinPreviewLine(String subject, String coefficient, String value, String remark,
                                   String average, String rank, String appreciation) {
            this.subject = subject;
            this.coefficient = coefficient;
            this.value = value;
            this.remark = remark;
            this.average = average;
            this.rank = rank;
            this.appreciation = appreciation;
        }

        public String getSubject()      { return subject; }
        public String getCoefficient()  { return coefficient; }
        public String getValue()        { return value; }
        public String getRemark()       { return remark; }
        public String getAverage()      { return average; }
        public String getRank()         { return rank; }
        public String getAppreciation() { return appreciation; }
    }
}
