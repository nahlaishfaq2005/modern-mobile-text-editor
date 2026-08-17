package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.DiffLine
import com.example.myapplication.data.DiffType
import com.example.myapplication.ui.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffViewerScreen(
    oldVersionId: String?,
    newVersionId: String?,
    fileName: String = "",
    onNavigateBack: () -> Unit = {},
    viewModel: EditorViewModel = viewModel()
) {
    val currentFileName by viewModel.currentFileName.collectAsState()
    val versions by viewModel.versions.collectAsState()

    val diffLines by produceState(initialValue = emptyList<DiffLine>(), key1 = oldVersionId, key2 = newVersionId, key3 = currentFileName) {
        value = if (currentFileName != fileName) {
            emptyList()
        } else if (oldVersionId != null && newVersionId != null) {
            viewModel.getDiff(oldVersionId, newVersionId)
        } else if (newVersionId != null) {
            viewModel.getDiffWithCurrent(newVersionId)
        } else {
            emptyList()
        }
    }

    LaunchedEffect(fileName) {
        if (fileName.isNotEmpty()) {
            viewModel.loadFile(fileName)
        }
    }

    var showRestoreDialog by remember { mutableStateOf(false) }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore This Version?") },
            text = { Text("This will create a new version with the content of this version. Your current version will be preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVersionId != null) {
                            viewModel.restoreVersion(newVersionId)
                            onNavigateBack()
                        }
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Diff Viewer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { showRestoreDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restore This Version", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Version selectors (Match UI from prototype)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val oldVersion = versions.find { it.id == oldVersionId }
                val newVersion = versions.find { it.id == newVersionId }

                VersionSelector(
                    label = if (oldVersionId == null) "Current" else "Version ${oldVersion?.versionNumber ?: "..."}",
                    modifier = Modifier.weight(1f)
                )
                VersionSelector(
                    label = if (newVersion != null) "Version ${newVersion.versionNumber}" else "Compare",
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (diffLines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No differences found", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    items(diffLines) { line ->
                        DiffLineItem(line)
                    }
                }
            }
        }
    }
}

@Composable
fun VersionSelector(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DiffLineItem(line: DiffLine) {
    val backgroundColor = when (line.type) {
        DiffType.ADDED -> Color(0xFF1B5E20).copy(alpha = 0.2f)
        DiffType.REMOVED -> Color(0xFFB71C1C).copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    
    val indicatorColor = when (line.type) {
        DiffType.ADDED -> Color(0xFF4CAF50)
        DiffType.REMOVED -> Color(0xFFF44336)
        else -> Color.Transparent
    }

    val prefix = when (line.type) {
        DiffType.ADDED -> "+"
        DiffType.REMOVED -> "-"
        else -> " "
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 2.dp)
    ) {
        // Line number or indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(indicatorColor)
        )
        
        Text(
            text = line.lineNumber?.toString() ?: "",
            modifier = Modifier.width(32.dp).padding(start = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        
        Text(
            text = prefix,
            modifier = Modifier.width(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = indicatorColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = line.content,
            style = MaterialTheme.typography.bodySmall,
            color = if (line.type == DiffType.UNCHANGED) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}
