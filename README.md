# Modern Text Editor for Android

A feature-rich, modern text editor application built with Jetpack Compose. This project focuses on high performance, reliable version control, and a seamless developer experience.

## 🤝 Contributions

This project was developed by a team of three members, each contributing to specific logical modules:

### **Nahla Ishfaq **
*   **Core Editor & File Management:** Implementation of file creation, opening, saving, and "Save As" functionality.
*   **Editing Features:** Developed undo/redo history, advanced search and replace, word wrapping, and read-only file locking.
*   **Integration:** Managed recent files repository and sidebar navigation.

### **Varshika Pavalaraj **
*   **Developer Features:** Implemented Kotlin code formatting and Kotlin syntax highlighting (strings, comments, annotations).
*   **Markdown Support:** Built the Markdown syntax highlighting and integrated the real-time Markdown preview toggle and rendering.
*   **Crash Recovery:** Developed the `RecoveryManager` for automatic background caching and session restoration after unexpected app closures.

### **Nuha Fawzer **
*   **Incremental Version Control:** Designed the core versioning engine, including delta/patch generation and duplicate prevention.
*   **Advanced Versioning UI:** Implemented version history viewing, side-by-side comparison, and line-by-line difference display.
*   **Persistence Layer:** Set up the Room database architecture to store file metadata, version history, and incremental deltas.
*   **Reconstruction Engine:** Built the logic to reconstruct files from a chain of stored versions and restore any historical point-in-time state.

## 🚀 Features

### 🖋️ Advanced Editing
- **Modern UI:** Built entirely with Jetpack Compose for a smooth, responsive interface.
- **Kotlin Code Formatting:** Integrated code formatter to keep your Kotlin files clean and organized.
- **Search & Replace:** Powerful search functionality with support for whole-word matching and global replace.
- **Word Wrap & Locking:** Toggle word wrap for better readability or lock the editor to prevent accidental changes.
- **Undo/Redo:** Full history tracking for your editing session.

### 📜 Version Control (Git-like)
- **Incremental Deltas:** Uses advanced diffing algorithms (`java-diff-utils`) to store only changes between versions, saving significant storage space.
- **Automated Versioning:** Automatically captures versions as you work, ensuring you never lose progress.
- **Version Comparison:** Side-by-side comparison between historical versions and the current state.
- **Point-in-Time Restore:** Easily restore any previous version of your file.
- **Manual Snapshots:** Name and save specific milestones in your development.

### 🛡️ Reliability & Recovery
- **Crash Recovery:** Background auto-caching (via `RecoveryManager`) ensures that even if the app closes unexpectedly, your unsaved work is preserved.
- **Room Database:** All version history and deltas are stored in a robust SQLite database using Room.
- **Physical Sync:** Manual saves ensure your work is physically written to storage while maintaining the version history.

## 🛠️ Technical Stack

- **UI:** Jetpack Compose (Material 3)
- **Database:** Room Persistence Library
- **Diffing:** [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils)
- **Language:** Kotlin 
