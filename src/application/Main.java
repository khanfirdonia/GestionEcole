package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.ThemeManager;

/**
 * Classe principale de l'application JavaFX
 * Point d'entrée de l'application Gestion Scolaire
 */
public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Démarrage de l'application Gestion Scolaire");

            // Charger le fichier FXML de la page de connexion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();

            // Créer la scène
            Scene scene = new Scene(root);

            // Ajouter les feuilles de style CSS
            String styleSheet = getClass().getResource("/css/styles.css").toExternalForm();
            String colorSheet = getClass().getResource("/css/colors.css").toExternalForm();
            scene.getStylesheets().add(styleSheet);
            scene.getStylesheets().add(colorSheet);
            ThemeManager.apply(scene);

            // Configurer la fenêtre principale (non maximisee par defaut)
            primaryStage.setTitle("Gestion Scolaire - Authentification");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(680);
            primaryStage.setWidth(1280);
            primaryStage.setHeight(820);
            primaryStage.setMaximized(false);
            primaryStage.centerOnScreen();

            // Afficher la fenêtre
            primaryStage.show();

            logger.info("Application démarrée avec succès");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erreur lors du démarrage de l'application", e);
            System.err.println("Erreur fatale: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
