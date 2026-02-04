import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple recent files manager: keeps up to `limit` most recent entries per user.
 * Stores list as plain text file in user home: .seproj_recent_<username>.txt
 */
public class RecentFiles {
    private final List<File> recent = new ArrayList<>();
    private final int limit = 10;
    private final Path storage;

    public RecentFiles(String username) {
        String fileName = ".seproj_recent_" + (username == null || username.isEmpty() ? "guest" : username) + ".txt";
        this.storage = Path.of(System.getProperty("user.home"), fileName);
        load();
    }

    public synchronized void add(File f) {
        if (f == null) return;
        try {
            File absolute = f.getAbsoluteFile();
            recent.removeIf(x -> x.getAbsolutePath().equals(absolute.getAbsolutePath()));
            recent.add(0, absolute);
            while (recent.size() > limit) recent.remove(recent.size() - 1);
            save();
        } catch (Exception e) {
            // ignore storage errors
        }
    }

    public synchronized List<File> getRecentFiles() {
        return Collections.unmodifiableList(recent);
    }

    private void load() {
        recent.clear();
        try {
            if (!Files.exists(storage)) return;
            List<String> lines = Files.readAllLines(storage);
            for (String l : lines) {
                if (l == null || l.isBlank()) continue;
                File f = new File(l.trim());
                if (f.exists()) recent.add(f);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private void save() {
        try {
            List<String> lines = new ArrayList<>();
            for (File f : recent) lines.add(f.getAbsolutePath());
            Files.write(storage, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            // ignore
        }
    }
}
