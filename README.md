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
Tables: One click inserts a professional Markdown table template.
Shortcuts: Ctrl+S saves, Ctrl+B bolds, Ctrl+N new file—it feels like a real desktop app.
Layout Toggling: You can hide the preview if you want to focus only on writing.
