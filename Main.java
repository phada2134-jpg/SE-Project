
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point for the Word-style AI Markdown Studio.
 */
public class Main extends Application {
    @Override
    public void start(Stage stage) {
        // Initialize the main UI component
        MainView mainView = new MainView();
        
        // Create a scene with standard Word dimensions (16:10 aspect)
        Scene scene = new Scene(mainView, 1280, 800);
        
        // Window metadata and styling
        stage.setTitle("Markdown Note-Taking App");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        
        // Add a professional icon (if you have one) or just show the stage
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
