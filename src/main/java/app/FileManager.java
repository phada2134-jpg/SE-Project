package app;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;

public class FileManager {

    // --- Open a file (.MD or .txt)
    public static String loadFromFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Note");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Markdown Files", "*.md"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    // --- Save a file (.MD or .txt)
    public static void saveToFile(Stage stage, String content) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Note");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Markdown Files", "*.md"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- Export as HTML
    public static void exportHTML(String content, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export as HTML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files", "*.html"));
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                String htmlContent = content
                    .replaceAll("^# (.*)", "<h1>$1</h1>")
                    .replaceAll("^## (.*)", "<h2>$1</h2>")
                    .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                    .replaceAll("\\*(.*?)\\*", "<i>$1</i>");
                writer.write("<html><body>" + htmlContent + "</body></html>");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- Export as PDF (requires PDF library, pseudo-code)
    public static void exportPDF(String content, Stage stage) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export as PDF");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
    File file = fileChooser.showSaveDialog(stage);
    
    if (file != null) {
        try {
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Simple conversion: split by lines
            for (String line : content.split("\n")) {
                document.add(new Paragraph(line));
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
}

