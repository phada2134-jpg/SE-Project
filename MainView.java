
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
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.HPos;

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
    private String currentUser = "guest";
    private RecentFiles recentFiles;
    private Menu recentMenu;

    public MainView() {
        setupLayout();
        initializeFirstNote();
        loadPreviewEngine();
        // Initialize recent files manager for the default user
        recentFiles = new RecentFiles(currentUser);
        updateRecentFilesMenu();
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

        // MenuItem printItem = new MenuItem("Print...");
        // printItem.setAccelerator(KeyCombination.valueOf("Shortcut+P"));
        // printItem.setOnAction(e -> Platform.runLater(() -> preview.getEngine().print(null))); // Print disabled per request
        
        // Recent files submenu and account/login
        recentMenu = new Menu("Recent Files");
        // MenuItem loginItem = new MenuItem("Login...");
        // loginItem.setOnAction(e -> promptLogin()); // Login disabled per request

        // Export submenu
        Menu exportMenu = new Menu("Export");
        MenuItem exportPdf = new MenuItem("Export as PDF");
        exportPdf.setOnAction(e -> exportAsPdf());
        MenuItem exportHtmlPdf = new MenuItem("HTML → PDF...");
        exportHtmlPdf.setOnAction(e -> exportHtmlToPdf());
        exportMenu.getItems().addAll(exportPdf, exportHtmlPdf);

        // Print and Login menu entries removed/disabled per user request
        fileMenu.getItems().addAll(newItem, openItem, new SeparatorMenuItem(), saveItem, saveAsItem, new SeparatorMenuItem(), exportMenu, recentMenu);

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
        Button saveBtn = new Button("Save");
        Button boldBtn = new Button("B"); boldBtn.setStyle("-fx-font-weight: bold;");
        boldBtn.setTooltip(new Tooltip("Bold selected text or insert bold placeholder"));
        Button italicBtn = new Button("I"); italicBtn.setStyle("-fx-font-style: italic;");
        italicBtn.setTooltip(new Tooltip("Italicize selected text or insert italic placeholder"));
        Button lineBtn = new Button("L");
        lineBtn.setTooltip(new Tooltip("Insert horizontal rule (---)"));
        ribbon.getItems().addAll(saveBtn, new Separator(), boldBtn, italicBtn, new Separator(), lineBtn);

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
        boldBtn.setOnAction(e -> applyWrapOrPlaceholder("**", "**", "Bold text"));
        italicBtn.setOnAction(e -> applyWrapOrPlaceholder("*", "*", "Italic text"));
        lineBtn.setOnAction(e -> insertHorizontalRule());
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
            // Track in recent files
            if (recentFiles != null) {
                recentFiles.add(file);
                updateRecentFilesMenu();
            }
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
                // Track in recent files
                if (recentFiles != null) {
                    recentFiles.add(file);
                    updateRecentFilesMenu();
                }
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

    // ----------------------- Recent files, login and export helpers -----------------------
    private void promptLogin() {
        // Login feature has been disabled per user request.
        statusLabel.setText("Login disabled");
        Alert a = new Alert(Alert.AlertType.INFORMATION, "The login feature has been disabled.");
        a.showAndWait();
    }

    private void updateRecentFilesMenu() {
        if (recentMenu == null) return;
        recentMenu.getItems().clear();
        if (recentFiles == null) {
            MenuItem empty = new MenuItem("No recent files");
            empty.setDisable(true);
            recentMenu.getItems().add(empty);
            return;
        }
        List<File> list = recentFiles.getRecentFiles();
        if (list.isEmpty()) {
            MenuItem empty = new MenuItem("No recent files");
            empty.setDisable(true);
            recentMenu.getItems().add(empty);
            return;
        }
        for (File f : list) {
            MenuItem mi = new MenuItem(f.getName());
            mi.setOnAction(e -> openFileFromRecent(f));
            recentMenu.getItems().add(mi);
        }
    }

    private void openFileFromRecent(File f) {
        try {
            String content = Files.readString(f.toPath());
            Note n = new Note(f.getName(), content);
            notes.add(0, n);
            noteList.getSelectionModel().select(n);
            activeSystemFile = f;
            statusLabel.setText("File Loaded: " + f.getName());
            if (recentFiles != null) { recentFiles.add(f); updateRecentFilesMenu(); }
        } catch (IOException e) {
            statusLabel.setText("Error: Load Failed");
        }
    }

    private void exportAsPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export as PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File out = chooser.showSaveDialog(getScene().getWindow());
        if (out == null) return;
        try {
            Object inner = preview.getEngine().executeScript("document.getElementById('content').innerHTML");
            String htmlBody = inner == null ? "" : inner.toString();
            String html = "<html><head><meta charset='utf-8'><style>body{font-family: Segoe UI, Arial; padding:20px;} table{border-collapse:collapse;} th,td{border:1px solid #ddd;padding:6px;}</style></head><body>" + htmlBody + "</body></html>";

            // Prefer direct HTML->PDF conversion using OpenHTMLToPDF when available
            if (hasOpenHtmlToPdf()) {
                boolean ok = convertHtmlToPdfByReflection(html, out);
                if (ok) {
                    if (recentFiles != null) { recentFiles.add(out); updateRecentFilesMenu(); }
                    return;
                }
            }

            // If conversion failed or OpenHTMLToPDF not available, offer user options instead of automatically printing
            Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
            choice.setTitle("Export PDF Options");
            choice.setHeaderText("Unable to export directly to PDF using HTML->PDF library.");
            choice.setContentText("Choose an alternative:");

            ButtonType btnDownload = new ButtonType("Download JARs (recommended)", ButtonBar.ButtonData.LEFT);
            ButtonType btnChoosePrinter = new ButtonType("Choose Printer...");
            ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            choice.getButtonTypes().setAll(btnDownload, btnChoosePrinter, btnCancel);
            Optional<ButtonType> res = choice.showAndWait();
            if (res.isPresent()) {
                if (res.get() == btnDownload) {
                    // Help the user: point to README instructions
                    try {
                        java.awt.Desktop.getDesktop().open(new File("README.md"));
                    } catch (Exception ex) {
                        Alert a = new Alert(Alert.AlertType.INFORMATION, "Please run the included fetch_openhtmltopdf.ps1 script to download required jars into ./lib. See README.md for details.");
                        a.showAndWait();
                    }
                } else if (res.get() == btnChoosePrinter) {
                    printViaSelectedPrinter();
                } else {
                    // cancel
                }
            }


        } catch (Exception e) {
            statusLabel.setText("Error: PDF Export Failed");
            e.printStackTrace();
        }
    }

    private void exportHtmlToPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select HTML File to Convert");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML", "*.html", "*.htm"));
        File htmlFile = chooser.showOpenDialog(getScene().getWindow());
        if (htmlFile == null) return;
        FileChooser outChooser = new FileChooser();
        outChooser.setTitle("Save PDF As");
        outChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File out = outChooser.showSaveDialog(getScene().getWindow());
        if (out == null) return;
        try {
            String html = Files.readString(htmlFile.toPath());
            boolean ok = convertHtmlToPdfByReflection(html, out);
            if (!ok) {
                statusLabel.setText("Error: Missing OpenHTMLToPDF dependency");
            } else {
                statusLabel.setText("Saved PDF: " + out.getName());
                if (recentFiles != null) { recentFiles.add(out); updateRecentFilesMenu(); }
            }
        } catch (IOException e) {
            statusLabel.setText("Error: HTML Read Failed");
        }
    }

    private boolean convertHtmlToPdfByReflection(String html, File pdfFile) {
        try {
            Class<?> builderClass = Class.forName("com.openhtmltopdf.pdfboxout.PdfRendererBuilder");
            Object builder = builderClass.getDeclaredConstructor().newInstance();
            Method withHtml = builderClass.getMethod("withHtmlContent", String.class, String.class);
            Method toStream = builderClass.getMethod("toStream", java.io.OutputStream.class);
            Method run = builderClass.getMethod("run");
            withHtml.invoke(builder, html, null);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            toStream.invoke(builder, fos);
            run.invoke(builder);
            fos.close();
            statusLabel.setText("Saved PDF: " + pdfFile.getName());
            return true;
        } catch (ClassNotFoundException e) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Missing Dependency");
                a.setHeaderText("OpenHTMLToPDF library not found");
                a.setContentText("To enable HTML->PDF export add OpenHTMLToPDF jars (Maven: com.openhtmltopdf:openhtmltopdf-pdfbox and core). Use the included fetch_openhtmltopdf.ps1 or add jars to ./lib and include them on the classpath.");
                a.showAndWait();
            });
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error: PDF Export Failed");
            return false;
        }
    }

    private boolean hasOpenHtmlToPdf() {
        try {
            Class.forName("com.openhtmltopdf.pdfboxout.PdfRendererBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void printViaSelectedPrinter() {
        javafx.print.Printer defaultPrinter = javafx.print.Printer.getDefaultPrinter();
        List<javafx.print.Printer> printers = new ArrayList<>();
        for (javafx.print.Printer p : javafx.print.Printer.getAllPrinters()) printers.add(p);
        if (printers.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.ERROR, "No printers found on the system.");
            a.showAndWait();
            return;
        }
        // Build choice list
        ChoiceDialog<javafx.print.Printer> dlg = new ChoiceDialog<>(defaultPrinter, printers);
        dlg.setTitle("Choose Printer");
        dlg.setHeaderText("Select a printer to print the preview to PDF");
        Optional<javafx.print.Printer> chosen = dlg.showAndWait();
        if (chosen.isPresent()) {
            javafx.print.Printer p = chosen.get();
            if (p.getName().toLowerCase().contains("adobe")) {
                Alert warn = new Alert(Alert.AlertType.CONFIRMATION, "Selected printer looks like Adobe. This may require a valid Adobe subscription. Choose a different printer if possible.", ButtonType.OK, ButtonType.CANCEL);
                Optional<ButtonType> r = warn.showAndWait();
                if (!r.isPresent() || r.get() == ButtonType.CANCEL) return;
            }
            javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob(p);
            if (job != null) {
                boolean success = job.printPage(preview);
                if (success) job.endJob();
            }
        }
    }

    private void showInsertTableDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Insert Table");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Spinner<Integer> rows = new Spinner<>(1, 20, 3);
        Spinner<Integer> cols = new Spinner<>(1, 10, 3);
        grid.add(new Label("Rows:"), 0, 0);
        grid.add(rows, 1, 0);
        grid.add(new Label("Columns:"), 0, 1);
        grid.add(cols, 1, 1);
        GridPane.setHalignment(rows, HPos.LEFT);
        GridPane.setHalignment(cols, HPos.LEFT);
        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            int r = rows.getValue();
            int c = cols.getValue();
            StringBuilder sb = new StringBuilder("\n");
            sb.append("|");
            for (int i=1;i<=c;i++) sb.append(" Header ").append(i).append(" |");
            sb.append("\n|");
            for (int i=1;i<=c;i++) sb.append(" --- |");
            sb.append("\n");
            for (int i=0;i<r;i++) {
                sb.append("|");
                for (int j=0;j<c;j++) sb.append(" Cell |");
                sb.append("\n");
            }
            applyFormatting(sb.toString(), "");
        }
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

    // Wrap selection with markers, toggle them if already present, or insert a placeholder when empty.
    // Additional behavior: if the selection is enclosed by markers (before/after selection), remove them.
    private void applyWrapOrPlaceholder(String pre, String post, String placeholder) {
        String selected = editor.getSelectedText();
        IndexRange range = editor.getSelection();
        String fullText = editor.getText();

        if (selected != null && !selected.isEmpty()) {
            // Case 1: selected text includes the markers -> unwrap
            if (selected.startsWith(pre) && selected.endsWith(post) && selected.length() > pre.length() + post.length()) {
                String inner = selected.substring(pre.length(), selected.length() - post.length());
                editor.replaceText(range, inner);
                editor.selectRange(range.getStart(), range.getStart() + inner.length());
                editor.requestFocus();
                return;
            }

            int selStart = range.getStart();
            int selEnd = range.getEnd();
            int beforeStart = selStart - pre.length();
            int afterEnd = selEnd + post.length();

            // Case 2: markers are immediately around the selection in the document -> remove them
            if (beforeStart >= 0 && afterEnd <= fullText.length()) {
                String before = fullText.substring(beforeStart, selStart);
                String after = fullText.substring(selEnd, afterEnd);
                if (before.equals(pre) && after.equals(post)) {
                    // remove the after marker first then the before marker
                    editor.replaceText(selEnd, afterEnd, "");
                    editor.replaceText(beforeStart, beforeStart + pre.length(), "");
                    // reselect the inner text
                    editor.selectRange(beforeStart, beforeStart + (selEnd - selStart));
                    editor.requestFocus();
                    return;
                }
            }

            // Default: wrap the selection
            editor.replaceText(range, pre + selected + post);
            editor.selectRange(range.getStart(), range.getStart() + pre.length() + selected.length() + post.length());
            editor.requestFocus();
            return;
        }

        // No selection: check if caret is inside a wrapped word and toggle
        int pos = editor.getCaretPosition();
        // find word boundaries
        int start = pos;
        int end = pos;
        while (start > 0 && !Character.isWhitespace(fullText.charAt(start - 1))) start--;
        while (end < fullText.length() && !Character.isWhitespace(fullText.charAt(end))) end++;
        if (start < end) {
            String word = fullText.substring(start, end);
            if (word.startsWith(pre) && word.endsWith(post) && word.length() > pre.length() + post.length()) {
                String inner = word.substring(pre.length(), word.length() - post.length());
                editor.replaceText(start, end, inner);
                editor.selectRange(start, start + inner.length());
                editor.requestFocus();
                return;
            }
        }

        // Otherwise insert placeholder wrapped text
        String insert = pre + placeholder + post;
        editor.insertText(pos, insert);
        editor.selectRange(pos + pre.length(), pos + pre.length() + placeholder.length());
        editor.requestFocus();
    }

    private void insertHorizontalRule() {
        int pos = editor.getCaretPosition();
        String hr = "\n---\n";
        editor.insertText(pos, hr);
        editor.requestFocus();
    }
}
