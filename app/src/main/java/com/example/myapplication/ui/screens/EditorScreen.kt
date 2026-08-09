package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.myapplication.ui.components.editor.SearchReplaceSheet
import com.example.myapplication.ui.components.editor.SyntaxHighlighter
import com.example.myapplication.ui.viewmodel.EditorViewModel
import com.example.myapplication.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    fileName: String = "MainActivity.kt",
    fileType: String = "Kotlin",
    onNavigateBack: () -> Unit = {},
    onNavigateToRecentAll: () -> Unit = {},
    onNavigateToVersions: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: EditorViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val content by viewModel.content
    val isWordWrapEnabled by viewModel.isWordWrapEnabled.collectAsState()
    val showSearchReplace by viewModel.showSearchReplace.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val recentFiles by homeViewModel.recentFiles.collectAsState()
    
    var isPreviewMode by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showSearchReplace) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleSearchReplace() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SearchReplaceSheet(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                replaceQuery = replaceQuery,
                onReplaceQueryChange = { viewModel.onReplaceQueryChange(it) },
                onReplaceNext = { viewModel.replaceNext() },
                onReplaceAll = { viewModel.replaceAll() },
                onDismiss = { viewModel.toggleSearchReplace() }
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
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    EditorTopBar(
                        fileName = fileName,
                        fileType = fileType,
                        onBackClick = onNavigateBack,
                        onSearchClick = { viewModel.toggleSearchReplace() },
                        onGridClick = { scope.launch { drawerState.open() } }
                    )
                    EditorToolbar(
                        fileType = fileType,
                        isPreviewMode = isPreviewMode,
                        onPreviewToggle = { isPreviewMode = !isPreviewMode },
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onWordWrapToggle = { viewModel.toggleWordWrap() },
                        onSearchClick = { viewModel.toggleSearchReplace() },
                        isWordWrapEnabled = isWordWrapEnabled,
                        showMoreMenu = showMoreMenu,
                        onMoreMenuToggle = { showMoreMenu = it }
                    )
                }
            },
            bottomBar = {
                Column {
                    EditorStatusBar(
                        line = 11,
                        col = 25,
                        fileType = fileType,
                        saveStatus = "Autosaved"
                    )
                    HomeBottomNavigation(
                        currentRoute = "editor",
                        onHomeClick = onNavigateBack,
                        onRecentClick = onNavigateToRecentAll,
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
                        LineNumbers(content.text.lines().size)
                        
                        BasicTextField(
                            value = content,
                            onValueChange = { viewModel.onContentChange(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .then(if (isWordWrapEnabled) Modifier else Modifier.verticalScroll(rememberScrollState())),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
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
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onGridClick: () -> Unit
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
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                
                // Nice Icon Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (fileType == "Kotlin") Color(0xFF8B5CF6).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (fileType) {
                            "Kotlin" -> Icons.Default.Code
                            "Markdown" -> Icons.Default.Description
                            else -> Icons.AutoMirrored.Filled.Notes
                        },
                        contentDescription = null,
                        tint = if (fileType == "Kotlin") Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "$fileType File",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Row {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = onGridClick) {
                    Icon(Icons.Default.GridView, contentDescription = "Files", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
fun EditorToolbar(
    fileType: String,
    isPreviewMode: Boolean,
    onPreviewToggle: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onWordWrapToggle: () -> Unit,
    onSearchClick: () -> Unit,
    isWordWrapEnabled: Boolean,
    showMoreMenu: Boolean,
    onMoreMenuToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.LightGray)
            }
            IconButton(onClick = onRedo) {
                Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color.LightGray)
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray)
            }
        }
        Row {
            IconButton(onClick = onPreviewToggle) {
                Icon(Icons.Default.CropFree, contentDescription = "Toggle", tint = Color.LightGray)
            }
            IconButton(onClick = onWordWrapToggle) {
                Icon(
                    Icons.Default.WrapText,
                    contentDescription = "Word Wrap",
                    tint = if (isWordWrapEnabled) MaterialTheme.colorScheme.primary else Color.LightGray
                )
            }
            IconButton(onClick = { /* Save */ }) {
                Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.LightGray)
            }
            Box {
                IconButton(onClick = { onMoreMenuToggle(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.LightGray)
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { onMoreMenuToggle(false) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Save As") },
                        onClick = { onMoreMenuToggle(false) },
                        leadingIcon = { Icon(Icons.Default.SaveAs, contentDescription = null) }
                    )
                    if (fileType == "Kotlin") {
                        DropdownMenuItem(
                            text = { Text("Format Code") },
                            onClick = { onMoreMenuToggle(false) },
                            leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Version History") },
                        onClick = { onMoreMenuToggle(false) },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun LineNumbers(lineCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(32.dp)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in 1..lineCount) {
            Text(
                text = i.toString(),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                modifier = Modifier.height(20.dp)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val lines = text.lines()
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                else -> {
                    Text(
                        text = SyntaxHighlighter.highlightMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
