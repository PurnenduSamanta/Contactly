package com.purnendu.contactly.ui.screens.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.ui.screens.setting.components.SettingsRow
import com.purnendu.contactly.ui.screens.setting.components.ThemeChip
import com.purnendu.contactly.ui.theme.SettingSectionTitle
import com.purnendu.contactly.utils.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SettingSectionTitle
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Companion.White)
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier.Companion
                .padding(padding)
                .fillMaxSize()
                .background(Color.Companion.White)
        ) {

            item {
                // Appearance Section Label
                Text(
                    "Appearance",
                    color = SettingSectionTitle,
                    fontWeight = FontWeight.Companion.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.Companion.padding(
                        start = 16.dp,
                        top = 16.dp,
                        bottom = 10.dp
                    )
                )
            }

            item {
                Row(
                    modifier = Modifier.Companion.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeChip("Light", currentTheme == AppThemeMode.LIGHT) {
                        onThemeChange(AppThemeMode.LIGHT)
                    }
                    ThemeChip("Dark", currentTheme == AppThemeMode.DARK) {
                        onThemeChange(AppThemeMode.DARK)
                    }
                    ThemeChip("System default", currentTheme == AppThemeMode.SYSTEM) {
                        onThemeChange(AppThemeMode.SYSTEM)
                    }
                }
            }

            item {
                Text(
                    "About",
                    color = SettingSectionTitle,
                    fontWeight = FontWeight.Companion.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.Companion.padding(start = 16.dp, top = 28.dp, bottom = 6.dp)
                )
            }

            // Version Row
            item {
                SettingsRow(name = "Version", value = "1.0.0", onClick = null)
            }

            // Privacy Policy Row
            item {
                SettingsRow(name = "Privacy Policy", value = null) { onPrivacyPolicyClick() }
            }

            // Terms Row
            item {
                SettingsRow(name = "Terms of Service", value = null) { onTermsClick() }
            }
        }
    }
}


@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        currentTheme = AppThemeMode.LIGHT,
        onThemeChange = {},
        onBack = {},
        onPrivacyPolicyClick = {},
        onTermsClick = {}
    )
}