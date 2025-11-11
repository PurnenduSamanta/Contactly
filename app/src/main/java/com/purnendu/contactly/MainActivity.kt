package com.purnendu.contactly

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.screens.schedule.SchedulesScreen
import com.purnendu.contactly.ui.theme.ContactlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactlyTheme {
                val schedules = remember {
                    mutableStateListOf(
                        Schedule(
                            id = "1",
                            name = "Ethan Carter",
                            originalName = "Ethan",
                            avatarResId = R.drawable.avatar_ethan
                        ),
                        Schedule(
                            id = "2",
                            name = "Sophia Clark",
                            originalName = "Sophia",
                            avatarResId = R.drawable.avatar_sophia
                        ),
                        Schedule(
                            id = "3",
                            name = "Liam Walker",
                            originalName = "Liam",
                            avatarResId = R.drawable.avatar_liam
                        )
                    )
                }
                
                SchedulesScreen(
                    schedules = schedules,
                    onEditClick = { 
                        Toast.makeText(this, "Edit ${it.name}", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteClick = {
                        Toast.makeText(this, "Delete ${it.name}", Toast.LENGTH_SHORT).show()
                    },
                    onAddClick = {
                        Toast.makeText(this, "Add Schedule", Toast.LENGTH_SHORT).show()
                    },
                    onHomeClick = {
                        Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                    },
                    onSettingsClick = {
                        Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SchedulesScreenPreview() {
    ContactlyTheme {
        SchedulesScreen(
            schedules = listOf(
                Schedule(
                    id = "1",
                    name = "Ethan Carter",
                    originalName = "Ethan",
                    avatarResId = R.drawable.avatar_ethan
                ),
                Schedule(
                    id = "2",
                    name = "Sophia Clark",
                    originalName = "Sophia",
                    avatarResId = R.drawable.avatar_sophia
                ),
                Schedule(
                    id = "3",
                    name = "Liam Walker",
                    originalName = "Liam",
                    avatarResId = R.drawable.avatar_liam
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