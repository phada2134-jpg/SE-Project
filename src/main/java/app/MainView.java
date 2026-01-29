package app;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainView {

    private BorderPane root;
    private TextArea previewArea;
    private TextArea editorArea;

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

        MenuItem newItem = new MenuItem("New");
        MenuItem openItem = new MenuItem("Open");
        MenuItem saveItem = new MenuItem("Save");
        MenuItem exportHTML = new MenuItem("Export as HTML");
        MenuItem exportPDF = new MenuItem("Export as PDF");

        // WEEK 4: Actions
        newItem.setOnAction(e -> editorArea.clear());
        openItem.setOnAction(e -> editorArea.setText(FileManager.loadFromFile((Stage) root.getScene().getWindow())));
        saveItem.setOnAction(e -> FileManager.saveToFile((Stage) root.getScene().getWindow(), editorArea.getText()));
        exportHTML.setOnAction(e -> FileManager.exportHTML(editorArea.getText(), (Stage) root.getScene().getWindow()));
        exportPDF.setOnAction(e -> FileManager.exportPDF(editorArea.getText(), (Stage) root.getScene().getWindow()));

        fileMenu.getItems().addAll(newItem, openItem, saveItem, exportHTML, exportPDF);
        return new MenuBar(fileMenu);
    }

    private SplitPane createCenterPane() {
        SplitPane splitPane = new SplitPane();

        editorArea = new TextArea();
        editorArea.setPromptText("Write Markdown here...");

        previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setPromptText("Preview will appear here...");

        // WEEK 3: live preview
        editorArea.textProperty().addListener((obs, oldText, newText) -> {
            String parsed = MarkdownParser.parse(newText);
            previewArea.setText(parsed);
        });

        splitPane.getItems().addAll(editorArea, previewArea);
        splitPane.setDividerPositions(0.5);

        return splitPane;
    }
}
