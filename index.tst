
import React, { useState, useEffect, useRef } from 'react';
import { createRoot } from 'react-dom/client';
import { GoogleGenAI } from '@google/genai';

// --- Types ---
interface Note {
  id: string;
  title: string;
  content: string;
  updatedAt: number;
}

const generateId = () => Math.random().toString(36).substring(2, 9);

const INITIAL_CONTENT = `# 📔 Microsoft Word Pro Studio

This workspace is designed to function exactly like a desktop word processor.

### Desktop Features:
- [ ] Use **File > Save As** to pick a location on your PC
- [x] Use **Format > Bold** or **Ctrl+B**
- [ ] Sync local files directly

### Quick Start:
1. **Save**: Press **Ctrl+S** to save changes to your local file.
2. **AI**: Highlight text and use **Edit > Smart Define**.
3. **Insert**: Add tables or images from the **Insert** menu.
`;

const App = () => {
  const [notes, setNotes] = useState<Note[]>([]);
  const [recentIds, setRecentIds] = useState<string[]>([]);
  const [activeNoteId, setActiveNoteId] = useState<string | null>(null);
  const [sidebarWidth, setSidebarWidth] = useState(260);
  const [isResizing, setIsResizing] = useState(false);
  
  // Persistent File Handle for true desktop "Save" logic
  const [fileHandle, setFileHandle] = useState<FileSystemFileHandle | null>(null);

  // Menu State
  const [activeMenu, setActiveMenu] = useState<string | null>(null);
  const [hoverSubMenu, setHoverSubMenu] = useState<string | null>(null);

  // View & UI State
  const [zoom, setZoom] = useState(1);
  const [showStatusBar, setShowStatusBar] = useState(true);
  const [wordWrap, setWordWrap] = useState(true);
  const [viewMode, setViewMode] = useState<'both' | 'editor' | 'preview'>('both');
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [aiResponse, setAiResponse] = useState<string | null>(null);
  
  const editorRef = useRef<HTMLTextAreaElement>(null);
  const previewRef = useRef<HTMLDivElement>(null);

  // Statistics
  const getWordCount = (text: string) => text.split(/\s+/).filter(x => x.length > 0).length;
  const getPageCount = (text: string) => Math.ceil(getWordCount(text) / 300) || 1;

  // --- Initialization ---
  useEffect(() => {
    const savedNotes = localStorage.getItem('word-studio-notes');
    const savedRecent = localStorage.getItem('word-studio-recent');
    
    if (savedNotes) {
      const parsed = JSON.parse(savedNotes);
      setNotes(parsed);
      if (parsed.length > 0) setActiveNoteId(parsed[0].id);
    } else {
      const firstNote: Note = {
        id: generateId(),
        title: 'Document 1',
        content: INITIAL_CONTENT,
        updatedAt: Date.now()
      };
      setNotes([firstNote]);
      setActiveNoteId(firstNote.id);
      setRecentIds([firstNote.id]);
    }
    if (savedRecent) setRecentIds(JSON.parse(savedRecent));
  }, []);

  useEffect(() => {
    localStorage.setItem('word-studio-notes', JSON.stringify(notes));
    localStorage.setItem('word-studio-recent', JSON.stringify(recentIds));
  }, [notes, recentIds]);

  useEffect(() => {
    if (activeNoteId) {
      setRecentIds(prev => {
        const filtered = prev.filter(id => id !== activeNoteId);
        return [activeNoteId, ...filtered].slice(0, 10);
      });
      setFileHandle(null); // Reset file handle when switching local browser notes
    }
  }, [activeNoteId]);

  const activeNote = notes.find(n => n.id === activeNoteId);

  // --- File Handlers (Word Style) ---
  const handleUpdateContent = (content: string) => {
    if (!activeNoteId) return;
    setNotes(prev => prev.map(n => n.id === activeNoteId ? { ...n, content, updatedAt: Date.now() } : n));
  };

  const createNewNote = () => {
    const newNote: Note = {
      id: generateId(),
      title: `Document ${notes.length + 1}`,
      content: '# New Document\n\n- [ ] ',
      updatedAt: Date.now()
    };
    setNotes([newNote, ...notes]);
    setActiveNoteId(newNote.id);
    setFileHandle(null);
    setActiveMenu(null);
    setTimeout(() => editorRef.current?.focus(), 50);
  };

  const openSystemFile = async () => {
    try {
      const [handle] = await (window as any).showOpenFilePicker({
        types: [{ description: 'Markdown/Text', accept: { 'text/markdown': ['.md'], 'text/plain': ['.txt'] } }],
      });
      const file = await handle.getFile();
      const content = await file.text();
      const newNote: Note = {
        id: generateId(),
        title: file.name.replace(/\.(md|txt)$/, ''),
        content: content,
        updatedAt: Date.now()
      };
      setNotes([newNote, ...notes]);
      setActiveNoteId(newNote.id);
      setFileHandle(handle);
      setActiveMenu(null);
    } catch (err) {
      console.error("Open File Cancelled or Error:", err);
    }
  };

  const saveFile = async () => {
    if (!activeNote) return;
    if (fileHandle) {
      try {
        const writable = await (fileHandle as any).createWritable();
        await writable.write(activeNote.content);
        await writable.close();
      } catch (err) {
        saveFileAs();
      }
    } else {
      saveFileAs();
    }
  };

  const saveFileAs = async () => {
    if (!activeNote) return;
    try {
      // NATIVE SYSTEM SAVE AS DIALOG
      const handle = await (window as any).showSaveFilePicker({
        suggestedName: `${activeNote.title}.md`,
        types: [{
          description: 'Markdown Document',
          accept: { 'text/markdown': ['.md'], 'text/plain': ['.txt'] },
        }],
      });
      setFileHandle(handle);
      const writable = await handle.createWritable();
      await writable.write(activeNote.content);
      await writable.close();
      setActiveMenu(null);
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        // Fallback for browsers without File System Access API
        const blob = new Blob([activeNote.content], { type: 'text/markdown' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${activeNote.title}.md`;
        a.click();
        URL.revokeObjectURL(url);
      }
    }
  };

  const applyFormat = (prefix: string, suffix: string = prefix) => {
    if (!editorRef.current) return;
    const textarea = editorRef.current;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selection = textarea.value.substring(start, end);
    const newText = prefix + selection + suffix;
    const newContent = textarea.value.substring(0, start) + newText + textarea.value.substring(end);
    handleUpdateContent(newContent);
    setActiveMenu(null);
    setTimeout(() => {
        textarea.focus();
        textarea.setSelectionRange(start + prefix.length, end + prefix.length);
    }, 0);
  };

  const handleClipboard = async (action: 'copy' | 'cut' | 'paste') => {
    if (!editorRef.current) return;
    const textarea = editorRef.current;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selection = textarea.value.substring(start, end);

    if (action === 'copy' || action === 'cut') {
      if (!selection) return;
      await navigator.clipboard.writeText(selection);
      if (action === 'cut') {
        const newContent = textarea.value.substring(0, start) + textarea.value.substring(end);
        handleUpdateContent(newContent);
      }
    } else if (action === 'paste') {
      try {
        const text = await navigator.clipboard.readText();
        const newContent = textarea.value.substring(0, start) + text + textarea.value.substring(end);
        handleUpdateContent(newContent);
      } catch (e) {
        alert("Permission denied. Use Ctrl+V.");
      }
    }
    setActiveMenu(null);
  };

  const aiAction = async (prompt: string) => {
    if (isAiLoading || !activeNote) return;
    setIsAiLoading(true);
    setAiResponse(null);
    try {
      const ai = new GoogleGenAI({ apiKey: process.env.API_KEY || '' });
      const response = await ai.models.generateContent({
        model: 'gemini-3-pro-preview',
        contents: `${prompt}\n\nDocument Context:\n${activeNote.content}`,
        config: { thinkingConfig: { thinkingBudget: 1000 } }
      });
      setAiResponse(response.text || 'Analysis complete.');
    } catch (e: any) {
      setAiResponse(`Error: ${e.message}`);
    } finally {
      setIsAiLoading(false);
    }
  };

  const handlePreviewClick = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement;
    if (target.tagName === 'INPUT' && (target as HTMLInputElement).type === 'checkbox') {
      const isChecked = (target as HTMLInputElement).checked;
      const index = parseInt(target.dataset.index || '-1');
      if (index === -1 || !activeNote) return;

      let count = 0;
      const newContent = activeNote.content.replace(/\[[ xX]\]/g, (match) => {
        if (count === index) {
          count++;
          return isChecked ? '[x]' : '[ ]';
        }
        count++;
        return match;
      });
      handleUpdateContent(newContent);
    }
  };

  const renderMarkdown = () => {
    if (!activeNote) return '';
    let count = 0;
    const html = (window as any).marked.parse(activeNote.content);
    return html.replace(/<input type="checkbox"/g, () => `<input type="checkbox" data-index="${count++}"`);
  };

  // --- Keyboard Shortcuts ---
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.key === 's') { e.preventDefault(); saveFile(); }
      if (e.ctrlKey && e.key === 'n') { e.preventDefault(); createNewNote(); }
      if (e.ctrlKey && e.key === 'o') { e.preventDefault(); openSystemFile(); }
      if (e.ctrlKey && e.key === 'b') { e.preventDefault(); applyFormat('**', '**'); }
      if (e.ctrlKey && e.key === 'i') { e.preventDefault(); applyFormat('*', '*'); }
      if (e.ctrlKey && e.key === 'u') { e.preventDefault(); applyFormat('<u>', '</u>'); }
      if (e.ctrlKey && e.key === 'p') { e.preventDefault(); window.print(); }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeNote, fileHandle]);

  // --- Menu Definitions ---
  const menus = [
    {
      id: 'file', label: 'File', items: [
        { label: 'New Document', shortcut: 'Ctrl+N', action: createNewNote },
        { label: 'Open File...', shortcut: 'Ctrl+O', action: openSystemFile },
        { label: 'Recent Documents', hasSub: true, subItems: recentIds.map(id => ({ label: notes.find(n => n.id === id)?.title || 'Note', action: () => setActiveNoteId(id) })) },
        { type: 'separator' },
        { label: 'Save', shortcut: 'Ctrl+S', action: saveFile },
        { label: 'Save As...', shortcut: 'Ctrl+Shift+S', action: saveFileAs },
        { label: 'Print...', shortcut: 'Ctrl+P', action: () => window.print() },
        { type: 'separator' },
        { label: 'Export to PDF', action: () => (window as any).html2pdf().from(previewRef.current).set({ filename: `${activeNote?.title}.pdf`, margin: 1 }).save() },
        { label: 'Exit Application', action: () => window.close() }
      ]
    },
    {
      id: 'edit', label: 'Edit', items: [
        { label: 'Undo Typing', shortcut: 'Ctrl+Z', action: () => document.execCommand('undo') },
        { type: 'separator' },
        { label: 'Cut Selection', shortcut: 'Ctrl+X', action: () => handleClipboard('cut') },
        { label: 'Copy Selection', shortcut: 'Ctrl+C', action: () => handleClipboard('copy') },
        { label: 'Paste Clipboard', shortcut: 'Ctrl+V', action: () => handleClipboard('paste') },
        { type: 'separator' },
        { label: 'Smart Define (AI)', shortcut: 'Ctrl+E', action: () => aiAction('Analyze this text and provide high-level professional definitions and insights.') },
        { label: 'Select All', shortcut: 'Ctrl+A', action: () => { editorRef.current?.focus(); editorRef.current?.select(); } },
        { label: 'Find & Replace', shortcut: 'Ctrl+F', action: () => { const q = prompt("Find text:"); if (q) (window as any).find(q); } }
      ]
    },
    {
        id: 'insert', label: 'Insert', items: [
            { label: 'Checklist Task', action: () => applyFormat('\n- [ ] ', '') },
            { label: 'Table (3x2)', action: () => applyFormat('\n| Col 1 | Col 2 |\n| --- | --- |\n| Cell | Cell |\n', '') },
            { label: 'Hyperlink...', shortcut: 'Ctrl+K', action: () => applyFormat('[Link Text](', ')') },
            { label: 'Image Attachment', action: () => applyFormat('![Caption](', ')') },
            { type: 'separator' },
            { label: 'Horizontal Line', action: () => applyFormat('\n---\n', '') },
            { label: 'Date & Time', shortcut: 'F5', action: () => applyFormat(new Date().toLocaleString(), '') }
        ]
    },
    {
      id: 'format', label: 'Format', items: [
        { label: 'Bold', shortcut: 'Ctrl+B', action: () => applyFormat('**', '**') },
        { label: 'Italic', shortcut: 'Ctrl+I', action: () => applyFormat('*', '*') },
        { label: 'Underline', shortcut: 'Ctrl+U', action: () => applyFormat('<u>', '</u>') },
        { label: 'Strikethrough', action: () => applyFormat('~~', '~~') },
        { type: 'separator' },
        { label: 'Heading 1', action: () => applyFormat('# ', '') },
        { label: 'Heading 2', action: () => applyFormat('## ', '') },
        { label: 'Blockquote', action: () => applyFormat('> ', '') },
        { type: 'separator' },
        { label: 'Bullet List', action: () => applyFormat('\n- ', '') },
        { label: 'Numbered List', action: () => applyFormat('\n1. ', '') }
      ]
    },
    {
      id: 'view', label: 'View', items: [
        { label: 'Zoom (%)', hasSub: true, subItems: [
          { label: '80%', action: () => setZoom(0.8) },
          { label: '100%', action: () => setZoom(1) },
          { label: '125%', action: () => setZoom(1.25) },
          { label: '150%', action: () => setZoom(1.5) }
        ] },
        { label: 'Show Status Bar', checked: showStatusBar, action: () => setShowStatusBar(!showStatusBar) },
        { label: 'Word Wrap', checked: wordWrap, action: () => setWordWrap(!wordWrap) },
        { label: 'Layout Modes', hasSub: true, subItems: [
          { label: 'Split Workspace', action: () => setViewMode('both') },
          { label: 'Full Editor', action: () => setViewMode('editor') },
          { label: 'Reading Mode', action: () => setViewMode('preview') }
        ] }
      ]
    }
  ];

  return (
    <div className="flex h-screen w-full bg-white overflow-hidden text-slate-700 select-none font-sans" onMouseDown={() => { setActiveMenu(null); setHoverSubMenu(null); }}>
      
      {/* Sidebar - Desktop Project Navigation */}
      <aside 
        style={{ width: sidebarWidth }}
        className="transition-all duration-300 ease-in-out flex flex-col overflow-hidden relative border-r border-slate-200 bg-[#F9FAFB]"
      >
        <div className="p-8 pb-4 flex items-center gap-3">
          <div className="w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center text-white shadow-lg rotate-3">
            <i className="fa-solid fa-pen-nib text-sm"></i>
          </div>
          <h1 className="font-bold text-sm tracking-tight text-indigo-950 uppercase">Studio notebook</h1>
        </div>

        <div className="flex-1 overflow-y-auto custom-scrollbar px-4 space-y-8 pt-4 pb-12">
          {recentIds.length > 0 && (
            <div>
              <p className="px-4 text-[9px] font-black text-indigo-300 uppercase tracking-[0.2em] mb-4">Recents</p>
              <div className="space-y-1">
                {recentIds.map(id => {
                  const n = notes.find(x => x.id === id);
                  if (!n) return null;
                  return (
                    <div key={id} onClick={() => setActiveNoteId(id)} className={`px-4 py-2.5 cursor-pointer rounded-xl transition-all ${activeNoteId === id ? 'bg-indigo-600 text-white shadow-lg' : 'hover:bg-indigo-50/80 text-slate-500'}`}>
                      <p className="text-xs font-bold truncate">{n.title}</p>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          <div>
            <p className="px-4 text-[9px] font-black text-indigo-300 uppercase tracking-[0.2em] mb-4">Library</p>
            <div className="space-y-1">
              {notes.map(note => (
                <div key={note.id} onClick={() => setActiveNoteId(note.id)} className={`group relative px-4 py-3 cursor-pointer rounded-2xl transition-all border ${activeNoteId === note.id ? 'bg-white border-indigo-100 shadow-sm' : 'border-transparent hover:bg-white hover:border-slate-100'}`}>
                  <div className="flex items-center gap-2">
                    <div className={`w-1.5 h-1.5 rounded-full ${activeNoteId === note.id ? 'bg-indigo-500' : 'bg-slate-200'}`}></div>
                    <h3 className={`font-bold text-xs truncate ${activeNoteId === note.id ? 'text-indigo-900' : 'text-slate-400'}`}>{note.title}</h3>
                  </div>
                  <button onMouseDown={(e) => { e.stopPropagation(); setNotes(notes.filter(x => x.id !== note.id)); }} className="absolute right-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 p-2 text-rose-300 hover:text-rose-500">
                    <i className="fa-solid fa-trash-can text-[10px]"></i>
                  </button>
                </div>
              ))}
            </div>
          </div>
          
          <button onMouseDown={createNewNote} className="mx-4 w-[calc(100%-32px)] py-4 border-2 border-dashed border-indigo-50 rounded-3xl text-indigo-300 text-[9px] font-black uppercase tracking-widest hover:border-indigo-200 hover:text-indigo-500 transition-all">
            + New Document
          </button>
        </div>
        <div onMouseDown={() => setIsResizing(true)} className="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-indigo-200 transition-colors" />
      </aside>

      {/* Workspace Area */}
      <main className="flex-1 flex flex-col h-full bg-white relative">
        
        {/* Microsoft Word Menu Bar */}
        <header className="h-9 flex items-center px-4 bg-[#F1F5F9] border-b border-slate-200 z-[100] gap-0.5 shadow-sm">
          {menus.map(menu => (
            <div key={menu.id} className="relative">
              <button onMouseDown={(e) => { e.stopPropagation(); setActiveMenu(activeMenu === menu.id ? null : menu.id); setHoverSubMenu(null); }} className={`px-3 py-1 rounded-md text-[11px] font-medium transition-all ${activeMenu === menu.id ? 'bg-white shadow-sm text-indigo-600' : 'hover:bg-slate-200 text-slate-700'}`}>
                {menu.label}
              </button>
              {activeMenu === menu.id && (
                <div className="absolute top-8 left-0 w-64 bg-white border border-slate-200 shadow-2xl rounded-xl py-2 z-[150] animate-menu">
                  {menu.items.map((item, idx) => (
                    item.type === 'separator' ? (
                      <div key={idx} className="h-[1px] bg-slate-100 my-1.5 mx-2" />
                    ) : (
                      <div key={item.label} className="relative" onMouseEnter={() => item.hasSub && setHoverSubMenu(item.label)}>
                        <button onClick={(e) => { e.stopPropagation(); if(!item.hasSub) { item.action(); setActiveMenu(null); } }} className={`w-full text-left px-4 py-2 text-[11px] flex items-center justify-between transition-colors ${hoverSubMenu === item.label ? 'bg-indigo-50 text-indigo-600' : 'hover:bg-slate-50'}`}>
                          <div className="flex items-center gap-3">
                            <div className="w-3.5 flex justify-center">{item.checked && <i className="fa-solid fa-check text-[9px] text-indigo-500"></i>}</div>
                            <span className="font-semibold">{item.label}</span>
                          </div>
                          {item.shortcut ? <span className="text-[9px] opacity-40 ml-4 font-mono">{item.shortcut}</span> : item.hasSub ? <i className="fa-solid fa-chevron-right text-[7px] text-slate-300"></i> : null}
                        </button>
                        {item.hasSub && hoverSubMenu === item.label && (
                          <div className="absolute top-0 left-full -ml-1 w-52 bg-white border border-slate-200 shadow-2xl rounded-xl py-2 animate-menu">
                            {item.subItems?.map((sub, sIdx) => (
                              <button key={sIdx} onClick={(e) => { e.stopPropagation(); sub.action(); setActiveMenu(null); }} className="w-full text-left px-4 py-2 text-[11px] hover:bg-indigo-50 hover:text-indigo-600 transition-colors font-semibold truncate">{sub.label}</button>
                            ))}
                          </div>
                        )}
                      </div>
                    )
                  ))}
                </div>
              )}
            </div>
          ))}
        </header>

        {activeNote ? (
          <div className="flex-1 flex flex-col md:flex-row overflow-hidden relative" style={{ transform: `scale(${zoom})`, transformOrigin: 'top left', width: `${100/zoom}%`, height: `${100/zoom}%` }}>
            {(viewMode === 'both' || viewMode === 'editor') && (
              <div className="flex-1 flex flex-col border-r border-slate-100 bg-white">
                <textarea ref={editorRef} value={activeNote.content} onChange={(e) => handleUpdateContent(e.target.value)} wrap={wordWrap ? 'soft' : 'off'} className="flex-1 w-full p-12 outline-none text-slate-800 leading-relaxed resize-none font-medium custom-scrollbar selection:bg-indigo-100 text-[15px]" placeholder="Start writing document content..." />
              </div>
            )}
            {(viewMode === 'both' || viewMode === 'preview') && (
              <div className="flex-1 bg-slate-50/20 flex flex-col" onClick={handlePreviewClick}>
                <div className="flex-1 p-12 overflow-y-auto custom-scrollbar bg-white">
                    <div className="max-w-3xl mx-auto">
                      <div ref={previewRef} className="markdown-preview" dangerouslySetInnerHTML={{ __html: renderMarkdown() }} />
                    </div>
                </div>
              </div>
            )}
            {(isAiLoading || aiResponse) && (
              <div className="fixed bottom-12 right-8 w-80 bg-white rounded-2xl shadow-2xl border border-indigo-50 z-[200] animate-menu overflow-hidden">
                <div className="px-5 py-3 bg-indigo-600 text-white flex items-center justify-between">
                  <span className="text-[10px] font-black uppercase tracking-[0.2em]">Studio AI Context</span>
                  <button onClick={() => { setAiResponse(null); setIsAiLoading(false); }} className="hover:scale-125 transition-transform"><i className="fa-solid fa-xmark"></i></button>
                </div>
                <div className="p-6 max-h-64 overflow-y-auto custom-scrollbar text-xs leading-relaxed text-slate-600 font-medium">
                  {isAiLoading ? <div className="flex items-center gap-3"><i className="fa-solid fa-circle-notch animate-spin text-indigo-500"></i><span>Processing document intelligence...</span></div> : aiResponse}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center bg-slate-50/10 p-12 text-center">
            <div className="w-20 h-20 bg-white shadow-2xl rounded-[2.5rem] flex items-center justify-center mb-8 rotate-6"><i className="fa-solid fa-feather text-3xl text-indigo-100"></i></div>
            <h2 className="text-xl font-black text-indigo-950 uppercase italic tracking-tighter">Studio Idle</h2>
            <p className="mt-2 text-indigo-300 text-sm max-w-xs">Create a new session or open a local document from your computer.</p>
            <button onMouseDown={createNewNote} className="mt-8 px-10 py-4 bg-indigo-600 text-white rounded-2xl font-black text-[10px] uppercase tracking-[0.3em] shadow-2xl transition-all hover:scale-105 active:scale-95">Awaken Workspace</button>
          </div>
        )}

        {showStatusBar && (
          <footer className="h-7 border-t border-slate-200 flex items-center justify-between px-4 bg-[#F8FAFC] z-[80] text-[10px] font-bold text-slate-400">
            <div className="flex items-center gap-6">
               <span className="flex items-center gap-2 uppercase tracking-widest"><i className="fa-solid fa-file-lines opacity-40"></i> PAGE {getPageCount(activeNote?.content || '')} OF {getPageCount(activeNote?.content || '')}</span>
               <span className="flex items-center gap-2 uppercase tracking-widest"><i className="fa-solid fa-book-open opacity-40"></i> {getWordCount(activeNote?.content || '')} WORDS</span>
            </div>
            <div className="flex items-center gap-6">
              {fileHandle && <span className="text-indigo-500 flex items-center gap-2"><i className="fa-solid fa-link"></i> SYNCED TO LOCAL FILE</span>}
              <span className="flex items-center gap-2 text-indigo-400 uppercase tracking-widest"><i className="fa-solid fa-shield-halved"></i> 100% SECURE</span>
            </div>
          </footer>
        )}
      </main>
    </div>
  );
};

const container = document.getElementById('root');
const root = createRoot(container!);
root.render(<App />);
