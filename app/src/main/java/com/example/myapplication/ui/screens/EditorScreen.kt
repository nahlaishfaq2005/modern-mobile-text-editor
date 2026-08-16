package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.editor.EditorSidebar
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
    val isReadOnly by viewModel.isReadOnly.collectAsState()
    val recentFiles by homeViewModel.recentFiles.collectAsState()
    
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(showSearchReplace) {
        if (showSearchReplace && !isReplaceMode) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(currentIndex, searchResults, viewportHeight, textLayoutResult) {
        if (currentIndex != -1 && currentIndex < searchResults.size && viewportHeight > 0) {
            val range = searchResults[currentIndex]
            textLayoutResult?.let { layout ->
                try {
                    val matchRect = layout.getBoundingBox(range.first)
                    val matchCenterY = matchRect.center.y
                    
                    // Create a rect centered on the match with a portion of viewport height to center it
                    // Using viewportHeight / 2.5f to provide a good "visible context" around the match
                    val scrollRect = Rect(
                        left = matchRect.left,
                        top = matchCenterY - (viewportHeight / 2.5f),
                        right = matchRect.right,
                        bottom = matchCenterY + (viewportHeight / 2.5f)
                    )
                    
                    bringIntoViewRequester.bringIntoView(scrollRect)
                } catch (_: Exception) {
                    // Ignore if layout is not yet updated for the current content
                }
            }
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
                    onFileClick = { _ ->
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
                        onSaveClick = { viewModel.saveFile(fileName) },
                        onFormatClick = { viewModel.formatCode() },
                        isReadOnly = isReadOnly,
                        onToggleReadOnly = { viewModel.toggleReadOnly() }
                    )
                    EditorToolbar(
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onWordWrapToggle = { viewModel.toggleWordWrap() },
                        isWordWrapEnabled = isWordWrapEnabled,
                        onFormatClick = { viewModel.formatCode() },
                        onSearchClick = { viewModel.toggleSearchReplace(false) },
                        onReplaceClick = { if (!isReadOnly) viewModel.toggleSearchReplace(true) },
                        onSaveClick = { viewModel.saveFile(fileName) },
                        showFormat = fileType != "Markdown",
                        enabled = !isReadOnly
                    )
                    
                        if (showSearchReplace) {
                        EditorSearchReplacePanel(
                            isReplaceMode = isReplaceMode,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                            replaceQuery = replaceQuery,
                            onReplaceQueryChange = { viewModel.onReplaceQueryChange(it) },
                            currentIndex = currentIndex,
                            totalCount = searchResults.size,
                            onNext = { viewModel.nextSearchResult() },
                            onPrevious = { viewModel.previousSearchResult() },
                            onReplace = { if (!isReadOnly) viewModel.replaceNext() },
                            onReplaceAll = { if (!isReadOnly) viewModel.replaceAll() },
                            onClose = { viewModel.toggleSearchReplace(false) },
                            focusRequester = focusRequester,
                            enabled = !isReadOnly
                        )
                    }

                    if (fileType == "Markdown") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { isPreviewMode = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isPreviewMode) Color(0xFFA56F63) else Color.Transparent,
                                    contentColor = if (!isPreviewMode) Color.White else Color.Gray
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Editor", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = { isPreviewMode = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPreviewMode) Color(0xFFA56F63) else Color.Transparent,
                                    contentColor = if (isPreviewMode) Color.White else Color.Gray
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Preview", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    EditorStatusBar(
                        line = 11,
                        col = 25,
                        fileType = fileType,
                        saveStatus = saveStatus,
                        isReadOnly = isReadOnly
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
                    .onGloballyPositioned { viewportHeight = it.size.height }
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
            ) {
                if (fileType == "Markdown" && isPreviewMode) {
                    MarkdownPreview(content.text)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        LineNumbers(content.text.lines().size, textLayoutResult)
                        
                        val visualTransformation = remember(fileType, searchQuery, searchResults, currentIndex) {
                            VisualTransformation { text ->
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
                                TransformedText(searchHighlighted, OffsetMapping.Identity)
                            }
                        }

                        BasicTextField(
                            value = content,
                            onValueChange = { if (!isReadOnly) viewModel.onContentChange(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .then(
                                    if (!isWordWrapEnabled) {
                                        Modifier.horizontalScroll(horizontalScrollState)
                                    } else {
                                        Modifier
                                    }
                                )
                                .bringIntoViewRequester(bringIntoViewRequester),
                            readOnly = isReadOnly,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            visualTransformation = visualTransformation,
                            onTextLayout = { textLayoutResult = it },
                            singleLine = false,
                            maxLines = if (isWordWrapEnabled) Int.MAX_VALUE else Int.MAX_VALUE // maxLines doesn't control wrapping, Modifier.horizontalScroll does
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
    onSaveClick: () -> Unit,
    onFormatClick: () -> Unit,
    isReadOnly: Boolean,
    onToggleReadOnly: () -> Unit
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
                            imageVector = Icons.AutoMirrored.Filled.Notes,
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
                            color = Color.White
                        )
                        if (isReadOnly) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFA56F63).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFA56F63).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFA56F63),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Read-Only",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFFA56F63)
                                    )
                                }
                            }
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
                            onClick = { 
                                onFormatClick()
                                onMoreMenuToggle(false) 
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = if (isReadOnly) "Make Editable" else "Make Read-Only",
                                color = if (isReadOnly) Color(0xFF4CAF50) else Color.Unspecified
                            ) 
                        },
                        onClick = { 
                            onToggleReadOnly()
                            onMoreMenuToggle(false) 
                        },
                        leadingIcon = { 
                            Icon(
                                if (isReadOnly) Icons.Default.LockOpen else Icons.Default.Lock, 
                                contentDescription = null,
                                tint = if (isReadOnly) Color(0xFF4CAF50) else LocalContentColor.current
                            ) 
                        }
                    )
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
    onWordWrapToggle: () -> Unit,
    isWordWrapEnabled: Boolean,
    onFormatClick: () -> Unit,
    onSearchClick: () -> Unit,
    onReplaceClick: () -> Unit,
    onSaveClick: () -> Unit,
    showFormat: Boolean = true,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUndo, enabled = enabled) {
            Icon(
                Icons.AutoMirrored.Filled.Undo, 
                contentDescription = "Undo", 
                tint = if (enabled) Color.LightGray else Color.DarkGray
            )
        }
        IconButton(onClick = onRedo, enabled = enabled) {
            Icon(
                Icons.AutoMirrored.Filled.Redo, 
                contentDescription = "Redo", 
                tint = if (enabled) Color.LightGray else Color.DarkGray
            )
        }
        
        IconButton(onClick = onWordWrapToggle) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.WrapText,
                contentDescription = "Word Wrap",
                tint = if (isWordWrapEnabled) Color(0xFFA56F63) else Color.LightGray
            )
        }

        if (showFormat) {
            IconButton(onClick = onFormatClick, enabled = enabled) {
                Icon(
                    imageVector = Icons.Default.DataObject,
                    contentDescription = "Format Code",
                    tint = if (enabled) Color.LightGray else Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray)
        }
        IconButton(onClick = onReplaceClick, enabled = enabled) {
            Icon(
                Icons.Default.SwapHoriz, 
                contentDescription = "Replace", 
                tint = if (enabled) Color.LightGray else Color.DarkGray
            )
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
fun LineNumbers(lineCount: Int, textLayoutResult: TextLayoutResult?) {
    Column(
        modifier = Modifier
            .width(32.dp)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (textLayoutResult == null) {
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
        } else {
            val text = textLayoutResult.layoutInput.text.text
            var currentDocLine = 1
            
            for (i in 0 until textLayoutResult.lineCount) {
                val lineStartOffset = textLayoutResult.getLineStart(i)
                val isNewDocLine = i == 0 || (lineStartOffset > 0 && text[lineStartOffset - 1] == '\n')
                
                if (isNewDocLine) {
                    Text(
                        text = currentDocLine.toString(),
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                    currentDocLine++
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun EditorStatusBar(line: Int, col: Int, fileType: String, saveStatus: String, isReadOnly: Boolean) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isReadOnly) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isReadOnly) Color(0xFFA56F63) else Color(0xFF4CAF50),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isReadOnly) "Read-Only" else "Editable",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isReadOnly) Color(0xFFA56F63) else Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(12.dp))
//                Text(
//                    text = "Ln $line    Col $col    UTF-8",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//                )
            }
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
fun EditorSearchReplacePanel(
    isReplaceMode: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    currentIndex: Int,
    totalCount: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean = true
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Row
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
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
                
                if (searchQuery.isNotEmpty()) {
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

            if (isReplaceMode) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Replace Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    BasicTextField(
                        value = replaceQuery,
                        onValueChange = onReplaceQueryChange,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (replaceQuery.isEmpty()) {
                                    Text(
                                        "Replace with...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    IconButton(onClick = { onReplaceQueryChange("") }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.HighlightOff, 
                            contentDescription = "Clear", 
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onReplace,
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            disabledContentColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Replace", style = MaterialTheme.typography.labelLarge)
                    }
                    
                    Button(
                        onClick = onReplaceAll,
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA56F63), // Theme accent color
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFA56F63).copy(alpha = 0.1f),
                            disabledContentColor = Color.Gray
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Replace All", style = MaterialTheme.typography.labelLarge)
                    }
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
        var inCodeBlock = false
        val codeBlockContent = StringBuilder()
        
        lines.forEach { line ->
            when {
                line.startsWith("```") -> {
                    if (inCodeBlock) {
                        // End of code block
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = codeBlockContent.toString().trim(),
                                modifier = Modifier.padding(12.dp),
                                style = TextStyle(
                                    color = Color(0xFFD99B7F),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            )
                        }
                        codeBlockContent.clear()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                }
                inCodeBlock -> {
                    codeBlockContent.append(line).append("\n")
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                line.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = renderMarkdownText(line.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text("•", color = Color.White, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = renderMarkdownText(line.trim().substring(2)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                line.trim().firstOrNull()?.isDigit() == true && line.contains(". ") -> {
                    val dotIndex = line.indexOf(". ")
                    if (dotIndex != -1) {
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text(line.substring(0, dotIndex + 1), color = Color.White, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = renderMarkdownText(line.substring(dotIndex + 2)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    } else {
                        Text(
                            text = renderMarkdownText(line),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = renderMarkdownText(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun renderMarkdownText(text: String): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(_.*?_)|(~~.*?~~)|(`.*?`)|(\\[.*?\\]\\(.*?\\))")
        var lastMatchEnd = 0
        
        regex.findAll(text).forEach { match ->
            append(text.substring(lastMatchEnd, match.range.first))
            
            val matchText = match.value
            when {
                matchText.startsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append(matchText.removeSurrounding("**"))
                    }
                }
                matchText.startsWith("~~") -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color.Gray)) {
                        append(matchText.removeSurrounding("~~"))
                    }
                }
                matchText.startsWith("*") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.9f))) {
                        append(matchText.removeSurrounding("*"))
                    }
                }
                matchText.startsWith("_") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = 0.9f))) {
                        append(matchText.removeSurrounding("_"))
                    }
                }
                matchText.startsWith("`") -> {
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF4CAF50),
                            background = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            fontFamily = FontFamily.Monospace
                        )
                    ) {
                        append(matchText.removeSurrounding("`"))
                    }
                }
                matchText.startsWith("[") -> {
                    val linkText = matchText.substringAfter("[").substringBefore("]")
                    withStyle(SpanStyle(color = Color(0xFF64B5F6), textDecoration = TextDecoration.Underline)) {
                        append(linkText)
                    }
                }
                else -> append(matchText)
            }
            lastMatchEnd = match.range.last + 1
        }
        append(text.substring(lastMatchEnd))
    }
}

@Composable
fun RowScope.InfoStep(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun MarkdownFeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
