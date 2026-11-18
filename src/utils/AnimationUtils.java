package utils;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Utilitaires d'animations visuelles pour rendre l'interface plus dynamique.
 */
public final class AnimationUtils {

    private AnimationUtils() {
    }

    public static void playSceneEntrance(Parent root) {
        if (root == null) {
            return;
        }

        root.setOpacity(0);
        FadeTransition rootFade = new FadeTransition(Duration.millis(280), root);
        rootFade.setFromValue(0);
        rootFade.setToValue(1);
        rootFade.setInterpolator(Interpolator.EASE_BOTH);
        rootFade.play();

        playCardCascade(root);
        enhanceButtonInteractions(root);
    }

    public static void playCardCascade(Parent root) {
        Set<Node> candidates = root.lookupAll(".dashboard-card, .dashboard-chart, .stat-box, .login-box");
        if (candidates.isEmpty()) {
            return;
        }

        List<Node> cards = new ArrayList<>(candidates);
        cards.sort(Comparator.comparingDouble(Node::getLayoutY));

        for (int i = 0; i < cards.size(); i++) {
            Node node = cards.get(i);
            node.setOpacity(0);
            node.setTranslateY(16);

            FadeTransition fade = new FadeTransition(Duration.millis(260), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(320), node);
            slide.setFromY(16);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            slide.setDelay(Duration.millis(i * 40L));

            SequentialTransition sequence = new SequentialTransition();
            sequence.getChildren().add(fade);
            sequence.getChildren().add(slide);
            sequence.play();
        }
    }

    public static void enhanceButtonInteractions(Parent root) {
        Set<Node> buttons = root.lookupAll(".button, .sidebar-item");
        for (Node node : buttons) {
            if (!(node instanceof Button button)) {
                continue;
            }

            button.setOnMouseEntered(event -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
                st.setToX(1.03);
                st.setToY(1.03);
                st.setInterpolator(Interpolator.EASE_OUT);
                st.play();
            });

            button.setOnMouseExited(event -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
                st.setToX(1.0);
                st.setToY(1.0);
                st.setInterpolator(Interpolator.EASE_BOTH);
                st.play();
            });
        }
    }
}

