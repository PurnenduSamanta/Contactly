package com.purnendu.contactly.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.screens.schedule.components.ScheduleItem
import com.purnendu.contactly.ui.theme.Crayola
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.theme.Crayola

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    modifier: Modifier = Modifier,
    schedules: List<Schedule>,
    resolveAvatar: (Long) -> String? = { _ -> null },
    onEditClick: (Schedule) -> Unit,
    onDeleteClick: (Schedule) -> Unit,
    onAddClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_schedules),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Companion.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (schedules.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                {
                    Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.action_add_schedule))

                        Spacer(modifier = Modifier.Companion.width(10.dp))

                        Text(
                            "Add Schedule",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Companion.Bold
                        )

                    }
                }
            }


        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp),
                contentAlignment = Alignment.Companion.Center
            ) {

                Column(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalAlignment = Alignment.Companion.CenterHorizontally
                )
                {

                    Box(
                        modifier = Modifier.Companion.background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ).padding(30.dp), contentAlignment = Alignment.Companion.Center
                    )
                    {

                        Icon(
                            modifier = Modifier.Companion.size(40.dp),
                            painter = painterResource(R.drawable.calendar_month),
                            contentDescription = "calender",
                        )


                    }

                    Spacer(modifier = Modifier.Companion.height(10.dp))

                    Text(
                        stringResource(id = R.string.empty_no_schedules),
                        fontWeight = FontWeight.Companion.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.Companion.height(5.dp))

                    Text(stringResource(id = R.string.empty_get_started), color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.Companion.height(10.dp))

                    Row(
                        modifier = Modifier.Companion.background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        ).fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp).clickable { onAddClick() },
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Schedule",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.Companion.width(10.dp))

                        Text(
                            stringResource(id = R.string.action_add_schedule),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Companion.Bold
                        )

                    }


                }


            }
        } else {
            LazyColumn(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(schedules) { schedule ->
                    ScheduleItem(
                        schedule = schedule,
                        avatarUri = schedule.contactId?.let { resolveAvatar(it) },
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
    ContactlyTheme(appThemeMode = com.purnendu.contactly.utils.AppThemeMode.LIGHT) {
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
