package utils;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Gestion simple du theme (clair/sombre) basee sur une classe CSS appliquee a la racine.
 *
 * Le theme est defini par la presence de la classe CSS "theme-dark" sur le root de la Scene / DialogPane.
 */
public final class ThemeManager {
    private static final String DARK_CLASS = "theme-dark";
    private static final String PREF_DARK = "theme.dark";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);

    private ThemeManager() {
    }

    public static boolean isDarkPreferred() {
        return PREFS.getBoolean(PREF_DARK, false);
    }

    public static void setDarkPreferred(boolean dark) {
        PREFS.putBoolean(PREF_DARK, dark);
    }

    public static boolean isDark(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return false;
        }
        return scene.getRoot().getStyleClass().contains(DARK_CLASS);
    }

    public static void setDark(Scene scene, boolean dark) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        if (dark) {
            if (!scene.getRoot().getStyleClass().contains(DARK_CLASS)) {
                scene.getRoot().getStyleClass().add(DARK_CLASS);
            }
        } else {
            scene.getRoot().getStyleClass().remove(DARK_CLASS);
        }
        setDarkPreferred(dark);
    }

    public static void apply(Scene scene) {
        setDark(scene, isDarkPreferred());
    }

    public static void apply(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }

        // Assurer les stylesheets sur les dialogues (Alert, ChoiceDialog, etc.).
        String styles = Objects.requireNonNull(
                ThemeManager.class.getResource("/css/styles.css"),
                "styles.css introuvable"
        ).toExternalForm();
        String colors = Objects.requireNonNull(
                ThemeManager.class.getResource("/css/colors.css"),
                "colors.css introuvable"
        ).toExternalForm();

        if (!dialogPane.getStylesheets().contains(styles)) {
            dialogPane.getStylesheets().add(styles);
        }
        if (!dialogPane.getStylesheets().contains(colors)) {
            dialogPane.getStylesheets().add(colors);
        }

        if (isDarkPreferred()) {
            if (!dialogPane.getStyleClass().contains(DARK_CLASS)) {
                dialogPane.getStyleClass().add(DARK_CLASS);
            }
        } else {
            dialogPane.getStyleClass().remove(DARK_CLASS);
        }
    }
}

