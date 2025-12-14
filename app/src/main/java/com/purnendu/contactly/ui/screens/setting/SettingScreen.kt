package com.purnendu.contactly.ui.screens.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purnendu.contactly.ui.screens.setting.components.SettingsRow
import com.purnendu.contactly.ui.screens.setting.components.ThemeChip
import com.purnendu.contactly.ui.screens.setting.components.ViewModeToggle
import com.purnendu.contactly.utils.AppThemeMode
import androidx.compose.ui.res.stringResource
import com.purnendu.contactly.R
import com.purnendu.contactly.ui.theme.ContactlyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit
) {

    val themeMode by settingsViewModel.theme.collectAsState()
    val viewMode by settingsViewModel.viewMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.title_settings),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            item {
                // Appearance Section Label
                Text(
                    stringResource(id = R.string.section_appearance),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        bottom = 10.dp
                    )
                )
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeChip(
                        stringResource(id = R.string.theme_light),
                        themeMode == AppThemeMode.LIGHT
                    ) {
                        settingsViewModel.setTheme(AppThemeMode.LIGHT)
                    }
                    ThemeChip(
                        stringResource(id = R.string.theme_dark),
                        themeMode == AppThemeMode.DARK
                    ) {
                        settingsViewModel.setTheme(AppThemeMode.DARK)
                    }
                    ThemeChip(
                        stringResource(id = R.string.theme_system),
                        themeMode == AppThemeMode.SYSTEM
                    ) {
                        settingsViewModel.setTheme(AppThemeMode.SYSTEM)
                    }
                }
            }

            // View Mode Section
            item {
                Text(
                    "View Mode",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 28.dp,
                        bottom = 10.dp
                    )
                )
            }
            
            item {
                ViewModeToggle(
                    selectedMode = viewMode,
                    onModeChange = { settingsViewModel.setViewMode(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Text(
                    stringResource(id = R.string.section_about),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 6.dp)
                )
            }

            // Version Row
            item {
                SettingsRow(
                    name = stringResource(id = R.string.row_version),
                    value = "1.0.0",
                    onClick = null
                )
            }

            // Privacy Policy Row
            item {
                SettingsRow(
                    name = stringResource(id = R.string.row_privacy_policy),
                    value = null
                ) { onPrivacyPolicyClick() }
            }

            // Terms Row
            item {
                SettingsRow(
                    name = stringResource(id = R.string.row_terms),
                    value = null
                ) { onTermsClick() }
            }
        }
    }
}


@Preview
@Composable
fun SettingsScreenPreview() {
    ContactlyTheme {
        SettingsScreen(
            settingsViewModel = viewModel(),
            onPrivacyPolicyClick = {},
            onTermsClick = {}
        )
    }
}