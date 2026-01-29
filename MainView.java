
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * The core view class mirroring Microsoft Word features and layout.
 * Fully implements Edit, Insert, and View menu logic.
 */
public class MainView extends BorderPane {

    private final TextArea editor = new TextArea();
    private final WebView preview = new WebView();
    private final ListView<Note> noteList = new ListView<>();
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private final SplitPane workspace = new SplitPane();
    
    // Status Bar Components
    private final HBox statusBar = new HBox(30);
    private final Label pageLabel = new Label("Page 1 of 1");
    private final Label wordLabel = new Label("0 Words");
    private final Label statusLabel = new Label("Ready");

    // File Tracking
    private File activeSystemFile = null;

    public MainView() {
        setupLayout();
        initializeFirstNote();
        loadPreviewEngine();
    }

    private void loadPreviewEngine() {
        preview.getEngine().loadContent(
            "<html><head>" +
            "<script src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'></script>" +
            "<style>body { font-family: 'Segoe UI', sans-serif; padding: 40px; line-height: 1.6; color: #334155; background: white; } " +
            "h1 { color: #1e293b; border-bottom: 2px solid #f1f5f9; padding-bottom: 10px; margin-top: 0; } " +
            "code { background: #f1f5f9; padding: 2px 5px; border-radius: 4px; font-family: 'Consolas', monospace; color: #4f46e5; } " +
            "pre { background: #1e293b; color: #f8fafc; padding: 20px; border-radius: 8px; overflow-x: auto; } " +
            "table { border-collapse: collapse; width: 100%; margin-bottom: 1rem; } " +
            "th, td { border: 1px solid #e2e8f0; padding: 8px; text-align: left; } " +
            "blockquote { border-left: 5px solid #6366f1; padding-left: 20px; color: #64748b; font-style: italic; background: #f8fafc; padding: 10px 20px; border-radius: 0 8px 8px 0; }</style>" +
            "</head><body><div id='content'></div></body></html>"
        );
    }

    private void setupLayout() {
        // --- 1. TOP SECTION: Menu Bar ---
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: #f8fafc; -fx-padding: 2;");

        // FILE MENU
        Menu fileMenu = new Menu("File");
        MenuItem newItem = new MenuItem("New Document");
        newItem.setAccelerator(KeyCombination.valueOf("Shortcut+N"));
        newItem.setOnAction(e -> createNewNote());
        
        MenuItem openItem = new MenuItem("Open...");
        openItem.setAccelerator(KeyCombination.valueOf("Shortcut+O"));
        openItem.setOnAction(e -> openSystemFile());
        
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(KeyCombination.valueOf("Shortcut+S"));
        saveItem.setOnAction(e -> saveFile());

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setAccelerator(KeyCombination.valueOf("Shortcut+Shift+S"));
        saveAsItem.setOnAction(e -> saveFileAs());

        MenuItem printItem = new MenuItem("Print...");
        printItem.setAccelerator(KeyCombination.valueOf("Shortcut+P"));
        printItem.setOnAction(e -> Platform.runLater(() -> preview.getEngine().print(null)));
        
        fileMenu.getItems().addAll(newItem, openItem, new SeparatorMenuItem(), saveItem, saveAsItem, new SeparatorMenuItem(), printItem);

        // EDIT MENU
        Menu editMenu = new Menu("Edit");
        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setAccelerator(KeyCombination.valueOf("Shortcut+Z"));
        undoItem.setOnAction(e -> editor.undo());

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(KeyCombination.valueOf("Shortcut+X"));
        cutItem.setOnAction(e -> editor.cut());

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(KeyCombination.valueOf("Shortcut+C"));
        copyItem.setOnAction(e -> editor.copy());

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(KeyCombination.valueOf("Shortcut+V"));
        pasteItem.setOnAction(e -> editor.paste());

        MenuItem smartDefine = new MenuItem("Smart Define (AI)");
        smartDefine.setAccelerator(KeyCombination.valueOf("Shortcut+E"));
        smartDefine.setOnAction(e -> handleSmartDefine());

        editMenu.getItems().addAll(undoItem, new SeparatorMenuItem(), cutItem, copyItem, pasteItem, new SeparatorMenuItem(), smartDefine);

        // INSERT MENU
        Menu insertMenu = new Menu("Insert");
        MenuItem tableItem = new MenuItem("Table (3x2)");
        tableItem.setOnAction(e -> applyFormatting("\n| Header 1 | Header 2 | Header 3 |\n| --- | --- | --- |\n| Cell | Cell | Cell |\n| Cell | Cell | Cell |\n", ""));
        
        MenuItem imageItem = new MenuItem("Picture from PC...");
        imageItem.setOnAction(e -> insertLocalPicture());

        MenuItem linkItem = new MenuItem("Hyperlink...");
        linkItem.setAccelerator(KeyCombination.valueOf("Shortcut+K"));
        linkItem.setOnAction(e -> applyFormatting("[Link Text](", "http://)"));
        
        MenuItem checkItem = new MenuItem("Checklist Task");
        checkItem.setOnAction(e -> applyFormatting("\n- [ ] ", ""));

        MenuItem dateItem = new MenuItem("Date & Time");
        dateItem.setOnAction(e -> {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            applyFormatting(now, "");
        });

        insertMenu.getItems().addAll(tableItem, imageItem, new SeparatorMenuItem(), linkItem, checkItem, dateItem);

        // VIEW MENU
        Menu viewMenu = new Menu("View");
        CheckMenuItem wrapItem = new CheckMenuItem("Word Wrap");
        wrapItem.setSelected(true);
        wrapItem.setOnAction(e -> editor.setWrapText(wrapItem.isSelected()));

        Menu layoutMenu = new Menu("Layout Mode");
        MenuItem splitMode = new MenuItem("Split Workspace");
        splitMode.setOnAction(e -> {
            workspace.getItems().setAll(editor, preview);
            workspace.setDividerPositions(0.5);
        });
        MenuItem editorMode = new MenuItem("Full Editor Only");
        editorMode.setOnAction(e -> workspace.getItems().setAll(editor));
        MenuItem readerMode = new MenuItem("Full Preview Only");
        readerMode.setOnAction(e -> workspace.getItems().setAll(preview));
        layoutMenu.getItems().addAll(splitMode, editorMode, readerMode);

        viewMenu.getItems().addAll(wrapItem, new SeparatorMenuItem(), layoutMenu);

        menuBar.getMenus().addAll(fileMenu, editMenu, insertMenu, viewMenu);

        // RIBBON
        ToolBar ribbon = new ToolBar();
        ribbon.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        Button saveBtn = new Button("💾 Save");
        Button boldBtn = new Button("B"); boldBtn.setStyle("-fx-font-weight: bold;");
        Button italicBtn = new Button("I"); italicBtn.setStyle("-fx-font-style: italic;");
        ribbon.getItems().addAll(saveBtn, new Separator(), boldBtn, italicBtn);

        setTop(new VBox(menuBar, ribbon));

        // SIDEBAR
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 0 1 0 0;");
        Label sidebarTitle = new Label("STUDIO EXPLORER");
        sidebarTitle.setStyle("-fx-font-weight: 800; -fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        
        noteList.setItems(notes);
        noteList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                editor.setText(newVal.getContent());
                activeSystemFile = null;
                updateWordCount(newVal.getContent());
            }
        });
        sidebar.getChildren().addAll(sidebarTitle, noteList);
        setLeft(sidebar);

        // WORKSPACE
        editor.setWrapText(true);
        editor.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 15px; -fx-padding: 30;");
        editor.textProperty().addListener((obs, old, newVal) -> {
            updateWordCount(newVal);
            Note current = noteList.getSelectionModel().getSelectedItem();
            if (current != null) current.setContent(newVal);
            updatePreview(newVal);
        });

        workspace.getItems().addAll(editor, preview);
        workspace.setDividerPositions(0.5);
        setCenter(workspace);

        // STATUS BAR
        statusBar.setPadding(new Insets(5, 20, 5, 20));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #2b579a;"); 
        String statusStyle = "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;";
        pageLabel.setStyle(statusStyle);
        wordLabel.setStyle(statusStyle);
        statusLabel.setStyle(statusStyle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusBar.getChildren().addAll(pageLabel, wordLabel, spacer, statusLabel);
        setBottom(statusBar);

        saveBtn.setOnAction(e -> saveFile());
        boldBtn.setOnAction(e -> applyFormatting("**", "**"));
        italicBtn.setOnAction(e -> applyFormatting("*", "*"));
    }

    private void insertLocalPicture() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Insert Picture");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = chooser.showOpenDialog(getScene().getWindow());
        if (selectedFile != null) {
            String path = selectedFile.toURI().toString();
            applyFormatting("![Image](" + path + ")", "");
        }
    }

    private void saveFile() {
        if (activeSystemFile != null) {
            saveToFile(activeSystemFile);
        } else {
            saveFileAs();
        }
    }

    private void saveFileAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Document As");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown Document", "*.md"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            activeSystemFile = file;
            saveToFile(file);
        }
    }

    private void saveToFile(File file) {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.print(editor.getText());
            statusLabel.setText("Document Saved: " + file.getName());
            // Update the note title in the sidebar if it matches
            Note current = noteList.getSelectionModel().getSelectedItem();
            if (current != null) current.setTitle(file.getName());
            noteList.refresh();
        } catch (IOException e) {
            statusLabel.setText("Error: Save Failed");
        }
    }

    private void openSystemFile() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                Note n = new Note(file.getName(), content);
                notes.add(0, n);
                noteList.getSelectionModel().select(n);
                activeSystemFile = file;
                statusLabel.setText("File Loaded: " + file.getName());
            } catch (IOException e) {
                statusLabel.setText("Error: Load Failed");
            }
        }
    }

    private void handleSmartDefine() {
        String term = editor.getSelectedText().isEmpty() ? "selected term" : editor.getSelectedText();
        statusLabel.setText("AI Thinking: Defining '" + term + "'...");
        // Mocking AI response
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("AI Insights");
            alert.setHeaderText("Smart Definition for: " + term);
            alert.setContentText("This feature uses Gemini to provide professional context. (API Integration required)");
            alert.showAndWait();
            statusLabel.setText("Ready");
        });
    }

    private void updateWordCount(String text) {
        int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        wordLabel.setText(words + " Words");
        pageLabel.setText("Page " + (Math.max(1, (int) Math.ceil(words / 300.0))) + " of 1");
    }

    private void updatePreview(String content) {
        String escaped = content.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$").replace("'", "\\'");
        Platform.runLater(() -> {
            try {
                preview.getEngine().executeScript(
                    "document.getElementById('content').innerHTML = marked.parse(`" + escaped + "`);"
                );
            } catch (Exception e) {}
        });
    }

    private void initializeFirstNote() {
        Note initial = new Note("Document 1", "# Welcome\n\nThis is your professional AI writing environment.");
        notes.add(initial);
        noteList.getSelectionModel().select(initial);
    }

    private void createNewNote() {
        Note n = new Note("Document " + (notes.size() + 1), "");
        notes.add(0, n);
        noteList.getSelectionModel().select(n);
        activeSystemFile = null;
    }

    private void applyFormatting(String pre, String post) {
        String selected = editor.getSelectedText();
        IndexRange range = editor.getSelection();
        editor.replaceText(range, pre + selected + post);
        editor.requestFocus();
    }
}
