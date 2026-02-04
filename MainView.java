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
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.layout.GridPane;
import javafx.geometry.HPos;

public class MainView extends BorderPane {

    private final TextArea editor = new TextArea();
    private final WebView preview = new WebView();
    private final ListView<Note> noteList = new ListView<>();
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private final SplitPane workspace = new SplitPane();

    private final HBox statusBar = new HBox(30);
    private final Label pageLabel = new Label("Page 1 of 1");
    private final Label wordLabel = new Label("0 Words");
    private final Label statusLabel = new Label("Ready");

    private File activeSystemFile = null;
    private String currentUser = "guest";
    private RecentFiles recentFiles;
    private Menu recentMenu;

    public MainView() {
        setupLayout();
        initializeFirstNote();
        loadPreviewEngine();
        recentFiles = new RecentFiles(currentUser);
        updateRecentFilesMenu();
    }

    private void loadPreviewEngine() {
        preview.getEngine().loadContent(
            "<html><head>" +
            "<script src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'></script>" +
            "<style>body{font-family:'Segoe UI',sans-serif;padding:40px;line-height:1.6;color:#334155;}h1{color:#1e293b;border-bottom:2px solid #f1f5f9;padding-bottom:10px;}code{background:#f1f5f9;padding:2px 5px;border-radius:4px;font-family:Consolas,monospace;color:#4f46e5;}pre{background:#1e293b;color:#f8fafc;padding:20px;border-radius:8px;}table{border-collapse:collapse;width:100%;}th,td{border:1px solid #e2e8f0;padding:8px;}blockquote{border-left:5px solid #6366f1;padding-left:20px;color:#64748b;background:#f8fafc;}</style>" +
            "</head><body><div id='content'></div></body></html>"
        );
    }

    private void setupLayout() {
        MenuBar menuBar = new MenuBar();

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

        recentMenu = new Menu("Recent Files");

        Menu exportMenu = new Menu("Export");
        MenuItem exportPdf = new MenuItem("Export as PDF");
        exportPdf.setOnAction(e -> exportAsPdf());
        MenuItem exportHtmlPdf = new MenuItem("HTML → PDF...");
        exportHtmlPdf.setOnAction(e -> exportHtmlToPdf());
        exportMenu.getItems().addAll(exportPdf, exportHtmlPdf);

        fileMenu.getItems().addAll(
            newItem, openItem, new SeparatorMenuItem(),
            saveItem, saveAsItem, new SeparatorMenuItem(),
            exportMenu, recentMenu
        );

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

        editMenu.getItems().addAll(
            undoItem, new SeparatorMenuItem(),
            cutItem, copyItem, pasteItem,
            new SeparatorMenuItem(), smartDefine
        );

        Menu insertMenu = new Menu("Insert");
        MenuItem tableItem = new MenuItem("Insert Table...");
        tableItem.setOnAction(e -> showInsertTableDialog());

        MenuItem imageItem = new MenuItem("Picture from PC...");
        imageItem.setOnAction(e -> insertLocalPicture());

        MenuItem linkItem = new MenuItem("Hyperlink...");
        linkItem.setAccelerator(KeyCombination.valueOf("Shortcut+K"));
        linkItem.setOnAction(e -> applyFormatting("[Link Text](", "http://)"));

        MenuItem checkItem = new MenuItem("Checklist Task");
        checkItem.setOnAction(e -> applyFormatting("\n- [ ] ", ""));

        MenuItem dateItem = new MenuItem("Date & Time");
        dateItem.setOnAction(e ->
            applyFormatting(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), "")
        );

        insertMenu.getItems().addAll(
            tableItem, imageItem, new SeparatorMenuItem(),
            linkItem, checkItem, dateItem
        );

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

        setTop(menuBar);
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
        IndexRange range = editor.getSelection();
        editor.replaceText(range, pre + editor.getSelectedText() + post);
        editor.requestFocus();
    }

    private void handleSmartDefine() {
        String term = editor.getSelectedText().isEmpty() ? "selected term" : editor.getSelectedText();
        statusLabel.setText("AI Thinking: Defining '" + term + "'...");
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("AI Insights");
            alert.setHeaderText("Smart Definition for: " + term);
            alert.setContentText("This feature requires API integration.");
            alert.showAndWait();
            statusLabel.setText("Ready");
        });
    }
}
