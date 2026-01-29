
import java.util.UUID;

/**
 * Represents a document in the Studio workspace.
 */
public class Note {
    private final String id;
    private String title;
    private String content;

    public Note(String title, String content) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.content = content;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public String toString() {
        return title;
    }
}
