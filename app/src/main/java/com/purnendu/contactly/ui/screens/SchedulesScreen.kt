package com.purnendu.contactly.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.components.ScheduleItem
import com.purnendu.contactly.ui.theme.ChineseBlack
import com.purnendu.contactly.ui.theme.ContactlyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    schedules: List<Schedule>,
    onEditClick: (Schedule) -> Unit,
    onDeleteClick: (Schedule) -> Unit,
    onAddClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) { 
    Scaffold( 
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Schedules",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = ChineseBlack
                )
            )
        },
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No schedules available",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(schedules) { schedule ->
                    ScheduleItem(
                        schedule = schedule,
                        onEditClick = { onEditClick(schedule) },
                        onDeleteClick = { onDeleteClick(schedule) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun SchedulesScreenPreview() {
    ContactlyTheme {
        SchedulesScreen(
            schedules = listOf(
                Schedule(
                    id = "1",
                    name = "Ethan Carter",
                    originalName = "Ethan"
                ),
                Schedule(
                    id = "2",
                    name = "Sophia Clark",
                    originalName = "Sophia"
                ),
                Schedule(
                    id = "3",
                    name = "Liam Walker",
                    originalName = "Liam"
                )
            ),
            onEditClick = {},
            onDeleteClick = {},
            onAddClick = {},
            onHomeClick = {},
            onSettingsClick = {}
        )
    }
}