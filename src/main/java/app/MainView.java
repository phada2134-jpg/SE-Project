package app;

import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainView {

    private BorderPane root;
    
    public MainView() {
        root = new BorderPane();
        root.setTop(createMenuBar());
        root.setCenter(createCenterPane());
    }

    public BorderPane getView() {
        return root;
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(
                new MenuItem("New"),
                new MenuItem("Open"),
                new MenuItem("Save")
        );

        return new MenuBar(fileMenu);
    }

private SplitPane createCenterPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5);
        return splitPane;
    }
}

}
