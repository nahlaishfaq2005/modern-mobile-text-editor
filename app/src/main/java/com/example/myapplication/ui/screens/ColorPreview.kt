package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorItem(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp).fillMaxWidth()
    ) {
        Box(modifier = Modifier.size(40.dp).background(color))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name, color = Color.White, fontSize = 18.sp)
    }
}

@Preview
@Composable
fun EditableColorPreview() {
    Column(
        modifier = Modifier
            .background(Color(0xFF0D1B2A)) // App background color
            .padding(16.dp)
            .width(300.dp)
    ) {
        Text("Editable State Color Options", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
        ColorItem("1. Azure Blue", Color(0xFF64B5F6))
        ColorItem("2. Soft Purple", Color(0xFFCE93D8))
        ColorItem("3. Teal", Color(0xFF4DB6AC))
        ColorItem("4. Amber", Color(0xFFFFB300))
        ColorItem("5. Cool Gray", Color(0xFF9AA2AA))
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(20.dp).background(Color(0xFF4CAF50)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reference: 'Saved' Green", color = Color.White, fontSize = 14.sp)
        }
    }
}
