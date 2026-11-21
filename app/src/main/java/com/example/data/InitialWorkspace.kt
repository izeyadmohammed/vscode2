package com.example.data

import com.example.model.ExtensionItem
import com.example.model.FSNode

object InitialWorkspace {
    val initialFiles: List<FSNode> = emptyList()

    val initialExtensions: List<ExtensionItem> = listOf(
        ExtensionItem(
            id = "esbenp.prettier-vscode",
            name = "Prettier - Code formatter",
            publisher = "Prettier",
            description = "Code formatter using prettier for TypeScript, JavaScript, JSON, CSS, and HTML.",
            version = "11.0.0",
            downloads = "52.4M",
            rating = 4.5,
            category = "Formatters",
            installed = true,
            enabled = true,
            iconColorHex = 0xFFF7B93E,
            shortTag = "P"
        ),
        ExtensionItem(
            id = "dbaeumer.vscode-eslint",
            name = "ESLint",
            publisher = "Microsoft",
            description = "Integrates ESLint JavaScript & TypeScript diagnostics into VS Code.",
            version = "3.0.10",
            downloads = "39.8M",
            rating = 4.6,
            category = "Linters",
            installed = true,
            enabled = true,
            iconColorHex = 0xFF4B32C3,
            shortTag = "ES"
        ),
        ExtensionItem(
            id = "eamodio.gitlens",
            name = "GitLens — Git supercharged",
            publisher = "GitKraken",
            description = "Supercharge Git within VS Code — visualize code authorship, diffs, and commit history.",
            version = "15.6.1",
            downloads = "34.1M",
            rating = 4.7,
            category = "SCM Providers",
            installed = true,
            enabled = true,
            iconColorHex = 0xFFA855F7,
            shortTag = "GL"
        ),
        ExtensionItem(
            id = "bradlc.vscode-tailwindcss",
            name = "Tailwind CSS IntelliSense",
            publisher = "Tailwind Labs",
            description = "Intelligent Tailwind CSS tooling for VS Code with class previews and linting.",
            version = "0.12.8",
            downloads = "18.2M",
            rating = 4.8,
            category = "Other",
            installed = false,
            enabled = false,
            iconColorHex = 0xFF38BDF8,
            shortTag = "TW"
        ),
        ExtensionItem(
            id = "ritwickdey.liveserver",
            name = "Live Server",
            publisher = "Ritwick Dey",
            description = "Launch a development local Server with live reload feature for static & dynamic pages.",
            version = "5.7.9",
            downloads = "49.1M",
            rating = 4.6,
            category = "Other",
            installed = true,
            enabled = true,
            iconColorHex = 0xFFEF4444,
            shortTag = "LS"
        ),
        ExtensionItem(
            id = "ms-python.python",
            name = "Python",
            publisher = "Microsoft",
            description = "IntelliSense (Pylance), linting, debugging, code formatting, and refactoring for Python.",
            version = "2024.16.0",
            downloads = "142M",
            rating = 4.5,
            category = "Programming Languages",
            installed = false,
            enabled = false,
            iconColorHex = 0xFF3776AB,
            shortTag = "Py"
        )
    )
}
