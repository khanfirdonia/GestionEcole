package utils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.TableView;

/**
 * Helpers TableView pour avoir un rendu "sans lignes vides" (hauteur ajuste au contenu).
 *
 * Note: JavaFX TableView affiche naturellement un espace vide sous les dernieres lignes
 * si sa hauteur est plus grande que le contenu. Ici on ajuste la hauteur a N lignes max.
 */
public final class TableViewUtils {

    private static final double DEFAULT_ROW_HEIGHT = 34.0;
    private static final double DEFAULT_HEADER_HEIGHT = 28.0;
    private static final double EXTRA_PADDING = 2.0;

    private TableViewUtils() {
    }

    public static void bindAutoFitHeight(TableView<?> tableView, int maxVisibleRows) {
        if (tableView == null) {
            return;
        }

        int safeMaxRows = Math.max(1, maxVisibleRows);
        double rowHeight = tableView.getFixedCellSize() > 0 ? tableView.getFixedCellSize() : DEFAULT_ROW_HEIGHT;
        tableView.setFixedCellSize(rowHeight);

        DoubleProperty headerHeight = new SimpleDoubleProperty(DEFAULT_HEADER_HEIGHT);
        Runnable updateHeaderHeight = () -> {
            Node header = tableView.lookup(".column-header-background");
            if (header != null) {
                double h = header.prefHeight(-1);
                if (h > 0) {
                    headerHeight.set(h);
                }
            }
        };

        // Au premier layout, le header n'est pas toujours present: re-tenter.
        tableView.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(updateHeaderHeight));
        tableView.sceneProperty().addListener((obs, oldScene, newScene) -> Platform.runLater(updateHeaderHeight));
        tableView.widthProperty().addListener((obs, oldValue, newValue) -> Platform.runLater(updateHeaderHeight));
        tableView.getItems().addListener((javafx.collections.ListChangeListener) change -> Platform.runLater(updateHeaderHeight));

        IntegerBinding itemsCount = Bindings.size(tableView.getItems());
        IntegerBinding visibleRows = Bindings.createIntegerBinding(
                () -> Math.min(itemsCount.get(), safeMaxRows),
                itemsCount
        );

        javafx.beans.binding.DoubleBinding heightBinding = Bindings.createDoubleBinding(
                () -> headerHeight.get() + (visibleRows.get() * rowHeight) + EXTRA_PADDING,
                headerHeight,
                visibleRows
        );

        tableView.prefHeightProperty().bind(heightBinding);
        tableView.minHeightProperty().bind(heightBinding);
        tableView.maxHeightProperty().bind(heightBinding);
    }
}
