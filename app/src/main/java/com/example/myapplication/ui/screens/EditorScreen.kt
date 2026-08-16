package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
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
    val isReplaceMode by viewModel.isReplaceMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val currentIndex by viewModel.currentResultIndex.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val recentFiles by homeViewModel.recentFiles.collectAsState()
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(showSearchReplace) {
        if (showSearchReplace && !isReplaceMode) {
            focusRequester.requestFocus()
        }
    }
    
    LaunchedEffect(fileName) {
        viewModel.loadFile(fileName)
    }
    
    var isPreviewMode by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                        onMenuClick = { scope.launch { drawerState.open() } },
                        showMoreMenu = showMoreMenu,
                        onMoreMenuToggle = { showMoreMenu = it },
                        onSaveClick = { viewModel.saveFile(fileName) }
                    )
                    EditorToolbar(
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onFormatClick = { /* Format logic */ },
                        onSearchClick = { viewModel.toggleSearchReplace(false) },
                        onReplaceClick = { viewModel.toggleSearchReplace(true) },
                        onSaveClick = { viewModel.saveFile(fileName) }
                    )
                    
                    if (showSearchReplace && !isReplaceMode) {
                        EditorSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            currentIndex = currentIndex,
                            totalCount = searchResults.size,
                            onNext = { viewModel.nextSearchResult() },
                            onPrevious = { viewModel.previousSearchResult() },
                            onClose = { viewModel.toggleSearchReplace(false) },
                            focusRequester = focusRequester
                        )
                    }
                }
            },
            bottomBar = {
                Column {
                    if (showSearchReplace && isReplaceMode) {
                        // Keep old replace logic for now if it's replace mode, 
                        // but the user said "Update only the Search UI for now"
                        // I'll hide the replace UI from bottom bar if it was there?
                        // Actually replace was a BottomSheet. 
                    }
                    EditorStatusBar(
                        line = 11,
                        col = 25,
                        fileType = fileType,
                        saveStatus = saveStatus
                    )
                    HomeBottomNavigation(
                        currentRoute = "editor",
                        onHomeClick = onNavigateBack,
                        onEditorClick = { /* Already in editor */ },
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
                                
                                val searchHighlighted = buildAnnotatedString {
                                    append(highlighted)
                                    if (searchQuery.isNotEmpty()) {
                                        searchResults.forEachIndexed { index, range ->
                                            val isCurrent = index == currentIndex
                                            addStyle(
                                                SpanStyle(
                                                    background = if (isCurrent) 
                                                        Color(0xFFA56F63) // PrimaryAccent for active
                                                    else 
                                                        Color(0xFFA56F63).copy(alpha = 0.3f), // Transparent accent for others
                                                    color = if (isCurrent) Color.White else Color.Unspecified
                                                ),
                                                range.first,
                                                range.last
                                            )
                                        }
                                    }
                                }
                                
                                TransformedText(
                                    searchHighlighted,
                                    OffsetMapping.Identity
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
    onMenuClick: () -> Unit,
    showMoreMenu: Boolean,
    onMoreMenuToggle: (Boolean) -> Unit,
    onSaveClick: () -> Unit
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
                        tint = Color.White,
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
            Box {
                IconButton(onClick = { onMoreMenuToggle(true) }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { onMoreMenuToggle(false) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
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
fun EditorToolbar(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFormatClick: () -> Unit,
    onSearchClick: () -> Unit,
    onReplaceClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUndo) {
            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.LightGray)
        }
        IconButton(onClick = onRedo) {
            Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color.LightGray)
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        IconButton(onClick = onFormatClick) {
            Icon(Icons.Default.PlaylistAdd, contentDescription = "Format", tint = Color.LightGray)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray)
        }
        IconButton(onClick = onReplaceClick) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Replace", tint = Color.LightGray)
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        VerticalDivider(
            modifier = Modifier.height(24.dp),
            color = Color.LightGray.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(onClick = onSaveClick) {
            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.LightGray)
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
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentIndex: Int,
    totalCount: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp), // More rounded like the home search bar
        border = BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Search text...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (totalCount > 0) "${currentIndex + 1} / $totalCount" else "0 / 0",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowUp, 
                        contentDescription = "Previous", 
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, 
                        contentDescription = "Next", 
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )
                
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Close", 
                        tint = MaterialTheme.colorScheme.primary
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
