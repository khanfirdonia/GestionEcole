package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import utils.AlertUtils;
import utils.ValidationUtils;
import utils.Constants;
import utils.AnimationUtils;
import utils.ThemeManager;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.ToggleButton;

/**
 * Contrôleur pour la page de connexion
 */
public class LoginController {
    private static final Logger logger = Logger.getLogger(LoginController.class.getName());
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Button exitButton;

    @FXML
    private ToggleButton themeToggle;
    
    private UserService userService = new UserService();

    /**
     * Initialiser le contrôleur
     */
    @FXML
    public void initialize() {
        logger.info("Initialisation de la page de connexion");
        
        // Ajouter des actions aux champs
        passwordField.setOnAction(event -> handleLogin());
        usernameField.setOnAction(event -> passwordField.requestFocus());
        setupDynamicEffects();
        setupThemeToggle();
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

    private void setupDynamicEffects() {
        loginButton.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            AnimationUtils.playSceneEntrance(newScene.getRoot());
        });
    }

    /**
     * Gérer la connexion
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Valider les entrées
        if (!ValidationUtils.isNotEmpty(username)) {
            AlertUtils.showError("Erreur de validation", "Veuillez entrer votre nom d'utilisateur");
            usernameField.requestFocus();
            return;
        }

        if (!ValidationUtils.isNotEmpty(password)) {
            AlertUtils.showError("Erreur de validation", "Veuillez entrer votre mot de passe");
            passwordField.requestFocus();
            return;
        }

        // Authentifier l'utilisateur
        try {
            User user = userService.authenticate(username, password);
            if (user != null) {
                logger.info("Utilisateur authentifié: " + username);
                navigateToDashboard(user);
            } else {
                AlertUtils.showError("Erreur d'authentification", 
                    "Nom d'utilisateur ou mot de passe incorrect");
                passwordField.clear();
                passwordField.requestFocus();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors de l'authentification", e);
            AlertUtils.showError("Erreur", "Une erreur s'est produite lors de la connexion");
        }
    }

    /**
     * Naviguer vers le dashboard approprié selon le rôle
     */
    private void navigateToDashboard(User user) {
        try {
            String fxmlFile;
            String windowTitle;

            switch (user.getRole()) {
                case Constants.ROLE_ADMIN:
                    fxmlFile = "/views/AdminDashboard.fxml";
                    windowTitle = "Dashboard Admin - " + Constants.APP_NAME;
                    break;
                case Constants.ROLE_TEACHER:
                    fxmlFile = "/views/TeacherDashboard.fxml";
                    windowTitle = "Dashboard Enseignant - " + Constants.APP_NAME;
                    break;
                case Constants.ROLE_STUDENT:
                    fxmlFile = "/views/StudentDashboard.fxml";
                    windowTitle = "Dashboard Élève - " + Constants.APP_NAME;
                    break;
                default:
                    AlertUtils.showError("Erreur", "Rôle utilisateur inconnu");
                    return;
            }

            // Charger la vue du dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Passer l'utilisateur au contrôleur du dashboard
            Object controller = loader.getController();
            if (controller instanceof DashboardController) {
                ((DashboardController) controller).setUser(user);
            }

            // Créer une nouvelle scène
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            
            // Ajouter les feuilles de style
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/colors.css").toExternalForm());
            ThemeManager.apply(scene);
            
            stage.setScene(scene);
            stage.setTitle(windowTitle);
            stage.setResizable(true);
            stage.setMinWidth(1024);
            stage.setMinHeight(680);
            stage.setWidth(1280);
            stage.setHeight(820);
            stage.setMaximized(false);
            stage.centerOnScreen();
            stage.show();
            AnimationUtils.playSceneEntrance(root);

            logger.info("Navigation vers le dashboard réussie pour l'utilisateur: " + user.getUsername());

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement du dashboard", e);
            AlertUtils.showError("Erreur", "Erreur lors du chargement de l'interface");
        }
    }

    /**
     * Gérer la fermeture de l'application
     */
    @FXML
    private void handleExit() {
        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Aller vers l'ecran de pre-inscription (sans compte).
     */
    @FXML
    private void handlePreEnrollment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PreEnrollment.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/colors.css").toExternalForm());
            ThemeManager.apply(scene);

            stage.setScene(scene);
            stage.setTitle("Pre-inscription - " + Constants.APP_NAME);
            stage.show();
            AnimationUtils.playSceneEntrance(root);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors du chargement de l'ecran pre-inscription", e);
            AlertUtils.showError("Erreur", "Impossible de charger l'ecran de pre-inscription.");
        }
    }
}
