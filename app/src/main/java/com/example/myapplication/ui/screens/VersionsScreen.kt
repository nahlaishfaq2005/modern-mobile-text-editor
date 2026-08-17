package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.Version
import com.example.myapplication.ui.viewmodel.EditorViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionsScreen(
    fileName: String = "MainActivity.kt",
    onNavigateBack: () -> Unit = {},
    onViewVersion: (String) -> Unit = {},
    onCompareVersions: (String, String) -> Unit = { _, _ -> },
    onNavigateToHome: () -> Unit = {},
    onNavigateToEditor: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onRestore: () -> Unit = {},
    viewModel: EditorViewModel = viewModel()
) {
    var versions by remember { mutableStateOf<List<Version>>(emptyList()) }
    val originalVersions by viewModel.versions.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<Version?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    var sortOrder by remember { mutableStateOf("Newest First") }
    var filterType by remember { mutableStateOf("All Versions") }
    
    LaunchedEffect(originalVersions, sortOrder, filterType) {
        var filtered = originalVersions.filter {
            when (filterType) {
                "Manually Saved" -> !it.isAutoSaved
                "Auto Saved" -> it.isAutoSaved
                else -> true
            }
        }
        
        filtered = when (sortOrder) {
            "Newest First" -> filtered.sortedByDescending { it.timestamp }
            "Oldest First" -> filtered.sortedBy { it.timestamp }
            else -> filtered
        }
        
        versions = filtered
    }
    
    var selectionMode by remember { mutableStateOf(false) }
    val selectedVersions = remember { mutableStateListOf<String>() }
    
    LaunchedEffect(fileName) {
        viewModel.loadFile(fileName)
    }
    
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Version?") },
            text = { Text("This action cannot be undone. The version will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVersion(showDeleteDialog!!)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf("Newest First", "Oldest First").forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortOrder = order }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortOrder == order, onClick = { sortOrder = order })
                        Text(order, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Filter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf("All Versions", "Manually Saved", "Auto Saved").forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filterType = type }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = filterType == type, onClick = { filterType = type })
                        Text(type, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3A487), contentColor = Color.Black)
                ) {
                    Text("Apply")
                }
            }
        }
    }

    if (showRenameDialog != null) {
        var newName by remember { mutableStateOf(showRenameDialog!!.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Version") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameVersion(showRenameDialog!!.id, newName)
                        showRenameDialog = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf("Newest First", "Oldest First").forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortOrder = order }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortOrder == order, onClick = { sortOrder = order })
                        Text(order, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Filter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf("All Versions", "Manually Saved", "Auto Saved").forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filterType = type }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = filterType == type, onClick = { filterType = type })
                        Text(type, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3A487), contentColor = Color.Black)
                ) {
                    Text("Apply")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${versions.size} Versions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = { 
                            if (selectedVersions.size == 2) {
                                onCompareVersions(selectedVersions[0], selectedVersions[1])
                            }
                        }, enabled = selectedVersions.size == 2) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Compare", tint = if (selectedVersions.size == 2) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { 
                            selectionMode = false
                            selectedVersions.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        IconButton(onClick = { selectionMode = true }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Select", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { /* Search versions */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            HomeBottomNavigation(
                currentRoute = "versions",
                onHomeClick = onNavigateToHome,
                onEditorClick = onNavigateToEditor,
                onVersionsClick = { /* Already in versions */ },
                onSettingsClick = onNavigateToSettings
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (versions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).padding(bottom = 16.dp),
                            tint = Color.DarkGray
                        )
                        Text("No versions yet", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Start editing and save your file to see versions here.", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onNavigateToEditor,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3A487), contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back to Editor")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(versions) { version ->
                        VersionItem(
                            version = version,
                            isCurrent = version.isCurrent,
                            isSelected = selectedVersions.contains(version.id),
                            selectionMode = selectionMode,
                            onClick = { 
                                if (selectionMode) {
                                    if (selectedVersions.contains(version.id)) {
                                        selectedVersions.remove(version.id)
                                    } else if (selectedVersions.size < 2) {
                                        selectedVersions.add(version.id)
                                    }
                                } else {
                                    onViewVersion(version.id)
                                }
                            },
                            onDelete = { showDeleteDialog = version.id },
                            onRename = { showRenameDialog = version },
                            onCompare = { onCompareVersions(version.id, versions.find { it.isCurrent }?.id ?: versions.first().id) },
                            onRestore = { 
                                viewModel.restoreVersion(version.id)
                                onRestore()
                            },
                            allVersions = versions
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VersionItem(
    version: Version,
    isCurrent: Boolean,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onCompare: () -> Unit,
    onRestore: () -> Unit,
    allVersions: List<Version> = emptyList()
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val timeString = dateFormat.format(Date(version.timestamp))
    var showMenu by remember { mutableStateOf(false) }
    
    val restoredFromVersion = remember(version.restoreSourceVersionId) {
        allVersions.find { it.id == version.restoreSourceVersionId }
    }
    
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                // Timeline connector as seen in prototype
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .border(2.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            .padding(2.dp)
                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Version ${version.versionNumber}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Current",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Text(
                    text = "Today, $timeString • 2.4 KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (restoredFromVersion != null) {
                    Text(
                        text = "Restored from Version ${restoredFromVersion.versionNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else if (version.restoreSourceVersionId != null) {
                    Text(
                        text = "Restored from Version ${version.restoreSourceVersionId.take(4)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (!version.name.isNullOrEmpty() || version.isAutoSaved) {
                    Text(
                        text = version.name ?: "Auto-saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF2A2A2A))
                ) {
                    DropdownMenuItem(
                        text = { Text("Restore", color = Color.White) },
                        onClick = { 
                            showMenu = false
                            onRestore()
                        },
                        leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("Compare with Current", color = Color.White) },
                        onClick = { 
                            showMenu = false
                            onCompare()
                        },
                        leadingIcon = { Icon(Icons.Default.Compare, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Version", color = Color.White) },
                        onClick = { 
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Version", color = Color.Red) },
                        onClick = { 
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}
