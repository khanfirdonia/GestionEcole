package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import models.SchoolClass;
import models.Student;
import models.Subject;
import models.Teacher;
import models.User;
import models.PreEnrollment;
import services.AuditService;
import services.ClassService;
import services.EnrollmentAdminService;
import services.InscriptionService;
import services.PreEnrollmentService;
import services.PreEnrollmentAdminService;
import services.StudentService;
import services.StudentManagementService;
import services.TeacherAssignmentService;
import services.TeacherManagementService;
import services.TeacherService;
import services.UserService;
import dao.LevelDAO;
import dao.ClassDAO;
import dao.SubjectDAO;
import utils.AlertUtils;
import utils.AnimationUtils;
import utils.Constants;
import utils.DateUtils;
import utils.NavigationUtils;
import utils.ThemeManager;
import utils.TableViewUtils;
import utils.ValidationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controleur pour le dashboard admin.
 */
public class AdminDashboardController implements DashboardController {
    private static final Logger logger = Logger.getLogger(AdminDashboardController.class.getName());

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label studentCountLabel;

    @FXML
    private Label teacherCountLabel;

    @FXML
    private Label classCountLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private BarChart<String, Number> studentsByLevelChart;

    @FXML
    private BarChart<String, Number> classesByLevelChart;

    @FXML
    private ListView<Student> pendingStudentsListView;

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private Node statsSection;

    @FXML
    private Node chartsSection;

    @FXML
    private Node pendingSection;

    @FXML
    private Node studentsSection;

    @FXML
    private Node teachersSection;

    @FXML
    private ToggleButton themeToggle;

    @FXML
    private ComboBox<String> approvalSortCombo;

    @FXML
    private Button approveButton;

    @FXML
    private Button rejectButton;

    @FXML
    private TableView<ApprovalDecisionRow> approvalsTableView;

    @FXML
    private TableColumn<ApprovalDecisionRow, String> approvalNameColumn;

    @FXML
    private TableColumn<ApprovalDecisionRow, String> approvalLevelColumn;

    @FXML
    private TableColumn<ApprovalDecisionRow, ApprovalDecision> approvalStatusColumn;

    private final javafx.collections.ObservableList<ApprovalDecisionRow> approvalDecisions =
            javafx.collections.FXCollections.observableArrayList();

    @FXML
    private TextField pendingSearchField;

    @FXML
    private ComboBox<models.Level> pendingLevelFilterCombo;

    @FXML
    private Button approvePreEnrollmentButton;

    @FXML
    private Button rejectPreEnrollmentButton;

    @FXML
    private TableView<PreEnrollment> preEnrollmentsTableView;

    @FXML
    private TableColumn<PreEnrollment, String> preEnrollmentNameColumn;

    @FXML
    private TableColumn<PreEnrollment, String> preEnrollmentLevelColumn;

    @FXML
    private TableColumn<PreEnrollment, String> preEnrollmentBirthColumn;

    @FXML
    private TableColumn<PreEnrollment, String> preEnrollmentParentPhoneColumn;

    @FXML
    private TextField studentsSearchField;

    @FXML
    private ComboBox<models.Level> studentsLevelFilterCombo;

    @FXML
    private ComboBox<SchoolClass> studentsClassFilterCombo;

    @FXML
    private Button addStudentButton;

    @FXML
    private Button editStudentButton;

    @FXML
    private Button deleteStudentButton;

    @FXML
    private TableView<Student> studentsTableView;

    @FXML
    private TableColumn<Student, String> studentNameColumn;

    @FXML
    private TableColumn<Student, String> studentUsernameColumn;

    @FXML
    private TableColumn<Student, String> studentMatriculeColumn;

    @FXML
    private TableColumn<Student, String> studentLevelColumn;

    @FXML
    private TableColumn<Student, String> studentClassColumn;

    @FXML
    private TableColumn<Student, String> studentStatusColumn;

    @FXML
    private TextField teachersSearchField;

    @FXML
    private Button addTeacherButton;

    @FXML
    private Button editTeacherButton;

    @FXML
    private Button assignTeacherButton;

    @FXML
    private TableView<Teacher> teachersTableView;

    @FXML
    private TableColumn<Teacher, String> teacherNameColumn;

    @FXML
    private TableColumn<Teacher, String> teacherUsernameColumn;

    @FXML
    private TableColumn<Teacher, String> teacherMatriculeColumn;

    @FXML
    private TableColumn<Teacher, String> teacherPhoneColumn;

    @FXML
    private TableColumn<Teacher, String> teacherSpecializationColumn;

    @FXML
    private TableColumn<Teacher, String> teacherAssignmentsColumn;

    private final ObservableList<Student> pendingStudents = FXCollections.observableArrayList();
    private final FilteredList<Student> filteredPendingStudents = new FilteredList<>(pendingStudents, student -> true);

    private final ObservableList<Student> allStudents = FXCollections.observableArrayList();
    private final FilteredList<Student> filteredStudents = new FilteredList<>(allStudents, student -> true);

    private final ObservableList<Teacher> allTeachers = FXCollections.observableArrayList();
    private final FilteredList<Teacher> filteredTeachers = new FilteredList<>(allTeachers, teacher -> true);
    private final Map<Integer, String> teacherAssignmentSummaryById = new HashMap<>();

    private User currentUser;
    private boolean autoRefreshBound;
    private final StudentService studentService = new StudentService();
    private final ClassService classService = new ClassService();
    private final TeacherService teacherService = new TeacherService();
    private final InscriptionService inscriptionService = new InscriptionService();
    private final EnrollmentAdminService enrollmentAdminService = new EnrollmentAdminService();
    private final StudentManagementService studentManagementService = new StudentManagementService();
    private final TeacherManagementService teacherManagementService = new TeacherManagementService();
    private final TeacherAssignmentService teacherAssignmentService = new TeacherAssignmentService();
    private final UserService userService = new UserService();
    private final LevelDAO levelDAO = new LevelDAO();
    private final ClassDAO classDAO = new ClassDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final PreEnrollmentService preEnrollmentService = new PreEnrollmentService();
    private final PreEnrollmentAdminService preEnrollmentAdminService = new PreEnrollmentAdminService();

    @FXML
    public void initialize() {
        logger.info("Initialisation du dashboard admin");
        setupPendingStudentsListView();
        setupPendingStudentFilters();
        setupPendingActionButtonsState();
        setupPreEnrollmentsTable();
        setupPreEnrollmentsButtonsState();
        setupStudentsTable();
        setupStudentsFilters();
        setupStudentsActionButtonsState();
        setupTeachersTable();
        setupTeachersFilters();
        setupTeachersActionButtonsState();
        setupDynamicEffects();
        setupAutoRefresh();
        setupThemeToggle();
        setupApprovalsTable();
    }

    private void setupDynamicEffects() {
        welcomeLabel.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
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

    private void refreshDashboardView() {
        if (currentUser == null) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            loadDashboardData();
        } else {
            Platform.runLater(this::loadDashboardData);
        }
    }

    private void setupThemeToggle() {
        if (themeToggle == null) {
            return;
        }

        themeToggle.selectedProperty().addListener((obs, wasSelected, selected) -> {
            if (themeToggle.getScene() != null) {
                ThemeManager.setDark(themeToggle.getScene(), selected);
            }
        });

        themeToggle.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            themeToggle.setSelected(ThemeManager.isDarkPreferred());
        });
    }

    @FXML
    private void handleNavOverview() {
        scrollTo(statsSection);
    }

    @FXML
    private void handleNavCharts() {
        scrollTo(chartsSection);
    }

    @FXML
    private void handleNavPending() {
        scrollTo(pendingSection);
    }

    @FXML
    private void handleNavStudents() {
        scrollTo(studentsSection);
    }

    @FXML
    private void handleNavTeachers() {
        scrollTo(teachersSection);
    }

    private void scrollTo(Node target) {
        if (contentScrollPane == null || contentScrollPane.getContent() == null || target == null) {
            return;
        }

        // Calculer la position verticale du target dans le contenu du ScrollPane.
        Bounds contentBounds = contentScrollPane.getContent().getLayoutBounds();
        Bounds viewportBounds = contentScrollPane.getViewportBounds();
        if (contentBounds.getHeight() <= viewportBounds.getHeight()) {
            return;
        }

        Bounds contentSceneBounds = contentScrollPane.getContent().localToScene(contentScrollPane.getContent().getBoundsInLocal());
        Bounds targetSceneBounds = target.localToScene(target.getBoundsInLocal());
        double deltaY = targetSceneBounds.getMinY() - contentSceneBounds.getMinY();

        double v = deltaY / (contentBounds.getHeight() - viewportBounds.getHeight());
        if (v < 0) v = 0;
        if (v > 1) v = 1;
        contentScrollPane.setVvalue(v);
    }

    private void setupPendingStudentsListView() {
        pendingStudentsListView.setItems(filteredPendingStudents);
        pendingStudentsListView.setPlaceholder(new Label("Aucun eleve en liste d'attente."));
        pendingStudentsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }

                String levelName = item.getLevel() != null ? item.getLevel().getName() : "N/A";
                String statusLabel = "Liste d'attente";
                setText(item.getFullName() + " (Niveau: " + levelName + ", Statut: " + statusLabel + ")");
            }
        });
    }

    private void setupPendingActionButtonsState() {
        if (pendingStudentsListView == null) {
            return;
        }
        if (approveButton != null) {
            approveButton.disableProperty().bind(pendingStudentsListView.getSelectionModel().selectedItemProperty().isNull());
        }
        if (rejectButton != null) {
            rejectButton.disableProperty().bind(pendingStudentsListView.getSelectionModel().selectedItemProperty().isNull());
        }
    }

    private void setupPendingStudentFilters() {
        if (pendingLevelFilterCombo != null) {
            models.Level all = new models.Level();
            all.setId(0);
            all.setName("Tous niveaux");
            all.setNumber(0);
            pendingLevelFilterCombo.getItems().setAll(all);
            pendingLevelFilterCombo.getItems().addAll(levelDAO.getAllLevels());
            pendingLevelFilterCombo.getSelectionModel().selectFirst();
            pendingLevelFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyPendingFilter());
        }

        if (pendingSearchField != null) {
            pendingSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyPendingFilter());
        }

        applyPendingFilter();
    }

    private void applyPendingFilter() {
        String query = pendingSearchField != null && pendingSearchField.getText() != null
                ? pendingSearchField.getText().trim().toLowerCase()
                : "";
        int levelId = pendingLevelFilterCombo != null && pendingLevelFilterCombo.getValue() != null
                ? pendingLevelFilterCombo.getValue().getId()
                : 0;

        filteredPendingStudents.setPredicate(student -> {
            if (student == null) {
                return false;
            }
            if (levelId > 0) {
                if (student.getLevel() == null || student.getLevel().getId() != levelId) {
                    return false;
                }
            }
            if (!query.isEmpty()) {
                String name = student.getFullName() != null ? student.getFullName().toLowerCase() : "";
                return name.contains(query);
            }
            return true;
        });
    }

    private void setupApprovalsTable() {
        if (approvalsTableView == null) {
            return;
        }

        approvalsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        approvalsTableView.setItems(approvalDecisions);
        TableViewUtils.bindAutoFitHeight(approvalsTableView, 8);

        if (approvalNameColumn != null) {
            approvalNameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().studentName()));
            approvalNameColumn.setSortable(true);
        }

        if (approvalLevelColumn != null) {
            approvalLevelColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().levelName()));
            approvalLevelColumn.setSortable(true);
        }

        if (approvalStatusColumn != null) {
            approvalStatusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().decision()));
            approvalStatusColumn.setSortable(false);
            approvalStatusColumn.setCellFactory(createApprovalStatusCellFactory());
        }

        if (approvalSortCombo != null) {
            approvalSortCombo.getItems().setAll("Nom", "Niveau");
            approvalSortCombo.getSelectionModel().selectFirst();
            approvalSortCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyApprovalSort());
        }
        applyApprovalSort();
    }

    private Callback<TableColumn<ApprovalDecisionRow, ApprovalDecision>, TableCell<ApprovalDecisionRow, ApprovalDecision>>
    createApprovalStatusCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(ApprovalDecision item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                if (item == ApprovalDecision.APPROVED) {
                    setText("Approuve");
                    setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-background-radius: 999px; -fx-padding: 4px 9px; -fx-font-weight: bold;");
                } else {
                    setText("Non approuve");
                    setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-background-radius: 999px; -fx-padding: 4px 9px; -fx-font-weight: bold;");
                }
            }
        };
    }

    private void applyApprovalSort() {
        if (approvalsTableView == null || approvalSortCombo == null) {
            return;
        }

        approvalsTableView.getSortOrder().clear();
        String key = approvalSortCombo.getSelectionModel().getSelectedItem();
        if ("Niveau".equalsIgnoreCase(key)) {
            if (approvalLevelColumn != null) {
                approvalLevelColumn.setSortType(TableColumn.SortType.ASCENDING);
                approvalsTableView.getSortOrder().add(approvalLevelColumn);
            }
            if (approvalNameColumn != null) {
                approvalNameColumn.setSortType(TableColumn.SortType.ASCENDING);
                approvalsTableView.getSortOrder().add(approvalNameColumn);
            }
        } else {
            if (approvalNameColumn != null) {
                approvalNameColumn.setSortType(TableColumn.SortType.ASCENDING);
                approvalsTableView.getSortOrder().add(approvalNameColumn);
            }
        }
        approvalsTableView.sort();
    }

    private void recordDecision(Student student, ApprovalDecision decision) {
        if (student == null) {
            return;
        }

        int studentId = student.getId();
        String name = student.getFullName();
        String levelName = (student.getLevel() != null && student.getLevel().getName() != null)
                ? student.getLevel().getName()
                : "N/A";

        // Remplacer l'entree existante pour ce student si elle existe.
        ApprovalDecisionRow newRow = new ApprovalDecisionRow(studentId, name, levelName, decision);
        for (int i = 0; i < approvalDecisions.size(); i++) {
            if (approvalDecisions.get(i).studentId() == studentId) {
                approvalDecisions.set(i, newRow);
                applyApprovalSort();
                return;
            }
        }

        approvalDecisions.add(newRow);
        applyApprovalSort();
    }

    @Override
    public void setUser(User user) {
        currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getFirstName() + " " + user.getLastName());
        loadDashboardData();
    }

    @Override
    public void loadDashboardData() {
        try {
            logger.info("Chargement des donnees du dashboard admin");
            loadStatistics();
            loadStudentsByLevelChart();
            loadClassesByLevelChart();
            loadPendingPreEnrollments();
            loadPendingStudents();
            loadAllStudents();
            loadAllTeachers();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement des donnees", e);
            AlertUtils.showError("Erreur", "Erreur lors du chargement des donnees");
        }
    }

    private void setupPreEnrollmentsTable() {
        if (preEnrollmentsTableView == null) {
            return;
        }

        preEnrollmentsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableViewUtils.bindAutoFitHeight(preEnrollmentsTableView, 8);

        if (preEnrollmentNameColumn != null) {
            preEnrollmentNameColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue() != null ? d.getValue().getFullName() : ""
            ));
        }
        if (preEnrollmentLevelColumn != null) {
            preEnrollmentLevelColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue() != null && d.getValue().getRequestedLevel() != null ? safe(d.getValue().getRequestedLevel().getName()) : ""
            ));
        }
        if (preEnrollmentBirthColumn != null) {
            preEnrollmentBirthColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue() != null ? safe(d.getValue().getBirthDate()) : ""
            ));
        }
        if (preEnrollmentParentPhoneColumn != null) {
            preEnrollmentParentPhoneColumn.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                    d.getValue() != null ? safe(d.getValue().getParentPhone()) : ""
            ));
        }
    }

    private void setupPreEnrollmentsButtonsState() {
        if (preEnrollmentsTableView == null) {
            return;
        }
        if (approvePreEnrollmentButton != null) {
            approvePreEnrollmentButton.disableProperty().bind(preEnrollmentsTableView.getSelectionModel().selectedItemProperty().isNull());
        }
        if (rejectPreEnrollmentButton != null) {
            rejectPreEnrollmentButton.disableProperty().bind(preEnrollmentsTableView.getSelectionModel().selectedItemProperty().isNull());
        }
    }

    private void loadPendingPreEnrollments() {
        try {
            if (preEnrollmentsTableView == null) {
                return;
            }
            List<PreEnrollment> pending = preEnrollmentService.getPendingPreEnrollments();
            preEnrollmentsTableView.getItems().setAll(pending);
            preEnrollmentsTableView.getSelectionModel().clearSelection();
            Platform.runLater(preEnrollmentsTableView::refresh);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors du chargement des pre-inscriptions", e);
        }
    }

    @FXML
    private void handleApprovePreEnrollment() {
        PreEnrollment selected = preEnrollmentsTableView != null ? preEnrollmentsTableView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            return;
        }

        if (!AlertUtils.showConfirmation("Validation", "Valider la pre-inscription de " + selected.getFullName() + " ?")) {
            return;
        }

        PreEnrollmentAdminService.Credentials creds = preEnrollmentAdminService.approve(selected);
        if (creds != null) {
            AlertUtils.showSuccess(
                    "Pre-inscription validee",
                    "Compte cree:\nUsername: " + creds.username() + "\nMot de passe: " + creds.password()
            );
            refreshDashboardView();
        } else {
            AlertUtils.showError("Pre-inscription", "Impossible de valider cette demande.");
        }
    }

    @FXML
    private void handleRejectPreEnrollment() {
        PreEnrollment selected = preEnrollmentsTableView != null ? preEnrollmentsTableView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            return;
        }

        if (!AlertUtils.showConfirmation("Refus", "Refuser la pre-inscription de " + selected.getFullName() + " ?")) {
            return;
        }

        boolean ok = preEnrollmentAdminService.reject(selected.getId());
        if (ok) {
            AlertUtils.showSuccess("Pre-inscription", "Demande refusee.");
            refreshDashboardView();
        } else {
            AlertUtils.showError("Pre-inscription", "Impossible de refuser la demande.");
        }
    }

    private void loadAllStudents() {
        try {
            List<Student> students = studentService.getAllStudents();
            allStudents.setAll(students);
            applyStudentsFilter();
            if (studentsTableView != null) {
                studentsTableView.getSelectionModel().clearSelection();
                Platform.runLater(studentsTableView::refresh);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors du chargement des eleves", e);
        }
    }

    private void loadAllTeachers() {
        try {
            List<Teacher> teachers = teacherService.getAllTeachers();
            teacherAssignmentSummaryById.clear();
            for (Teacher teacher : teachers) {
                if (teacher == null || teacher.getId() <= 0) {
                    continue;
                }
                teacherAssignmentSummaryById.put(teacher.getId(), teacherAssignmentService.buildAssignmentSummary(teacher.getId()));
            }
            allTeachers.setAll(teachers);
            applyTeachersFilter();
            if (teachersTableView != null) {
                teachersTableView.getSelectionModel().clearSelection();
                Platform.runLater(teachersTableView::refresh);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors du chargement des enseignants", e);
        }
    }

    private void setupStudentsTable() {
        if (studentsTableView == null) {
            return;
        }

        studentsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        studentsTableView.setItems(filteredStudents);
        TableViewUtils.bindAutoFitHeight(studentsTableView, 10);

        if (studentNameColumn != null) {
            studentNameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? data.getValue().getFullName() : ""
            ));
        }
        if (studentUsernameColumn != null) {
            studentUsernameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null && data.getValue().getUser() != null ? data.getValue().getUser().getUsername() : ""
            ));
        }
        if (studentMatriculeColumn != null) {
            studentMatriculeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? safe(data.getValue().getMatricule()) : ""
            ));
        }
        if (studentLevelColumn != null) {
            studentLevelColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null && data.getValue().getLevel() != null ? safe(data.getValue().getLevel().getName()) : ""
            ));
        }
        if (studentClassColumn != null) {
            studentClassColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? safe(data.getValue().getClassName()) : ""
            ));
        }
        if (studentStatusColumn != null) {
            studentStatusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? formatStudentStatus(data.getValue().getStatus()) : ""
            ));
        }
    }

    private void setupTeachersTable() {
        if (teachersTableView == null) {
            return;
        }

        teachersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        teachersTableView.setItems(filteredTeachers);
        TableViewUtils.bindAutoFitHeight(teachersTableView, 10);

        if (teacherNameColumn != null) {
            teacherNameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? data.getValue().getFullName() : ""
            ));
        }
        if (teacherUsernameColumn != null) {
            teacherUsernameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null && data.getValue().getUser() != null ? safe(data.getValue().getUser().getUsername()) : ""
            ));
        }
        if (teacherMatriculeColumn != null) {
            teacherMatriculeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? safe(data.getValue().getMatricule()) : ""
            ));
        }
        if (teacherPhoneColumn != null) {
            teacherPhoneColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? safe(data.getValue().getPhone()) : ""
            ));
        }
        if (teacherSpecializationColumn != null) {
            teacherSpecializationColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? safe(data.getValue().getSpecialization()) : ""
            ));
        }
        if (teacherAssignmentsColumn != null) {
            teacherAssignmentsColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                    data.getValue() != null ? safe(teacherAssignmentSummaryById.getOrDefault(data.getValue().getId(), "Aucune affectation")) : ""
            ));
        }
    }

    private void setupStudentsFilters() {
        if (studentsLevelFilterCombo != null) {
            models.Level all = new models.Level();
            all.setId(0);
            all.setName("Tous niveaux");
            all.setNumber(0);
            studentsLevelFilterCombo.getItems().setAll(all);
            studentsLevelFilterCombo.getItems().addAll(levelDAO.getAllLevels());
            studentsLevelFilterCombo.getSelectionModel().selectFirst();
            studentsLevelFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
                refreshStudentsClassFilter();
                applyStudentsFilter();
            });
        }

        if (studentsClassFilterCombo != null) {
            configureClassFilterCombo(studentsClassFilterCombo);
            refreshStudentsClassFilter();
            studentsClassFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyStudentsFilter());
        }

        if (studentsSearchField != null) {
            studentsSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyStudentsFilter());
        }

        applyStudentsFilter();
    }

    private void setupTeachersFilters() {
        if (teachersSearchField != null) {
            teachersSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyTeachersFilter());
        }
        applyTeachersFilter();
    }

    private void refreshStudentsClassFilter() {
        if (studentsClassFilterCombo == null) {
            return;
        }
        studentsClassFilterCombo.getItems().clear();
        studentsClassFilterCombo.getItems().add(null); // toutes classes

        int levelId = studentsLevelFilterCombo != null && studentsLevelFilterCombo.getValue() != null
                ? studentsLevelFilterCombo.getValue().getId()
                : 0;
        List<SchoolClass> classes = levelId > 0 ? classDAO.getClassesByLevel(levelId) : classService.getAllClasses();
        studentsClassFilterCombo.getItems().addAll(classes);
        studentsClassFilterCombo.getSelectionModel().selectFirst();
    }

    private void configureClassFilterCombo(ComboBox<SchoolClass> combo) {
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SchoolClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("Toutes classes");
                } else {
                    setText(item.getName());
                }
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SchoolClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("Toutes classes");
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    private void setupStudentsActionButtonsState() {
        if (studentsTableView == null) {
            return;
        }
        if (editStudentButton != null) {
            editStudentButton.disableProperty().bind(studentsTableView.getSelectionModel().selectedItemProperty().isNull());
        }
        if (deleteStudentButton != null) {
            deleteStudentButton.disableProperty().bind(studentsTableView.getSelectionModel().selectedItemProperty().isNull());
        }
    }

    private void setupTeachersActionButtonsState() {
        if (teachersTableView == null) {
            return;
        }
        if (editTeacherButton != null) {
            editTeacherButton.disableProperty().bind(teachersTableView.getSelectionModel().selectedItemProperty().isNull());
        }
        if (assignTeacherButton != null) {
            assignTeacherButton.disableProperty().bind(teachersTableView.getSelectionModel().selectedItemProperty().isNull());
        }
    }

    private void applyStudentsFilter() {
        String query = studentsSearchField != null && studentsSearchField.getText() != null
                ? studentsSearchField.getText().trim().toLowerCase()
                : "";
        int levelId = studentsLevelFilterCombo != null && studentsLevelFilterCombo.getValue() != null
                ? studentsLevelFilterCombo.getValue().getId()
                : 0;
        final Integer classIdFinal;
        if (studentsClassFilterCombo != null) {
            SchoolClass sc = studentsClassFilterCombo.getValue();
            classIdFinal = (sc != null && sc.getId() > 0) ? sc.getId() : null;
        } else {
            classIdFinal = null;
        }

        filteredStudents.setPredicate(student -> {
            if (student == null) {
                return false;
            }
            if (levelId > 0) {
                if (student.getLevel() == null || student.getLevel().getId() != levelId) {
                    return false;
                }
            }
            if (classIdFinal != null) {
                if (student.getSchoolClass() == null || student.getSchoolClass().getId() != classIdFinal) {
                    return false;
                }
            }
            if (!query.isEmpty()) {
                String name = student.getFullName() != null ? student.getFullName().toLowerCase() : "";
                String matricule = student.getMatricule() != null ? student.getMatricule().toLowerCase() : "";
                String username = (student.getUser() != null && student.getUser().getUsername() != null)
                        ? student.getUser().getUsername().toLowerCase()
                        : "";
                return name.contains(query) || matricule.contains(query) || username.contains(query);
            }
            return true;
        });
    }

    private void applyTeachersFilter() {
        String query = teachersSearchField != null && teachersSearchField.getText() != null
                ? teachersSearchField.getText().trim().toLowerCase()
                : "";

        filteredTeachers.setPredicate(teacher -> {
            if (teacher == null) {
                return false;
            }
            if (query.isEmpty()) {
                return true;
            }

            String fullName = teacher.getFullName() != null ? teacher.getFullName().toLowerCase() : "";
            String username = teacher.getUser() != null && teacher.getUser().getUsername() != null
                    ? teacher.getUser().getUsername().toLowerCase()
                    : "";
            String matricule = teacher.getMatricule() != null ? teacher.getMatricule().toLowerCase() : "";
            String specialization = teacher.getSpecialization() != null ? teacher.getSpecialization().toLowerCase() : "";
            String assignments = teacherAssignmentSummaryById.getOrDefault(teacher.getId(), "").toLowerCase();

            return fullName.contains(query)
                    || username.contains(query)
                    || matricule.contains(query)
                    || specialization.contains(query)
                    || assignments.contains(query);
        });
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeStudentStatus(String status) {
        if (status == null || status.isBlank()) {
            return Constants.STUDENT_STATUS_WAITING_LIST;
        }
        if (Constants.STUDENT_STATUS_PENDING.equalsIgnoreCase(status)) {
            return Constants.STUDENT_STATUS_WAITING_LIST;
        }
        return status;
    }

    private String formatStudentStatus(String status) {
        String normalized = normalizeStudentStatus(status);
        return switch (normalized) {
            case Constants.STUDENT_STATUS_ACTIVE -> "Actif";
            case Constants.STUDENT_STATUS_INACTIVE -> "Inactif";
            case Constants.STUDENT_STATUS_WAITING_LIST -> "Liste d'attente";
            default -> normalized;
        };
    }

    @FXML
    private void handleAddStudent() {
        StudentEditResult result = showStudentDialog(null);
        if (result == null) {
            return;
        }

        try {
            int studentId = studentManagementService.createStudent(result.user(), result.student(), true);
            if (studentId > 0) {
                AlertUtils.showSuccess("Eleves", "Eleve cree avec succes (ID=" + studentId + ").");
                refreshDashboardView();
            } else {
                AlertUtils.showError("Eleves", "Echec de creation de l'eleve. Verifiez les champs (username unique, matricule unique).");
            }
        } catch (IllegalStateException e) {
            AlertUtils.showError("Eleves", e.getMessage());
        }
    }

    @FXML
    private void handleEditStudent() {
        Student selected = studentsTableView != null ? studentsTableView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            return;
        }

        // Charger le user complet (password notamment) pour ne pas l'ecraser.
        User fullUser = selected.getUser() != null ? userService.getUserById(selected.getUser().getId()) : null;
        if (fullUser == null) {
            AlertUtils.showError("Eleves", "Impossible de charger le compte utilisateur de l'eleve.");
            return;
        }
        selected.setUser(fullUser);

        StudentEditResult result = showStudentDialog(selected);
        if (result == null) {
            return;
        }

        try {
            boolean updated = studentManagementService.updateStudent(result.user(), result.student(), true);
            if (updated) {
                AlertUtils.showSuccess("Eleves", "Eleve mis a jour avec succes.");
                refreshDashboardView();
            } else {
                AlertUtils.showError("Eleves", "Echec de mise a jour de l'eleve.");
            }
        } catch (IllegalStateException e) {
            AlertUtils.showError("Eleves", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteStudent() {
        Student selected = studentsTableView != null ? studentsTableView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            return;
        }

        String label = selected.getFullName() + (selected.getMatricule() != null ? " (" + selected.getMatricule() + ")" : "");
        if (!AlertUtils.showConfirmation("Suppression", "Supprimer (desactiver) l'eleve " + label + " ?")) {
            return;
        }

        boolean ok = studentManagementService.deactivateStudent(selected.getId());
        if (ok) {
            AlertUtils.showSuccess("Suppression", "Eleve desactive.");
            refreshDashboardView();
        } else {
            AlertUtils.showError("Suppression", "Impossible de desactiver l'eleve.");
        }
    }

    @FXML
    private void handleAddTeacher() {
        TeacherEditResult result = showTeacherDialog(null);
        if (result == null) {
            return;
        }

        int teacherId = teacherManagementService.createTeacher(result.user(), result.teacher());
        if (teacherId > 0) {
            AlertUtils.showSuccess("Enseignants", "Enseignant cree avec succes (ID=" + teacherId + ").");
            refreshDashboardView();
        } else {
            AlertUtils.showError("Enseignants", "Echec de creation de l'enseignant. Verifiez les champs (username unique, code unique).");
        }
    }

    @FXML
    private void handleEditTeacher() {
        Teacher selected = teachersTableView != null ? teachersTableView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            return;
        }

        User fullUser = selected.getUser() != null ? userService.getUserById(selected.getUser().getId()) : null;
        if (fullUser == null) {
            AlertUtils.showError("Enseignants", "Impossible de charger le compte utilisateur de l'enseignant.");
            return;
        }
        selected.setUser(fullUser);

        TeacherEditResult result = showTeacherDialog(selected);
        if (result == null) {
            return;
        }

        boolean updated = teacherManagementService.updateTeacher(result.user(), result.teacher());
        if (updated) {
            AlertUtils.showSuccess("Enseignants", "Enseignant mis a jour avec succes.");
            refreshDashboardView();
        } else {
            AlertUtils.showError("Enseignants", "Echec de mise a jour de l'enseignant.");
        }
    }

    @FXML
    private void handleAssignTeacher() {
        Teacher selected = teachersTableView != null ? teachersTableView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            return;
        }

        boolean saved = showTeacherAssignmentDialog(selected);
        if (saved) {
            AlertUtils.showSuccess("Affectations", "Affectations enregistrees.");
            refreshDashboardView();
        }
    }

    private StudentEditResult showStudentDialog(Student existing) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter eleve" : "Modifier eleve");
        dialog.setHeaderText(null);
        ThemeManager.apply(dialog.getDialogPane());

        javafx.scene.control.ButtonType saveType = new javafx.scene.control.ButtonType(
                "Enregistrer",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(saveType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(14));

        javafx.scene.control.TextField firstNameField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField lastNameField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField usernameField = new javafx.scene.control.TextField();
        javafx.scene.control.PasswordField passwordField = new javafx.scene.control.PasswordField();
        javafx.scene.control.TextField emailField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField phoneField = new javafx.scene.control.TextField();

        javafx.scene.control.TextField matriculeField = new javafx.scene.control.TextField();
        javafx.scene.control.ComboBox<models.Level> levelCombo = new javafx.scene.control.ComboBox<>();
        javafx.scene.control.ComboBox<SchoolClass> classCombo = new javafx.scene.control.ComboBox<>();
        javafx.scene.control.DatePicker birthDatePicker = new javafx.scene.control.DatePicker();
        javafx.scene.control.TextField addressField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField parentNameField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField parentPhoneField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField parentEmailField = new javafx.scene.control.TextField();
        javafx.scene.control.ComboBox<String> statusCombo = new javafx.scene.control.ComboBox<>();

        levelCombo.getItems().setAll(levelDAO.getAllLevels());
        statusCombo.getItems().setAll(
                Constants.STUDENT_STATUS_WAITING_LIST,
                Constants.STUDENT_STATUS_ACTIVE,
                Constants.STUDENT_STATUS_INACTIVE
        );
        statusCombo.getSelectionModel().select(Constants.STUDENT_STATUS_WAITING_LIST);

        // Charger classes selon niveau
        levelCombo.valueProperty().addListener((obs, oldV, newV) -> {
            classCombo.getItems().clear();
            classCombo.getItems().add(null); // Non assigne
            if (newV != null && newV.getId() > 0) {
                classCombo.getItems().addAll(classDAO.getClassesByLevel(newV.getId()));
            }
            classCombo.getSelectionModel().selectFirst();
        });
        classCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SchoolClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("Non assigne");
                } else {
                    setText(item.getName());
                }
            }
        });
        classCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SchoolClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("Non assigne");
                } else {
                    setText(item.getName());
                }
            }
        });

        int row = 0;
        grid.add(new Label("Prenom*"), 0, row);
        grid.add(firstNameField, 1, row);
        grid.add(new Label("Nom*"), 2, row);
        grid.add(lastNameField, 3, row);
        row++;

        grid.add(new Label("Username*"), 0, row);
        grid.add(usernameField, 1, row);
        grid.add(new Label(existing == null ? "Mot de passe*" : "Mot de passe (laisser vide)"), 2, row);
        grid.add(passwordField, 3, row);
        row++;

        grid.add(new Label("Email"), 0, row);
        grid.add(emailField, 1, row);
        grid.add(new Label("Tel"), 2, row);
        grid.add(phoneField, 3, row);
        row++;

        grid.add(new Label("Matricule"), 0, row);
        grid.add(matriculeField, 1, row);
        grid.add(new Label("Niveau*"), 2, row);
        grid.add(levelCombo, 3, row);
        row++;

        grid.add(new Label("Classe"), 0, row);
        grid.add(classCombo, 1, row);
        grid.add(new Label("Statut"), 2, row);
        grid.add(statusCombo, 3, row);
        row++;

        grid.add(new Label("Date naissance"), 0, row);
        grid.add(birthDatePicker, 1, row);
        grid.add(new Label("Adresse"), 2, row);
        grid.add(addressField, 3, row);
        row++;

        grid.add(new Label("Parent"), 0, row);
        grid.add(parentNameField, 1, row);
        grid.add(new Label("Tel parent"), 2, row);
        grid.add(parentPhoneField, 3, row);
        row++;

        grid.add(new Label("Email parent"), 0, row);
        grid.add(parentEmailField, 1, row);

        Integer existingClassId = null;
        if (existing != null && existing.getSchoolClass() != null) {
            existingClassId = existing.getSchoolClass().getId();
        }

        if (existing != null && existing.getUser() != null) {
            User u = existing.getUser();
            firstNameField.setText(safe(u.getFirstName()));
            lastNameField.setText(safe(u.getLastName()));
            usernameField.setText(safe(u.getUsername()));
            emailField.setText(safe(u.getEmail()));
            phoneField.setText(safe(u.getPhone()));

            matriculeField.setText(safe(existing.getMatricule()));
            if (existing.getBirthDate() != null && !existing.getBirthDate().isBlank()) {
                java.time.LocalDate parsed = DateUtils.parseDate(existing.getBirthDate().trim());
                if (parsed != null) {
                    birthDatePicker.setValue(parsed);
                }
            }
            addressField.setText(safe(existing.getAddress()));
            parentNameField.setText(safe(existing.getParentName()));
            parentPhoneField.setText(safe(existing.getParentPhone()));
            parentEmailField.setText(safe(existing.getParentEmail()));

            if (existing.getLevel() != null) {
                models.Level match = levelCombo.getItems().stream()
                        .filter(l -> l != null && l.getId() == existing.getLevel().getId())
                        .findFirst()
                        .orElse(null);
                if (match != null) {
                    // selection niveau + chargement classes
                    levelCombo.getSelectionModel().select(match);
                } else {
                    levelCombo.getSelectionModel().selectFirst();
                }
            } else {
                levelCombo.getSelectionModel().selectFirst();
            }

            statusCombo.getSelectionModel().select(normalizeStudentStatus(existing.getStatus()));
        } else {
            levelCombo.getSelectionModel().selectFirst();
        }

        // selection classe si possible (apres chargement des classes)
        if (existingClassId != null) {
            final Integer classIdFinal = existingClassId;
            SchoolClass cls = classCombo.getItems().stream()
                    .filter(c -> c != null && c.getId() == classIdFinal)
                    .findFirst()
                    .orElse(null);
            if (cls != null) {
                classCombo.getSelectionModel().select(cls);
            }
        }

        dialog.getDialogPane().setContent(grid);

        javafx.beans.value.ObservableBooleanValue passwordMissing = existing == null
                ? passwordField.textProperty().isEmpty()
                : new javafx.beans.property.SimpleBooleanProperty(false);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.disableProperty().bind(
                firstNameField.textProperty().isEmpty()
                        .or(lastNameField.textProperty().isEmpty())
                        .or(usernameField.textProperty().isEmpty())
                        .or(levelCombo.valueProperty().isNull())
                        .or(passwordMissing)
        );

        dialog.setResultConverter(btn -> btn);
        Optional<javafx.scene.control.ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() != saveType) {
            return null;
        }

        models.Level selectedLevel = levelCombo.getValue();
        if (selectedLevel == null) {
            return null;
        }

        User user = existing != null && existing.getUser() != null ? existing.getUser() : new User();
        user.setRole(Constants.ROLE_STUDENT);
        user.setFirstName(firstNameField.getText().trim());
        user.setLastName(lastNameField.getText().trim());
        user.setUsername(usernameField.getText().trim());
        user.setEmail(emailField.getText() != null ? emailField.getText().trim() : null);
        user.setPhone(phoneField.getText() != null ? phoneField.getText().trim() : null);
        user.setActive(true);
        if (existing == null) {
            user.setPassword(passwordField.getText().trim());
        } else {
            String newPwd = passwordField.getText() != null ? passwordField.getText().trim() : "";
            if (!newPwd.isEmpty()) {
                user.setPassword(newPwd);
            }
        }

        Student student = existing != null ? existing : new Student();
        student.setUser(user);
        student.setLevel(selectedLevel);
        student.setSchoolClass(classCombo.getValue());
        student.setMatricule(matriculeField.getText() != null ? matriculeField.getText().trim() : null);
        student.setBirthDate(birthDatePicker.getValue() != null ? DateUtils.formatDate(birthDatePicker.getValue()) : null);
        student.setAddress(addressField.getText() != null ? addressField.getText().trim() : null);
        student.setParentName(parentNameField.getText() != null ? parentNameField.getText().trim() : null);
        student.setParentPhone(parentPhoneField.getText() != null ? parentPhoneField.getText().trim() : null);
        student.setParentEmail(parentEmailField.getText() != null ? parentEmailField.getText().trim() : null);
        student.setStatus(normalizeStudentStatus(statusCombo.getValue()));

        return new StudentEditResult(user, student);
    }

    private TeacherEditResult showTeacherDialog(Teacher existing) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter enseignant" : "Modifier enseignant");
        dialog.setHeaderText(null);
        ThemeManager.apply(dialog.getDialogPane());

        javafx.scene.control.ButtonType saveType = new javafx.scene.control.ButtonType(
                "Enregistrer",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(saveType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(14));

        javafx.scene.control.TextField firstNameField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField lastNameField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField usernameField = new javafx.scene.control.TextField();
        javafx.scene.control.PasswordField passwordField = new javafx.scene.control.PasswordField();
        javafx.scene.control.TextField emailField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField phoneField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField matriculeField = new javafx.scene.control.TextField();
        javafx.scene.control.TextArea qualificationsArea = new javafx.scene.control.TextArea();
        qualificationsArea.setPrefRowCount(3);
        qualificationsArea.setWrapText(true);

        int row = 0;
        grid.add(new Label("Prenom*"), 0, row);
        grid.add(firstNameField, 1, row);
        grid.add(new Label("Nom*"), 2, row);
        grid.add(lastNameField, 3, row);
        row++;

        grid.add(new Label("Username*"), 0, row);
        grid.add(usernameField, 1, row);
        grid.add(new Label(existing == null ? "Mot de passe*" : "Mot de passe (laisser vide)"), 2, row);
        grid.add(passwordField, 3, row);
        row++;

        grid.add(new Label("Email"), 0, row);
        grid.add(emailField, 1, row);
        grid.add(new Label("Telephone"), 2, row);
        grid.add(phoneField, 3, row);
        row++;

        grid.add(new Label("Code*"), 0, row);
        grid.add(matriculeField, 1, row);
        row++;

        grid.add(new Label("Qualifications"), 0, row);
        grid.add(qualificationsArea, 1, row, 3, 1);

        if (existing != null) {
            Teacher teacher = existing;
            User user = teacher.getUser();
            if (user != null) {
                firstNameField.setText(safe(user.getFirstName()));
                lastNameField.setText(safe(user.getLastName()));
                usernameField.setText(safe(user.getUsername()));
                emailField.setText(safe(user.getEmail()));
                phoneField.setText(safe(user.getPhone()));
            }
            matriculeField.setText(safe(teacher.getMatricule()));
            qualificationsArea.setText(safe(teacher.getQualifications()));
        }

        dialog.getDialogPane().setContent(grid);

        javafx.beans.value.ObservableBooleanValue passwordMissing = existing == null
                ? passwordField.textProperty().isEmpty()
                : new javafx.beans.property.SimpleBooleanProperty(false);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.disableProperty().bind(
                firstNameField.textProperty().isEmpty()
                        .or(lastNameField.textProperty().isEmpty())
                        .or(usernameField.textProperty().isEmpty())
                        .or(matriculeField.textProperty().isEmpty())
                        .or(passwordMissing)
        );

        dialog.setResultConverter(btn -> btn);
        Optional<javafx.scene.control.ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() != saveType) {
            return null;
        }

        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String firstName = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
        String lastName = lastNameField.getText() != null ? lastNameField.getText().trim() : "";
        String matricule = matriculeField.getText() != null ? matriculeField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : null;
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : null;

        if (!ValidationUtils.isValidFullName(firstName, lastName)) {
            AlertUtils.showError("Enseignants", "Nom et prenom obligatoires (min 2 caracteres).");
            return null;
        }
        if (!ValidationUtils.isValidUsername(username)) {
            AlertUtils.showError("Enseignants", "Username invalide (3 a 20 caracteres, lettres/chiffres/_).");
            return null;
        }
        if (email != null && !email.isBlank() && !ValidationUtils.isValidEmail(email)) {
            AlertUtils.showError("Enseignants", "Email invalide.");
            return null;
        }
        if (phone != null && !phone.isBlank() && !ValidationUtils.isValidPhone(phone)) {
            AlertUtils.showError("Enseignants", "Telephone invalide (8 a 15 chiffres).");
            return null;
        }

        User user = existing != null && existing.getUser() != null ? existing.getUser() : new User();
        user.setRole(Constants.ROLE_TEACHER);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setActive(true);
        if (existing == null) {
            user.setPassword(passwordField.getText().trim());
        } else {
            String newPassword = passwordField.getText() != null ? passwordField.getText().trim() : "";
            if (!newPassword.isEmpty()) {
                user.setPassword(newPassword);
            }
        }

        Teacher teacher = existing != null ? existing : new Teacher();
        teacher.setUser(user);
        teacher.setMatricule(matricule);
        teacher.setPhone(phone);
        teacher.setEmail(email);
        teacher.setQualifications(safe(qualificationsArea.getText()).trim());
        teacher.setActive(true);

        return new TeacherEditResult(user, teacher);
    }

    private boolean showTeacherAssignmentDialog(Teacher teacher) {
        if (teacher == null) {
            return false;
        }

        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Affectations enseignant");
        dialog.setHeaderText("Classes et matieres pour " + teacher.getFullName());
        ThemeManager.apply(dialog.getDialogPane());

        javafx.scene.control.ButtonType saveType = new javafx.scene.control.ButtonType(
                "Enregistrer",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(saveType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
        root.setPadding(new Insets(14));

        Label hint = new Label("Choisissez une classe puis cochez les matieres. Laisser vide supprime les affectations de cette classe.");
        hint.getStyleClass().add("muted");

        ComboBox<SchoolClass> classCombo = new ComboBox<>();
        classCombo.setMaxWidth(Double.MAX_VALUE);
        configureClassCombo(classCombo, "Selectionner une classe");
        List<SchoolClass> allClassesSorted = classService.getAllClasses().stream()
                .sorted((c1, c2) -> {
                    int level1 = c1 != null && c1.getLevel() != null ? c1.getLevel().getNumber() : 0;
                    int level2 = c2 != null && c2.getLevel() != null ? c2.getLevel().getNumber() : 0;
                    if (level1 != level2) {
                        return Integer.compare(level1, level2);
                    }
                    String name1 = c1 != null ? safe(c1.getName()) : "";
                    String name2 = c2 != null ? safe(c2.getName()) : "";
                    return name1.compareToIgnoreCase(name2);
                })
                .toList();
        classCombo.getItems().setAll(allClassesSorted);

        ListView<Subject> subjectListView = new ListView<>();
        subjectListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        subjectListView.setPrefHeight(220);
        subjectListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Subject item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.getName());
            }
        });

        javafx.scene.control.TextArea currentAssignmentsArea = new javafx.scene.control.TextArea();
        currentAssignmentsArea.setEditable(false);
        currentAssignmentsArea.setWrapText(true);
        currentAssignmentsArea.setPrefRowCount(5);
        currentAssignmentsArea.setText(buildTeacherAssignmentsText(teacher.getId()));

        classCombo.valueProperty().addListener((obs, oldValue, selectedClass) -> {
            subjectListView.getItems().clear();
            subjectListView.getSelectionModel().clearSelection();
            if (selectedClass == null || selectedClass.getLevel() == null) {
                return;
            }

            List<Subject> availableSubjects = subjectDAO.getSubjectsByLevel(selectedClass.getLevel().getNumber());
            subjectListView.getItems().setAll(availableSubjects);

            List<Integer> selectedSubjectIds = teacherAssignmentService.getSubjectIdsByTeacherAndClass(teacher.getId(), selectedClass.getId());
            for (int i = 0; i < availableSubjects.size(); i++) {
                Subject subject = availableSubjects.get(i);
                if (selectedSubjectIds.contains(subject.getId())) {
                    subjectListView.getSelectionModel().select(i);
                }
            }
        });

        if (!allClassesSorted.isEmpty()) {
            classCombo.getSelectionModel().selectFirst();
        }

        root.getChildren().addAll(
                hint,
                new Label("Classe"),
                classCombo,
                new Label("Matieres"),
                subjectListView,
                new Label("Affectations actuelles"),
                currentAssignmentsArea
        );
        dialog.getDialogPane().setContent(root);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.disableProperty().bind(classCombo.valueProperty().isNull());

        dialog.setResultConverter(btn -> btn);
        Optional<javafx.scene.control.ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() != saveType) {
            return false;
        }

        SchoolClass selectedClass = classCombo.getValue();
        if (selectedClass == null) {
            return false;
        }

        List<Integer> subjectIds = subjectListView.getSelectionModel().getSelectedItems().stream()
                .map(Subject::getId)
                .toList();
        return teacherAssignmentService.replaceAssignmentsForTeacherAndClass(teacher.getId(), selectedClass.getId(), subjectIds);
    }

    private void configureClassCombo(ComboBox<SchoolClass> combo, String emptyLabel) {
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SchoolClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(empty ? null : emptyLabel);
                    return;
                }
                String levelName = item.getLevel() != null ? safe(item.getLevel().getName()) : "N/A";
                setText(item.getName() + " (" + levelName + ")");
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SchoolClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(empty ? null : emptyLabel);
                    return;
                }
                String levelName = item.getLevel() != null ? safe(item.getLevel().getName()) : "N/A";
                setText(item.getName() + " (" + levelName + ")");
            }
        });
    }

    private String buildTeacherAssignmentsText(int teacherId) {
        List<dao.TeacherAssignmentDAO.AssignmentDetail> assignments = teacherAssignmentService.getAssignmentsByTeacher(teacherId);
        if (assignments.isEmpty()) {
            return "Aucune affectation";
        }

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (dao.TeacherAssignmentDAO.AssignmentDetail assignment : assignments) {
            String key = assignment.className() + " (" + assignment.levelName() + ")";
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(assignment.subjectName());
        }

        StringBuilder sb = new StringBuilder();
        grouped.forEach((key, subjects) -> {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("- ").append(key).append(": ").append(String.join(", ", subjects.stream().distinct().toList()));
        });
        return sb.toString();
    }

    private record StudentEditResult(User user, Student student) {
    }

    private record TeacherEditResult(User user, Teacher teacher) {
    }

    private void loadStatistics() {
        int studentCount = studentService.getAllStudents().size();
        int teacherCount = teacherService.getAllTeachers().size();
        int classCount = classService.getAllClasses().size();
        int pendingCount = studentService.getPendingStudents().size();

        studentCountLabel.setText(String.valueOf(studentCount));
        teacherCountLabel.setText(String.valueOf(teacherCount));
        classCountLabel.setText(String.valueOf(classCount));
        if (pendingCountLabel != null) {
            pendingCountLabel.setText(String.valueOf(pendingCount));
        }
    }

    private void loadStudentsByLevelChart() {
        try {
            List<Student> students = studentService.getAllStudents();
            Map<String, Long> studentsByLevel = students.stream()
                    .filter(student -> student.getLevel() != null)
                    .collect(Collectors.groupingBy(student -> student.getLevel().getName(), Collectors.counting()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Nombre d'eleves");
            studentsByLevel.forEach((level, count) -> series.getData().add(new XYChart.Data<>(level, count)));

            studentsByLevelChart.getData().clear();
            studentsByLevelChart.getData().add(series);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors du chargement du graphique des eleves", e);
        }
    }

    private void loadClassesByLevelChart() {
        try {
            List<SchoolClass> classes = classService.getAllClasses();
            Map<String, Double> fillRateByLevel = classes.stream()
                    .filter(schoolClass -> schoolClass.getLevel() != null)
                    .collect(Collectors.groupingBy(
                            schoolClass -> schoolClass.getLevel().getName(),
                            Collectors.collectingAndThen(Collectors.toList(), groupedClasses -> {
                                int totalStudents = groupedClasses.stream()
                                        .mapToInt(SchoolClass::getCurrentStudents)
                                        .sum();
                                int totalCapacity = groupedClasses.stream()
                                        .mapToInt(schoolClass -> Math.max(0, schoolClass.getCapacity()))
                                        .sum();
                                if (totalCapacity <= 0) {
                                    return 0.0;
                                }
                                return (totalStudents * 100.0) / totalCapacity;
                            })
                    ));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Taux de remplissage (%)");
            fillRateByLevel.forEach((level, rate) -> series.getData().add(new XYChart.Data<>(level, Math.round(rate * 10.0) / 10.0)));

            classesByLevelChart.getData().clear();
            classesByLevelChart.getData().add(series);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors du chargement du graphique des classes", e);
        }
    }

    private void loadPendingStudents() {
        try {
            List<Student> pendingStudents = studentService.getPendingStudents();
            this.pendingStudents.setAll(pendingStudents);
            applyPendingFilter();
            pendingStudentsListView.getSelectionModel().clearSelection();
            logger.info("Eleves en attente charges: " + pendingStudents.size());
            Platform.runLater(pendingStudentsListView::refresh);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur lors du chargement des eleves en attente", e);
        }
    }

    @FXML
    private void handleSyncInscriptions() {
        int processed = inscriptionService.ensureActiveYearInscriptions();
        AlertUtils.showInfo("Inscriptions", processed + " inscription(s) synchronisee(s) pour l'annee active.");
        refreshDashboardView();
    }

    @FXML
    private void handleGenerateClasses() {
        inscriptionService.ensureActiveYearInscriptions();
        int created = enrollmentAdminService.generateClassesForActiveYear();
        if (created > 0) {
            AlertUtils.showInfo("Classes", "Nombre de classes ajoutees : " + created + ".");
        } else {
            AlertUtils.showInfo("Classes", "Aucune nouvelle classe a ajouter.");
        }
        refreshDashboardView();
    }

    @FXML
    private void handleAutoAssign() {
        // Bouton retire: on garde la methode (non utilisee) pour compatibilite si besoin.
    }

    @FXML
    private void handleApproveStudent() {
        Student selectedStudent = pendingStudentsListView.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            AlertUtils.showWarning("Attention", "Veuillez sÃ©lectionner un Ã©lÃ¨ve.");
            return;
        }

        if (selectedStudent.getLevel() == null) {
            AlertUtils.showError("Erreur", "Cet eleve n'a pas de niveau associe.");
            return;
        }

        List<SchoolClass> suggestedClasses = studentService.getSuggestedClasses(selectedStudent.getLevel().getId());
        if (suggestedClasses.isEmpty()) {
            AlertUtils.showWarning("Aucune classe", "Aucune classe disponible pour ce niveau.");
            return;
        }

        ChoiceDialog<SchoolClass> dialog = new ChoiceDialog<>(suggestedClasses.get(0), suggestedClasses);
        dialog.setTitle("Approbation de l'eleve");
        dialog.setHeaderText("Choisir une classe pour " + selectedStudent.getFullName());
        dialog.setContentText("Classe:");
        styleClassChoiceDialog(dialog);

        Optional<SchoolClass> selectedClass = dialog.showAndWait();
        if (selectedClass.isEmpty()) {
            return;
        }

        SchoolClass schoolClass = selectedClass.get();
        if (schoolClass.getCurrentStudents() >= Constants.MAX_CLASS_CAPACITY || schoolClass.isFull()) {
            AlertUtils.showWarning(
                    "Classe pleine",
                    "La classe " + schoolClass.getName() + " est pleine (" +
                            schoolClass.getCurrentStudents() + "/" + Constants.MAX_CLASS_CAPACITY + ")."
            );
            return;
        }
        boolean approved = studentService.approveStudent(
                selectedStudent.getId(),
                schoolClass.getId(),
                currentUser != null ? currentUser.getId() : null,
                currentUser != null ? currentUser.getUsername() : null,
                selectedStudent.getFullName()
        );

        if (approved) {
            AlertUtils.showSuccess("Succes", "Eleve approuve et affecte a la classe " + schoolClass.getName() + ".");
            recordDecision(selectedStudent, ApprovalDecision.APPROVED);
            pendingStudentsListView.getSelectionModel().clearSelection();
            refreshDashboardView();
        } else {
            AlertUtils.showError("Erreur", "Impossible d'approuver cet eleve. Verifiez la capacite de la classe.");
            recordDecision(selectedStudent, ApprovalDecision.REJECTED);
        }
    }

    @FXML
    private void handleRejectStudent() {
        Student selectedStudent = pendingStudentsListView.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            AlertUtils.showWarning("Attention", "Veuillez sÃ©lectionner un Ã©lÃ¨ve.");
            return;
        }

        if (AlertUtils.showConfirmation("Confirmation", "Etes-vous sur de vouloir rejeter cet eleve ?")) {
            if (studentService.rejectStudent(selectedStudent.getId())) {
                AlertUtils.showSuccess("Succes", "Eleve rejete avec succes");
                recordDecision(selectedStudent, ApprovalDecision.REJECTED);
                pendingStudentsListView.getSelectionModel().clearSelection();
                refreshDashboardView();
            } else {
                AlertUtils.showError("Erreur", "Erreur lors du rejet de l'eleve");
            }
        }
    }

    @Override
    public void handleLogout() {
        if (!AlertUtils.showConfirmation("Deconnexion", "Etes-vous sur de vouloir vous deconnecter ?")) {
            return;
        }

        try {
            if (currentUser != null) {
                AuditService.logLogout(currentUser.getId(), currentUser.getUsername());
            }
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            NavigationUtils.showLoginScreen(stage, getClass());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors du retour a la connexion", e);
            AlertUtils.showError("Erreur", "Impossible de recharger l'ecran de connexion.");
        }
    }

    @SuppressWarnings("unchecked")
    private void styleClassChoiceDialog(ChoiceDialog<SchoolClass> dialog) {
        ComboBox<SchoolClass> comboBox = (ComboBox<SchoolClass>) dialog.getDialogPane().lookup(".combo-box");
        if (comboBox == null) {
            return;
        }

        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(SchoolClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                String text = item.getName() + " (" + item.getCurrentStudents() + "/" + item.getCapacity() + ")";
                if (item.getCurrentStudents() >= Constants.MAX_CLASS_CAPACITY || item.isFull()) {
                    setText(text + " - CLASSE PLEINE");
                    setStyle("-fx-text-fill: #f5222d; -fx-font-weight: bold;");
                } else {
                    setText(text);
                    setStyle("");
                }
            }
        });
    }

    private enum ApprovalDecision {
        APPROVED,
        REJECTED
    }

    private record ApprovalDecisionRow(int studentId, String studentName, String levelName, ApprovalDecision decision) {
    }
}

