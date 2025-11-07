package com.purnendu.contactly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.components.ScheduleItem
import com.purnendu.contactly.ui.theme.AuroMetalSaurus
import com.purnendu.contactly.ui.theme.ChineseBlack
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.theme.Crayola

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
            CenterAlignedTopAppBar(
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
            if (schedules.isNotEmpty())
            {
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    containerColor = Crayola,
                    contentColor = Color.White
                )
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "Add Schedule")

                        Spacer(modifier = Modifier.width(10.dp))

                        Text("Add Schedule", color = Color.White, fontWeight = FontWeight.Bold)

                    }
                }
            }


        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally)
                {

                    Box(modifier = Modifier.background(color = Color.Gray.copy(alpha = 0.1f), shape = CircleShape).padding(30.dp), contentAlignment = Alignment.Center)
                    {

                        Icon(modifier = Modifier.size(40.dp), painter = painterResource(R.drawable.calendar_month), contentDescription = "calender",)


                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("No Schedules yet!", fontWeight = FontWeight.Bold, color = ChineseBlack)

                    Spacer(modifier = Modifier.height(5.dp))

                    Text("Get started by creating your first schedule", color = AuroMetalSaurus)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.background(color = Crayola, shape = RoundedCornerShape(10.dp)).fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add Schedule", tint = Color.White)

                        Spacer(modifier = Modifier.width(10.dp))

                        Text("Add Schedule", color = Color.White, fontWeight = FontWeight.Bold)

                    }



                }





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