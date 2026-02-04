1. The Professional Layout
Ribbon & Menu Bar: A clean top section with quick-access buttons (Save, Bold, Italic) and a full-featured Menu system.
Dual-Pane Workspace: A split-screen view where you type on the left (Editor) and see the professional result on the right (Preview).
Status Bar: A dark blue professional bar at the bottom showing Word Count, Page Numbers, and Connection Status.


2. Function-by-Function Explanation
Core Setup & UI
MainView() (Constructor): This is the heart of the app. It calls setupLayout() to build the UI, initializeFirstNote() to give the user a starting document, and loadPreviewEngine() to prepare the Markdown renderer.

loadPreviewEngine(): This injects a complete HTML/CSS environment into the WebView. It includes the marked.js library so that Markdown looks like a professional published document (with nice tables, headers, and quotes).

setupLayout(): The "Master Architect" function. It builds the MenuBar, the ToolBar (Ribbon), the Sidebar (where your documents live), the SplitPane workspace, and the blue StatusBar.
File Management (The "File" Menu)

createNewNote(): Clears the active workspace and adds a new "Untitled" document to your sidebar.

openSystemFile(): Opens a Windows/Mac file picker so you can load any .md or .txt file from your hard drive into the Studio.

saveFile(): A smart function. If you’ve already saved the file, it updates it instantly. If it's a new file, it automatically calls saveFileAs().

saveFileAs(): Opens a "Save As" dialog allowing you to name your file and choose where to store it on your computer.

saveToFile(File file): The actual "Worker" that takes the text from your screen and writes it onto your computer's storage.

Content & Formatting (The "Edit" & "Insert" Menus)

applyFormatting(String pre, String post): A very efficient helper function. It looks at what text you highlighted, then wraps it in Markdown (e.g., adding ** before and after for Bold).

insertLocalPicture(): Opens a file picker specifically for images. When you pick a photo from your PC, it inserts the code to show that photo inside your document.

handleSmartDefine(): This simulates the AI feature. It captures your selected word and "analyzes" it, providing a professional popup window with insights.

Real-Time Intelligence

updatePreview(String content): Every single time you type a letter, this function sends that text to the internal web engine to update the preview instantly.

updateWordCount(String text): Calculates how many words you’ve written and updates the "Page Count" (assuming roughly 300 words per page) in the status bar.

The View Logic (The "View" Menu)
Layout Switching: I added logic to the View menu that allows you to toggle between:
Split Workspace: See both Editor and Preview.
Full Editor: Maximize space for writing.
Full Preview: A "Reading Mode" to see the final document.
Summary of what works now:
Save As: You can now save your work anywhere on your PC.
Image Insertion: You can pull photos directly from your hard drive into the document.
Tables: You can insert a custom-sized markdown table (Insert → Insert Table...).
Shortcuts: Ctrl+S saves, Ctrl+B bolds, Ctrl+N new file—it feels like a real desktop app.
Layout Toggling: You can hide the preview if you want to focus only on writing.

New Features (Added):
- Export as PDF: File → Export → Export as PDF. This attempts an HTML→PDF conversion using OpenHTMLToPDF (recommended), or falls back to the system print dialog.
- HTML → PDF: File → Export → HTML → PDF... (select an HTML file to convert to PDF using OpenHTMLToPDF).
- Recent Files & Login: File → Recent Files shows up to 10 recent files. Log in (File → Login...) to keep a user-specific recent-file history (stored in your home folder as `.seproj_recent_<username>.txt`).

Dependencies (for HTML → PDF):
To enable the best HTML→PDF conversion you should add OpenHTMLToPDF libraries to the classpath when running or building the application:
- com.openhtmltopdf:openhtmltopdf-core:1.0.10
- com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10

If you use Maven, add these to your `pom.xml`. If you don't use a build system, place the JARs on the classpath when running the app.

No-Maven: PowerShell downloader (Windows)
- A helper script `fetch_openhtmltopdf.ps1` is included. Run it from the project root to download required JARs into a `lib\` folder.

Example:
```
# run the downloader
.\fetch_openhtmltopdf.ps1
# compile with the downloaded jars
javac -cp "lib/*" --module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.web -d out *.java
# run the app
java --module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.web -cp "out;lib/*" MainApp
```

Notes & Tips:
- Recent file lists are saved per username in your system home folder.
- If OpenHTMLToPDF is not available, the app will prompt you to add it and will show options instead of automatically using the system default printer (this avoids automatically using an expired Adobe printer). You can either:
  - Run `fetch_openhtmltopdf.ps1` to download the required jars into `./lib/` and re-run the app, or
  - Choose another installed printer (File → Export → Export as PDF → Choose Printer...).

Tip: If your system default PDF printer is Adobe and it is expired, use the "Choose Printer..." option and pick a different printer (e.g., Microsoft Print to PDF) or use the HTML→PDF route which writes a PDF file directly using OpenHTMLToPDF.
