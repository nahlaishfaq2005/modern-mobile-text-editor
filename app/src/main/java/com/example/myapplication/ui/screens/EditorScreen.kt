package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.editor.EditorSidebar
import com.example.myapplication.ui.components.editor.*
import com.example.myapplication.ui.viewmodel.EditorViewModel
import com.example.myapplication.ui.viewmodel.HomeViewModel
import com.example.myapplication.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    fileName: String = "MainActivity.kt",
    fileType: String = "Kotlin",
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToRecentAll: () -> Unit = {},
    onNavigateToEditor: (String, String) -> Unit = { _, _ -> },
    onNavigateToVersions: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: EditorViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val content by viewModel.content
    val isWordWrapEnabled by settingsViewModel.isWordWrapEnabled.collectAsState()
    val fontSize by settingsViewModel.fontSize.collectAsState()
    val fontFamilyName by settingsViewModel.fontFamily.collectAsState()
    
    val editorFontFamily = when (fontFamilyName) {
        "Monospace" -> FontFamily.Monospace
        "Sans Serif" -> FontFamily.SansSerif
        "Serif" -> FontFamily.Serif
        else -> FontFamily.Monospace
    }
    
    val showSearchReplace by viewModel.showSearchReplace.collectAsState()
    val isReplaceMode by viewModel.isReplaceMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val pendingRecovery by viewModel.pendingRecovery.collectAsState()
    val recentFiles by homeViewModel.recentFiles.collectAsState()
    val currentFileName by viewModel.currentFileName.collectAsState()
    val showSaveAsDialog by viewModel.showSaveAsDialog.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    
    LaunchedEffect(fileName) {
        viewModel.loadFile(fileName)
    }
    
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRecoveryPreview by remember { mutableStateOf(false) }
    var showCreateVersionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    if (showSaveAsDialog) {
        SaveAsDialog(
            initialName = currentFileName.ifEmpty { "Untitled.kt" },
            onDismiss = { viewModel.setShowSaveAsDialog(false) },
            onSave = { newName ->
                val success = viewModel.saveFileAs(newName)
                viewModel.setShowSaveAsDialog(false)
                scope.launch {
                    if (success) {
                        snackbarHostState.showSnackbar("File saved as $newName")
                    } else {
                        snackbarHostState.showSnackbar("Error saving file")
                    }
                }
            }
        )
    }

    if (showCreateVersionDialog) {
        CreateVersionDialog(
            onDismiss = { showCreateVersionDialog = false },
            onConfirm = { name ->
                viewModel.createVersion(fileName, name)
                showCreateVersionDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete This File?") },
            text = { Text("Are you sure you want to delete '$fileName'? This action cannot be undone and all versions will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentFile()
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    if (pendingRecovery != null) {
        RecoveryDialog(
            data = pendingRecovery!!,
            onRecover = { viewModel.recoverUnsavedWork(pendingRecovery!!) },
            onDiscard = { viewModel.discardRecovery(fileName) },
            onShowPreview = { showRecoveryPreview = true }
        )
    }

    if (showRecoveryPreview && pendingRecovery != null) {
        AlertDialog(
            onDismissRequest = { showRecoveryPreview = false },
            title = { Text("Recovery Preview") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = pendingRecovery!!.content,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        color = Color.White
                    )
                }
            },
            confirmButton = {
                Button(onClick = { 
                    viewModel.recoverUnsavedWork(pendingRecovery!!)
                    showRecoveryPreview = false 
                }) {
                    Text("Recover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryPreview = false }) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    if (showSearchReplace) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleSearchReplace(isReplaceMode) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SearchReplaceSheet(
                isReplaceMode = isReplaceMode,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                replaceQuery = replaceQuery,
                onReplaceQueryChange = { viewModel.onReplaceQueryChange(it) },
                onReplaceNext = { viewModel.replaceNext() },
                onReplaceAll = { viewModel.replaceAll() },
                onDismiss = { viewModel.toggleSearchReplace(isReplaceMode) }
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                EditorSidebar(
                    recentFiles = recentFiles,
                    activeFileName = fileName,
                    onFileClick = { file ->
                        scope.launch { 
                            drawerState.close()
                            onNavigateToEditor(file.name, file.type)
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    EditorTopBar(
                        fileName = currentFileName.ifEmpty { fileName },
                        fileType = fileType,
                        isLocked = isLocked,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        showMoreMenu = showMoreMenu,
                        onMoreMenuToggle = { showMoreMenu = it },
                        onSaveClick = { viewModel.saveFile(currentFileName.ifEmpty { fileName }) },
                        onSaveAsClick = { viewModel.setShowSaveAsDialog(true) },
                        onNavigateToVersions = onNavigateToVersions,
                        onCreateVersion = { showCreateVersionDialog = true },
                        onToggleLock = { viewModel.toggleLock() },
                        onDeleteFile = { showDeleteConfirm = true }
                    )
                    EditorToolbar(
                        fileType = fileType,
                        isPreviewMode = isPreviewMode,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onFormatClick = { viewModel.formatContent(fileType) },
                        onSearchClick = { viewModel.toggleSearchReplace(false) },
                        onReplaceClick = { viewModel.toggleSearchReplace(true) },
                        onSaveClick = { viewModel.saveFile(currentFileName.ifEmpty { fileName }) },
                        onTogglePreview = { viewModel.togglePreviewMode() }
                    )
                }
            },
            bottomBar = {
                Column {
                    EditorStatusBar(
                        line = 11,
                        col = 25,
                        fileType = fileType,
                        saveStatus = saveStatus
                    )
                    HomeBottomNavigation(
                        currentRoute = "editor",
                        onHomeClick = onNavigateToHome,
                        onEditorClick = { /* Already in editor, but maybe refresh? */ },
                        onVersionsClick = onNavigateToVersions,
                        onSettingsClick = onNavigateToSettings
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
            ) {
                if (fileType == "Markdown" && isPreviewMode) {
                    MarkdownPreview(content.text)
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        LineNumbers(content.text.lines().size, fontSize, editorFontFamily)
                        
                        BasicTextField(
                            content,
                            { viewModel.onContentChange(it) },
                            readOnly = isLocked,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .then(
                                    if (isWordWrapEnabled) {
                                        Modifier.verticalScroll(rememberScrollState())
                                    } else {
                                        Modifier.horizontalScroll(rememberScrollState())
                                            .verticalScroll(rememberScrollState())
                                    }
                                ),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = editorFontFamily,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.5).sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            visualTransformation = { text ->
                                val highlighted = if (fileType == "Kotlin") {
                                    SyntaxHighlighter.highlightKotlin(text.text)
                                } else {
                                    SyntaxHighlighter.highlightMarkdown(text.text)
                                }
                                androidx.compose.ui.text.input.TransformedText(
                                    highlighted,
                                    androidx.compose.ui.text.input.OffsetMapping.Identity
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorTopBar(
    fileName: String,
    fileType: String,
    isLocked: Boolean,
    onMenuClick: () -> Unit,
    showMoreMenu: Boolean,
    onMoreMenuToggle: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit = {},
    onNavigateToVersions: () -> Unit = {},
    onCreateVersion: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    onDeleteFile: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                
                // Nice Icon Box (matching Home Screen style)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconText = when (fileType) {
                        "Kotlin" -> "K"
                        "Markdown" -> "M"
                        "Plain Text" -> "T"
                        else -> null
                    }
                    
                    if (iconText != null) {
                        Text(
                            text = iconText,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = when (fileType) {
                                "Markdown" -> Icons.Default.Description
                                else -> Icons.AutoMirrored.Filled.Notes
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "$fileType File",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Box {
                IconButton(onClick = { onMoreMenuToggle(true) }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { onMoreMenuToggle(false) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isLocked) "Unlock File" else "Lock File") },
                        onClick = { 
                            onToggleLock()
                            onMoreMenuToggle(false) 
                        },
                        leadingIcon = { Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Save") },
                        onClick = { 
                            onSaveClick()
                            onMoreMenuToggle(false) 
                        },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Save As") },
                        onClick = { 
                            onSaveAsClick()
                            onMoreMenuToggle(false) 
                        },
                        leadingIcon = { Icon(Icons.Default.SaveAs, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Create Version") },
                        onClick = { 
                            onCreateVersion()
                            onMoreMenuToggle(false)
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Version History") },
                        onClick = { 
                            onNavigateToVersions()
                            onMoreMenuToggle(false) 
                        },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                    HorizontalDivider(color = Color.DarkGray)
                    DropdownMenuItem(
                        text = { Text("Delete File") },
                        onClick = { 
                            onDeleteFile()
                            onMoreMenuToggle(false) 
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        colors = MenuDefaults.itemColors(textColor = Color.Red)
                    )
                }
            }
        }
    }
}

@Composable
fun EditorToolbar(
    fileType: String,
    isPreviewMode: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFormatClick: () -> Unit,
    onSearchClick: () -> Unit,
    onReplaceClick: () -> Unit,
    onSaveClick: () -> Unit,
    onTogglePreview: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUndo) {
            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRedo) {
            Icon(Icons.Default.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        if (fileType == "Markdown") {
            IconButton(onClick = onTogglePreview) {
                Icon(
                    if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                    contentDescription = "Toggle Preview",
                    tint = if (isPreviewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onReplaceClick) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Replace", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        VerticalDivider(
            modifier = Modifier.height(24.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(onClick = onSaveClick) {
            Icon(Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LineNumbers(lineCount: Int, fontSize: Int = 12, fontFamily: FontFamily = FontFamily.Monospace) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width((fontSize * 2.5).dp)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in 1..lineCount) {
            Text(
                text = i.toString(),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    fontFamily = fontFamily,
                    fontSize = (fontSize * 0.8).sp
                ),
                modifier = Modifier.height((fontSize * 1.5).dp)
            )
        }
    }
}

@Composable
fun EditorStatusBar(line: Int, col: Int, fileType: String, saveStatus: String) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ln $line    Col $col    UTF-8",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fileType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = saveStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownPreview(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = text.lines()
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row {
                        Text("• ", color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            text = line.substring(2),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
