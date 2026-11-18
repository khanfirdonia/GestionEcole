package controllers;

import dao.LevelDAO;
import dao.SchoolYearDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import models.Level;
import models.SchoolYear;
import services.PreEnrollmentService;
import utils.AlertUtils;
import utils.AnimationUtils;
import utils.ThemeManager;
import utils.ValidationUtils;
import utils.DateUtils;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Ecran de pre-inscription (sans compte).
 */
public class PreEnrollmentController {
    private static final Logger logger = Logger.getLogger(PreEnrollmentController.class.getName());

    @FXML
    private ToggleButton themeToggle;

    @FXML
    private Label activeYearLabel;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private DatePicker birthDatePicker;

    @FXML
    private TextField addressField;

    @FXML
    private TextField parentNameField;

    @FXML
    private TextField parentPhoneField;

    @FXML
    private TextField parentEmailField;

    @FXML
    private ComboBox<Level> requestedLevelCombo;

    @FXML
    private Label feedbackLabel;

    private final LevelDAO levelDAO = new LevelDAO();
    private final SchoolYearDAO schoolYearDAO = new SchoolYearDAO();
    private final PreEnrollmentService preEnrollmentService = new PreEnrollmentService();

    @FXML
    public void initialize() {
        setupThemeToggle();
        populateReferenceData();
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

    private void populateReferenceData() {
        try {
            SchoolYear active = schoolYearDAO.getActiveSchoolYear();
            if (activeYearLabel != null) {
                activeYearLabel.setText(active != null ? active.getLabel() : "N/A");
            }

            List<Level> levels = levelDAO.getAllLevels();
            if (requestedLevelCombo != null) {
                requestedLevelCombo.getItems().setAll(levels);
                if (!levels.isEmpty()) {
                    requestedLevelCombo.getSelectionModel().selectFirst();
                }
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "Erreur chargement reference pre-inscription", e);
        }
    }

    @FXML
    private void handleSubmit() {
        String firstName = firstNameField != null ? firstNameField.getText().trim() : "";
        String lastName = lastNameField != null ? lastNameField.getText().trim() : "";
        java.time.LocalDate birthDateValue = birthDatePicker != null ? birthDatePicker.getValue() : null;
        String birthDate = birthDateValue != null ? DateUtils.formatDate(birthDateValue) : "";
        String address = addressField != null ? addressField.getText().trim() : null;
        String parentName = parentNameField != null ? parentNameField.getText().trim() : null;
        String parentPhone = parentPhoneField != null ? parentPhoneField.getText().trim() : null;
        String parentEmail = parentEmailField != null ? parentEmailField.getText().trim() : null;

        Level requestedLevel = requestedLevelCombo != null ? requestedLevelCombo.getSelectionModel().getSelectedItem() : null;
        if (!ValidationUtils.isValidFullName(firstName, lastName)) {
            AlertUtils.showError("Pre-inscription", "Nom et prenom obligatoires (min 2 caracteres).");
            return;
        }
        if (requestedLevel == null) {
            AlertUtils.showError("Pre-inscription", "Selectionnez un niveau.");
            return;
        }
        // DatePicker garantit une date valide. (Optionnelle)
        if (parentPhone != null && !parentPhone.isBlank() && !ValidationUtils.isValidPhone(parentPhone)) {
            AlertUtils.showError("Pre-inscription", "Telephone parent invalide (8 a 15 chiffres).");
            return;
        }
        if (parentEmail != null && !parentEmail.isBlank() && !ValidationUtils.isValidEmail(parentEmail)) {
            AlertUtils.showError("Pre-inscription", "Email parent invalide.");
            return;
        }

        try {
            int id = preEnrollmentService.createPreEnrollment(
                    requestedLevel.getId(),
                    firstName,
                    lastName,
                    birthDate.isEmpty() ? null : birthDate,
                    address,
                    parentName,
                    parentPhone,
                    parentEmail
            );

            if (id > 0) {
                if (feedbackLabel != null) {
                    feedbackLabel.setText("Demande envoyee (ID=" + id + ").");
                }
                AlertUtils.showSuccess("Pre-inscription", "Votre demande a ete envoyee. Attendez la validation.");
                handleBack();
            } else {
                AlertUtils.showError("Pre-inscription", "Impossible d'envoyer la demande (doublon possible).");
            }
        } catch (IllegalStateException e) {
            AlertUtils.showError("Pre-inscription", e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) (activeYearLabel != null ? activeYearLabel.getScene().getWindow() : null);
            if (stage == null) {
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/colors.css").toExternalForm());
            ThemeManager.apply(scene);

            stage.setScene(scene);
            stage.setTitle("Gestion Scolaire - Authentification");
            stage.show();
            AnimationUtils.playSceneEntrance(root);
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Erreur retour login depuis pre-inscription", e);
        }
    }
}
