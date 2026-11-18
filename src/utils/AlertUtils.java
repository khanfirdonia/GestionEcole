package utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import java.util.Optional;

/**
 * Classe utilitaire pour les alertes et dialogues
 */
public class AlertUtils {

    /**
     * Afficher une alerte d'information
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.apply(alert.getDialogPane());
        alert.showAndWait();
    }

    /**
     * Afficher une alerte d'erreur
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.apply(alert.getDialogPane());
        alert.showAndWait();
    }

    /**
     * Afficher une alerte d'avertissement
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.apply(alert.getDialogPane());
        alert.showAndWait();
    }

    /**
     * Afficher une alerte de confirmation
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.apply(alert.getDialogPane());
        
        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
    }

    /**
     * Afficher un dialogue de saisie de texte
     */
    public static Optional<String> showInputDialog(String title, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        ThemeManager.apply(dialog.getDialogPane());
        
        return dialog.showAndWait();
    }

    /**
     * Afficher une alerte de succès
     */
    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Succès");
        alert.setContentText(message);
        ThemeManager.apply(alert.getDialogPane());
        alert.showAndWait();
    }

    /**
     * Afficher une alerte personnalisée
     */
    public static void showAlert(AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        ThemeManager.apply(alert.getDialogPane());
        alert.showAndWait();
    }
}
