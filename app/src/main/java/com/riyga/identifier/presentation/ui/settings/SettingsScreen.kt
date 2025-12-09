package com.riyga.identifier.presentation.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.riyga.identifier.R
import com.riyga.identifier.data.models.Language
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

    // Language options
    val languages = Language.getAllLanguages()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(languages) { language ->
                LanguageItem(
                    language = language,
                    isSelected = language.code == currentLanguage,
                    onClick = {
                        viewModel.setLanguage(language)
                        updateAppLocale(context, language.code)
                    }
                )
            }
        }
    }
}

@Composable
fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = language.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(id = R.string.selected),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun updateAppLocale(context: Context, languageCode: String) {
    val locale = java.util.Locale(languageCode)
    java.util.Locale.setDefault(locale)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
        localeManager.applicationLocales = LocaleList(locale)
    } else {
        @Suppress("DEPRECATION")
        val config = context.resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}