
/**
 * MainApp acts as a launcher that uses Reflection to start the Main class.
 * This is a 'clean' launcher that doesn't import JavaFX directly, allowing
 * it to compile successfully even if the IDE hasn't linked the libraries yet.
 */
public class MainApp {
    public static void main(String[] args) {
        try {
            // Load the Main class dynamically to avoid compile-time dependency on Application
            Class<?> mainClass = Class.forName("Main");
            
            // Invoke the static main(String[] args) method of the Main class
            java.lang.reflect.Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
            
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL ERROR: Main.class not found.");
            System.err.println("Please compile your files first using: javac --module-path ... *.java");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Application failed to launch.");
            System.err.println("This is usually caused by missing JavaFX modules at runtime.");
            System.err.println("Ensure your --module-path points to the 'lib' folder of your JavaFX SDK.");
            e.printStackTrace();
        }
    }
}
