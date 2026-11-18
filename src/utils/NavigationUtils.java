package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Utilitaires de navigation entre les vues JavaFX.
 */
public final class NavigationUtils {

    private NavigationUtils() {
    }

    public static void showLoginScreen(Stage stage, Class<?> resourceClass) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                resourceClass.getResource("/views/Login.fxml"),
                "Vue de connexion introuvable"
        ));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(
                resourceClass.getResource("/css/styles.css"),
                "Feuille de style styles.css introuvable"
        ).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(
                resourceClass.getResource("/css/colors.css"),
                "Feuille de style colors.css introuvable"
        ).toExternalForm());
        ThemeManager.apply(scene);

        stage.setScene(scene);
        stage.setTitle("Gestion Scolaire - Authentification");
        stage.setResizable(true);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setWidth(1280);
        stage.setHeight(820);
        stage.setMaximized(false);
        stage.centerOnScreen();
        stage.show();
        AnimationUtils.playSceneEntrance(root);
    }
}
