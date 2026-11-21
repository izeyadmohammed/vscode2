# VS Code Web Studio (Android)

An Android Kotlin and Jetpack Compose code editor and IDE workbench rewritten from the React/Vite project (`izeyadmohammed/vs-code`).

## Features

- **Adaptive Workbench Layout**: Responsive Material 3 UI with edge-to-edge system bar styling, collapsible sidebar drawer on phones, and side-by-side layout with Split Editor support on tablets/landscape screens.
- **Multi-Tab Code Editor**: Syntax highlighting across TypeScript, TSX, JavaScript, JSX, HTML, CSS, JSON, Markdown, Python, and Plain Text; line numbers and clickable breakpoint gutters; in-file find bar; minimap strip; auto-closing brackets/quotes and smart indentation.
- **File Explorer & Storage Access Framework**: Create, rename, duplicate, collapse, delete, and export workspace files, plus import local project folders via Android SAF (`OpenDocumentTree`), Outline symbols, and commit Timeline.
- **Global Search & Replace**: Case-sensitive, whole-word, and regex search across all workspace files with one-tap Replace All and jump-to-line navigation.
- **Git Source Control & Diff Viewer**: Stage/unstage changes, discard edits, commit snapshots, inspect commit history, switch branches, and view line-by-line diffs (`HEAD` vs Working Tree).
- **Run, Debug Console & Terminal**: Execute JavaScript/TypeScript files in an isolated WebView JS sandbox, evaluate live expressions in Debug Console, preview HTML (WebView) and Markdown files side-by-side, and run bash-style commands across multiple terminal sessions.
- **Extensions, Themes & Cloud Sync**: Toggle extensions (Prettier formatting, ESLint workspace diagnostics, GitLens, etc.), switch between 6 color themes (`Dark Modern`, `Dark+`, `Light Modern`, `Monokai`, `GitHub Dark`, `Dracula`), open the Command Palette, and optionally sync workspaces via Supabase Auth & REST (`SUPABASE_URL` / `SUPABASE_ANON_KEY`).

## Build Requirements

- Android Gradle Plugin `9.1.1` / Gradle `9.3.1`
- JDK `21`
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`
