package controllers;

import dao.SchoolYearDAO;
import dao.TrimesterDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Bulletin;
import models.Grade;
import models.SchoolYear;
import models.Student;
import models.Trimester;
import models.User;
import services.AuditService;
import services.BulletinService;
import services.GradeService;
import services.StudentService;
import utils.AlertUtils;
import utils.AnimationUtils;
import utils.Constants;
import utils.DateUtils;
import utils.NavigationUtils;
import utils.ThemeManager;
import utils.TableViewUtils;

import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Controleur pour le dashboard eleve.
 */
public class StudentDashboardController implements DashboardController {
    private static final Logger logger = Logger.getLogger(StudentDashboardController.class.getName());
    private static final String EMPTY_PLACEHOLDER = "___";
    private static final String INFO_STUDENT_PARENT_TITLE = "Infos élève / Parent";
    private static final double TRIMESTER_COMBO_WIDTH = 240.0;
    private static final double TRIMESTER_COMBO_MIN_WIDTH = 190.0;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private Label classLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<SchoolYear> schoolYearCombo;

    @FXML
    private ComboBox<Trimester> trimesterCombo;

    @FXML
    private TableView<Grade> gradesTableView;

    @FXML
    private Label averageLabel;

    @FXML
    private Label rankLabel;

    @FXML
    private Label appreciationLabel;

    @FXML
    private VBox bulletinContainer;

    @FXML
    private ToggleButton themeToggle;

    private User currentUser;
    private Student currentStudent;
    private boolean autoRefreshBound;
    private final StudentService studentService = new StudentService();
    private final GradeService gradeService = new GradeService();
    private final BulletinService bulletinService = new BulletinService();
    private final SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
    private final TrimesterDAO trimesterDAO = new TrimesterDAO();

    @FXML
    public void initialize() {
        logger.info("Initialisation du dashboard eleve");
        setupGradesTable();
        setupTrimesterFilter();
        gradesTableView.setManaged(false);
        gradesTableView.setVisible(false);
        setupDynamicEffects();
        setupAutoRefresh();
        setupThemeToggle();
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

    @Override
    public void setUser(User user) {
        currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getFirstName() + " " + user.getLastName());
        loadDashboardData();
    }

    @Override
    public void loadDashboardData() {
        try {
            logger.info("Chargement des donnees du dashboard eleve");
            currentStudent = currentUser != null ? studentService.getStudentByUserId(currentUser.getId()) : null;
            populateStudentInformation();
            populateReferenceData();
            loadSelectedBulletinData();
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors du chargement des donnees", e);
        }
    }

    @FXML
    private void handleShowBulletins() {
        loadSelectedBulletinData();
    }

    @FXML
    private void handleClassDetails() {
        if (currentStudent == null) {
            AlertUtils.showWarning("Information", "Aucun dossier eleve n'est associe a ce compte.");
            return;
        }

        String details = "Classe: " + valueOrDefault(currentStudent.getClassName(), "Non assignee") + "\n" +
                "Niveau: " + (currentStudent.getLevel() != null ? currentStudent.getLevel().getName() : "Non renseigne") + "\n" +
                "Date de naissance: " + valueOrDefault(currentStudent.getBirthDate(), "Non renseignee") + "\n" +
                "Parent: " + valueOrDefault(currentStudent.getParentName(), "Non renseigne") + "\n" +
                "Téléphone parent: " + valueOrDefault(currentStudent.getParentPhone(), "Non renseigne");
        AlertUtils.showInfo(INFO_STUDENT_PARENT_TITLE, details);
    }

    @FXML
    private void handleLoadBulletin() {
        loadSelectedBulletinData();
    }

    @FXML
    private void handleExportBulletin() {
        if (currentStudent == null) {
            AlertUtils.showWarning("Export", "Aucun dossier eleve n'est associe a ce compte.");
            return;
        }

        SchoolYear schoolYear = schoolYearCombo.getSelectionModel().getSelectedItem();
        Trimester trimester = trimesterCombo.getSelectionModel().getSelectedItem();
        if (schoolYear == null || trimester == null) {
            AlertUtils.showWarning("Export", "Selectionnez une annee scolaire et un trimestre.");
            return;
        }

        List<Grade> grades = gradeService.getGradesByStudentAndPeriod(currentStudent.getId(), schoolYear.getId(), trimester.getId());
        Bulletin bulletin = bulletinService.getBulletin(currentStudent.getId(), schoolYear.getId(), trimester.getId());

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le bulletin");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier texte", "*.txt"));
        chooser.setInitialFileName(buildBulletinFileName(schoolYear, trimester));

        File target = chooser.showSaveDialog(welcomeLabel.getScene().getWindow());
        if (target == null) {
            return;
        }

        try {
            Files.writeString(target.toPath(), buildBulletinExportText(schoolYear, trimester, bulletin, grades), StandardCharsets.UTF_8);
            AlertUtils.showSuccess("Export", "Bulletin exporte avec succes.");
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur lors de l'export du bulletin", e);
            AlertUtils.showError("Export", "Impossible d'exporter le bulletin.");
        }
    }

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

    private void setupGradesTable() {
        if (gradesTableView.getColumns().size() < 3) {
            return;
        }

        gradesTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableViewUtils.bindAutoFitHeight(gradesTableView, 10);

        TableColumn<Grade, String> subjectColumn = castColumn(0);
        subjectColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getSubject() != null ? cellData.getValue().getSubject().getName() : "N/A"
                ));

        TableColumn<Grade, String> valueColumn = castColumn(1);
        valueColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatGradeValue(cellData.getValue())));

        TableColumn<Grade, String> appreciationColumn = castColumn(2);
        appreciationColumn.setCellValueFactory(cellData -> {
            Grade grade = cellData.getValue();
            return new SimpleStringProperty(formatDisplayText(valueOrDefault(grade.getRemarks(), grade.getAppreciation())));
        });
        appreciationColumn.setCellFactory(column -> createAppreciationCell());
    }

    @SuppressWarnings("unchecked")
    private TableColumn<Grade, String> castColumn(int index) {
        return (TableColumn<Grade, String>) gradesTableView.getColumns().get(index);
    }

    private void populateStudentInformation() {
        if (currentStudent == null) {
            nameLabel.setText(currentUser != null ? currentUser.getFullName() : "N/A");
            classLabel.setText("Non assignee");
            levelLabel.setText("Non renseigne");
            statusLabel.setText("Dossier eleve introuvable");
            gradesTableView.getItems().clear();
            bulletinContainer.getChildren().setAll(new Label("Aucune information eleve disponible."));
            averageLabel.setText("0.00");
            rankLabel.setText("N/A");
            setSummaryAppreciation("Aucune note");
            applyStudentStatusStyle("missing");
            return;
        }

        nameLabel.setText(currentStudent.getFullName());
        classLabel.setText(valueOrDefault(currentStudent.getClassName(), "Non assignee"));
        levelLabel.setText(currentStudent.getLevel() != null ? currentStudent.getLevel().getName() : "Non renseigne");
        statusLabel.setText(formatStudentStatus(currentStudent.getStatus()));
        applyStudentStatusStyle(currentStudent.getStatus());
    }

    private void populateReferenceData() {
        SchoolYear previousSchoolYear = schoolYearCombo.getSelectionModel().getSelectedItem();
        Trimester previousTrimester = trimesterCombo.getSelectionModel().getSelectedItem();

        List<SchoolYear> schoolYears = schoolYearDAO.getAllSchoolYears();
        schoolYearCombo.getItems().setAll(schoolYears);

        if (previousSchoolYear != null) {
            selectSchoolYear(previousSchoolYear.getId());
        } else {
            SchoolYear activeSchoolYear = schoolYearDAO.getActiveSchoolYear();
            if (activeSchoolYear != null) {
                selectSchoolYear(activeSchoolYear.getId());
            } else if (!schoolYears.isEmpty()) {
                schoolYearCombo.getSelectionModel().selectFirst();
            }
        }

        List<Trimester> trimesters = trimesterDAO.getAllTrimesters();
        trimesterCombo.getItems().setAll(trimesters);

        if (previousTrimester != null) {
            selectTrimester(previousTrimester.getId());
        } else if (!trimesters.isEmpty()) {
            trimesterCombo.getSelectionModel().selectFirst();
        }
    }

    private void selectSchoolYear(int schoolYearId) {
        for (SchoolYear schoolYear : schoolYearCombo.getItems()) {
            if (schoolYear.getId() == schoolYearId) {
                schoolYearCombo.getSelectionModel().select(schoolYear);
                return;
            }
        }
    }

    private void selectTrimester(int trimesterId) {
        for (Trimester trimester : trimesterCombo.getItems()) {
            if (trimester.getId() == trimesterId) {
                trimesterCombo.getSelectionModel().select(trimester);
                return;
            }
        }
    }

    private void loadSelectedBulletinData() {
        if (currentStudent == null) {
            return;
        }

        SchoolYear schoolYear = schoolYearCombo.getSelectionModel().getSelectedItem();
        Trimester trimester = trimesterCombo.getSelectionModel().getSelectedItem();
        if (schoolYear == null || trimester == null) {
            gradesTableView.getItems().clear();
            bulletinContainer.getChildren().setAll(new Label("Selectionnez une annee scolaire et un trimestre."));
            return;
        }

        List<Grade> grades = gradeService.getGradesByStudentAndPeriod(currentStudent.getId(), schoolYear.getId(), trimester.getId());
        Bulletin bulletin = bulletinService.getBulletin(currentStudent.getId(), schoolYear.getId(), trimester.getId());

        gradesTableView.getItems().setAll(grades);
        updateSummary(bulletin, grades);
        renderBulletin(schoolYear, trimester, bulletin, grades);
    }

    private void updateSummary(Bulletin bulletin, List<Grade> grades) {
        if (bulletin != null) {
            averageLabel.setText(String.format(Locale.US, "%.2f", bulletin.getAverage()));
            rankLabel.setText(bulletin.getRank() > 0 ? String.valueOf(bulletin.getRank()) : "N/A");
            setSummaryAppreciation(valueOrDefault(bulletin.getAppreciation(), "Aucune note"));
            return;
        }

        double average = grades.isEmpty()
                ? 0.0
                : grades.stream().mapToDouble(Grade::getValue).average().orElse(0.0);
        averageLabel.setText(String.format(Locale.US, "%.2f", average));
        rankLabel.setText("N/A");
        setSummaryAppreciation(grades.isEmpty() ? "Aucune note" : deriveAppreciation(average));
    }

    private void renderBulletin(SchoolYear schoolYear, Trimester trimester, Bulletin bulletin, List<Grade> grades) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("dashboard-card", "bulletin-document");

        Label title = new Label("Bulletin " + schoolYear.getLabel() + " - " + trimester.getName());
        title.getStyleClass().add("bulletin-title");

        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(10);
        details.getStyleClass().add("document-grid");

        addGridRow(details, 0, "Periode scolaire", schoolYear.getLabel());
        addGridRow(details, 1, "Dates annee", DateUtils.formatDateRange(schoolYear.getStartDate(), schoolYear.getEndDate()));
        addGridRow(details, 2, "Trimestre", trimester.getName());
        addGridRow(details, 3, "Dates trimestre", DateUtils.formatDateRange(trimester.getStartDate(), trimester.getEndDate()));
        addGridRow(details, 4, "Nombre de notes", String.valueOf(grades.size()));

        if (bulletin != null) {
            addGridRow(details, 5, "Date d'enregistrement", valueOrDefault(bulletin.getRecordedDate(), "Non renseignee"));
            addGridRow(details, 6, "Commentaire enseignant", valueOrDefault(bulletin.getTeacherComment(), "Aucun commentaire"));
        } else {
            addGridRow(details, 5, "Date d'enregistrement", "Bulletin non genere");
            addGridRow(details, 6, "Commentaire enseignant", grades.isEmpty() ? "Aucune note disponible" : "Moyenne calculee a partir des notes");
        }

        Label gradesTitle = new Label("Détails des notes");
        gradesTitle.getStyleClass().add("section-title");

        TableView<StudentBulletinGradeLine> gradesTable = new TableView<>();
        gradesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableViewUtils.bindAutoFitHeight(gradesTable, 14);

        TableColumn<StudentBulletinGradeLine, String> subjectColumn = new TableColumn<>("Matière");
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));

        TableColumn<StudentBulletinGradeLine, String> coefficientColumn = new TableColumn<>("Coef");
        coefficientColumn.setCellValueFactory(new PropertyValueFactory<>("coefficient"));

        TableColumn<StudentBulletinGradeLine, String> valueColumn = new TableColumn<>("Note");
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        TableColumn<StudentBulletinGradeLine, String> appreciationColumn = new TableColumn<>("Appréciation");
        appreciationColumn.setCellValueFactory(new PropertyValueFactory<>("appreciation"));
        appreciationColumn.setCellFactory(column -> createAppreciationCell());

        TableColumn<StudentBulletinGradeLine, String> remarkColumn = new TableColumn<>("Remarque");
        remarkColumn.setCellValueFactory(new PropertyValueFactory<>("remark"));

        gradesTable.getColumns().setAll(subjectColumn, coefficientColumn, valueColumn, appreciationColumn, remarkColumn);

        if (grades == null || grades.isEmpty()) {
            gradesTable.setPlaceholder(new Label("Aucune note disponible pour la période sélectionnée."));
        } else {
            List<StudentBulletinGradeLine> lines = grades.stream()
                    .sorted((a, b) -> {
                        String an = a.getSubject() != null ? a.getSubject().getName() : "";
                        String bn = b.getSubject() != null ? b.getSubject().getName() : "";
                        return an.compareToIgnoreCase(bn);
                    })
                    .map(g -> new StudentBulletinGradeLine(
                            g.getSubject() != null ? g.getSubject().getName() : "N/A",
                            String.valueOf(g.getSubject() != null ? Math.max(1, g.getSubject().getCoefficient()) : 1),
                            formatGradeValue(g),
                            formatDisplayText(g.getAppreciation()),
                            formatDisplayText(g.getRemarks())
                    ))
                    .toList();
            gradesTable.getItems().setAll(lines);
        }

        card.getChildren().setAll(title, details, gradesTitle, gradesTable);
        bulletinContainer.getChildren().setAll(card);
    }

    public static final class StudentBulletinGradeLine {
        private final String subject;
        private final String coefficient;
        private final String value;
        private final String appreciation;
        private final String remark;

        public StudentBulletinGradeLine(String subject, String coefficient, String value, String appreciation, String remark) {
            this.subject = subject;
            this.coefficient = coefficient;
            this.value = value;
            this.appreciation = appreciation;
            this.remark = remark;
        }

        public String getSubject() {
            return subject;
        }

        public String getCoefficient() {
            return coefficient;
        }

        public String getValue() {
            return value;
        }

        public String getAppreciation() {
            return appreciation;
        }

        public String getRemark() {
            return remark;
        }
    }

    private void addGridRow(GridPane grid, int rowIndex, String labelText, String valueText) {
        Label label = new Label(labelText + ":");
        label.getStyleClass().add("field-label");

        Label value = new Label(valueOrDefault(valueText, "Non renseigne"));
        value.getStyleClass().add("field-value");
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(value, Priority.ALWAYS);

        grid.add(label, 0, rowIndex);
        grid.add(value, 1, rowIndex);
    }

    private String formatStudentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Non renseigne";
        }

        return switch (status) {
            case "active" -> "Actif";
            case "pending" -> "Liste d'attente";
            case "inactive" -> "Inactif";
            case "waiting_list" -> "Liste d'attente";
            default -> status;
        };
    }

    private void applyStudentStatusStyle(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        String style = switch (normalized) {
            case "active" -> "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-background-radius: 999px; -fx-padding: 4px 9px; -fx-font-weight: bold;";
            case "pending" -> "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-background-radius: 999px; -fx-padding: 4px 9px; -fx-font-weight: bold;";
            case "waiting_list" -> "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-background-radius: 999px; -fx-padding: 4px 9px; -fx-font-weight: bold;";
            case "inactive" -> "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-background-radius: 999px; -fx-padding: 4px 9px; -fx-font-weight: bold;";
            default -> "-fx-background-color: #EEF1F5; -fx-text-fill: #475569; -fx-background-radius: 999px; -fx-padding: 4px 9px; -fx-font-weight: bold;";
        };
        statusLabel.setStyle(style);
    }

    private String deriveAppreciation(double average) {
        if (average >= 17) return "Excellent";
        if (average >= 14) return "Tres bien";
        if (average >= 12) return "Bien";
        if (average >= 10) return "Satisfaisant";
        if (average >= 7) return "A ameliorer";
        return "Insatisfaisant";
    }

    private void setupTrimesterFilter() {
        if (trimesterCombo == null) {
            return;
        }
        trimesterCombo.setPrefWidth(TRIMESTER_COMBO_WIDTH);
        trimesterCombo.setMinWidth(TRIMESTER_COMBO_MIN_WIDTH);
        trimesterCombo.setMaxWidth(Double.MAX_VALUE);
    }

    private String formatGradeValue(Grade grade) {
        if (grade == null || isMissingGradeValue(grade)) {
            return EMPTY_PLACEHOLDER;
        }
        return String.format(Locale.US, "%.2f", grade.getValue());
    }

    private boolean isMissingGradeValue(Grade grade) {
        return grade.getId() <= 0
                && grade.getValue() == 0.0
                && (grade.getRecordedDate() == null || grade.getRecordedDate().isBlank())
                && (grade.getRemarks() == null || grade.getRemarks().isBlank());
    }

    private String formatDisplayText(String value) {
        return value == null || value.isBlank() ? EMPTY_PLACEHOLDER : value;
    }

    private String buildBulletinFileName(SchoolYear schoolYear, Trimester trimester) {
        String studentName = currentStudent != null ? currentStudent.getFullName() : "eleve";
        String rawName = "bulletin_" + studentName + "_" + schoolYear.getLabel() + "_" + trimester.getName() + ".txt";
        return rawName.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private String buildBulletinExportText(SchoolYear schoolYear, Trimester trimester, Bulletin bulletin, List<Grade> grades) {
        StringBuilder builder = new StringBuilder();
        builder.append("BULLETIN DE NOTES").append(System.lineSeparator());
        builder.append("=================").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("Eleve: ").append(currentStudent.getFullName()).append(System.lineSeparator());
        builder.append("Classe: ").append(valueOrDefault(currentStudent.getClassName(), "Non assignee")).append(System.lineSeparator());
        builder.append("Niveau: ").append(currentStudent.getLevel() != null ? currentStudent.getLevel().getName() : "Non renseigne").append(System.lineSeparator());
        builder.append("Annee scolaire: ").append(schoolYear.getLabel()).append(System.lineSeparator());
        builder.append("Trimestre: ").append(trimester.getName()).append(System.lineSeparator());
        builder.append("Dates trimestre: ")
                .append(DateUtils.formatDateRange(trimester.getStartDate(), trimester.getEndDate()))
                .append(System.lineSeparator()).append(System.lineSeparator());

        builder.append(String.format("%-28s %-8s %-8s %-18s %s%n", "Matiere", "Coef", "Note", "Appreciation", "Remarque"));
        builder.append("--------------------------------------------------------------------------").append(System.lineSeparator());
        if (grades == null || grades.isEmpty()) {
            builder.append("Aucune note disponible pour cette periode.").append(System.lineSeparator());
        } else {
            grades.stream()
                    .sorted((a, b) -> {
                        String an = a.getSubject() != null ? a.getSubject().getName() : "";
                        String bn = b.getSubject() != null ? b.getSubject().getName() : "";
                        return an.compareToIgnoreCase(bn);
                    })
                    .forEach(grade -> builder.append(String.format(
                            "%-28s %-8s %-8s %-18s %s%n",
                            grade.getSubject() != null ? grade.getSubject().getName() : "N/A",
                            grade.getSubject() != null ? Math.max(1, grade.getSubject().getCoefficient()) : 1,
                            formatGradeValue(grade),
                            formatDisplayText(grade.getAppreciation()),
                            formatDisplayText(grade.getRemarks())
                    )));
        }

        builder.append(System.lineSeparator());
        if (bulletin != null) {
            builder.append("Moyenne generale: ").append(String.format(Locale.US, "%.2f", bulletin.getAverage())).append(System.lineSeparator());
            builder.append("Rang: ").append(bulletin.getRank() > 0 ? bulletin.getRank() : "N/A").append(System.lineSeparator());
            builder.append("Appreciation: ").append(valueOrDefault(bulletin.getAppreciation(), "Aucune note")).append(System.lineSeparator());
            builder.append("Commentaire enseignant: ").append(valueOrDefault(bulletin.getTeacherComment(), "Aucun commentaire")).append(System.lineSeparator());
        } else {
            double average = grades == null || grades.isEmpty()
                    ? 0.0
                    : grades.stream().mapToDouble(Grade::getValue).average().orElse(0.0);
            builder.append("Moyenne generale: ").append(grades == null || grades.isEmpty() ? EMPTY_PLACEHOLDER : String.format(Locale.US, "%.2f", average)).append(System.lineSeparator());
            builder.append("Rang: N/A").append(System.lineSeparator());
            builder.append("Appreciation: ").append(grades == null || grades.isEmpty() ? "Aucune note" : deriveAppreciation(average)).append(System.lineSeparator());
            builder.append("Commentaire enseignant: ").append(grades == null || grades.isEmpty() ? "Aucune note disponible" : "Moyenne calculee a partir des notes").append(System.lineSeparator());
        }

        return builder.toString();
    }

    private <S> TableCell<S, String> createAppreciationCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formatDisplayText(item));
                setStyle(empty ? "" : appreciationStyle(item));
            }
        };
    }

    private void setSummaryAppreciation(String appreciation) {
        String displayValue = valueOrDefault(appreciation, "Aucune note");
        appreciationLabel.setText(displayValue);
        appreciationLabel.setStyle(appreciationStyle(displayValue));
    }

    private String appreciationStyle(String appreciation) {
        String normalized = normalizeAppreciation(appreciation);
        if (normalized.equals("excellent")) {
            return "-fx-background-color: #DFF5E1; -fx-text-fill: #1E7D3B; -fx-background-radius: 8px; -fx-padding: 4px 8px;";
        }
        if (normalized.equals("tres bien")) {
            return "-fx-background-color: #DDEEFF; -fx-text-fill: #1D5F9F; -fx-background-radius: 8px; -fx-padding: 4px 8px;";
        }
        if (normalized.equals("bien") || normalized.equals("satisfaisant")) {
            return "-fx-background-color: #FFF3D6; -fx-text-fill: #9A5B00; -fx-background-radius: 8px; -fx-padding: 4px 8px;";
        }
        if (normalized.equals("a ameliorer") || normalized.equals("insatisfaisant") || normalized.equals("insuffisant")) {
            return "-fx-background-color: #FFE3D8; -fx-text-fill: #A33A1F; -fx-background-radius: 8px; -fx-padding: 4px 8px;";
        }
        return "-fx-background-color: #EEF1F5; -fx-text-fill: #657080; -fx-background-radius: 8px; -fx-padding: 4px 8px;";
    }

    private String normalizeAppreciation(String appreciation) {
        if (appreciation == null || appreciation.isBlank() || "Aucune note".equalsIgnoreCase(appreciation.trim())) {
            return "";
        }
        return Normalizer.normalize(appreciation, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("Ã¨", "e")
                .replace("Ã©", "e")
                .replace("Ã€", "A")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static final class BulletinLine {
        private final String field;
        private final String value;

        public BulletinLine(String field, String value) {
            this.field = field;
            this.value = value;
        }

        public String getField() {
            return field;
        }

        public String getValue() {
            return value;
        }
    }
}
