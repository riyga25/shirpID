package by.riyga.shirpid.presentation.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.riyga.shirpid.presentation.R
import androidx.core.net.toUri
import by.riyga.shirpid.presentation.utils.LocalNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen() {
    val navController = LocalNavController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.birdnet_logo),
                    contentDescription = "birdnet logo",
                    modifier = Modifier.size(120.dp)
                )
                LicenseText()
            }
        }
    }
}

@Composable
fun LicenseText() {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary

    // Аннотированный текст
    val annotatedText = buildAnnotatedString {
        append("This application includes unmodified ")

        pushStringAnnotation(
            tag = "birdnet",
            annotation = "https://birdnet-team.github.io/BirdNET-Analyzer/"
        )
        withStyle(style = SpanStyle(color = linkColor)) {
            append("BirdNET")
        }
        pop()

        append(" TensorFlow models.\nThe BirdNET models are licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (CC BY-NC-SA 4.0).\n")

        pushStringAnnotation(
            tag = "license",
            annotation = "https://creativecommons.org/licenses/by-nc-sa/4.0/"
        )
        withStyle(style = SpanStyle(color = linkColor)) {
            append("https://creativecommons.org/licenses/by-nc-sa/4.0/")
        }
        pop()
    }

    ClickableText(
        text = annotatedText,
        style = TextStyle(
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            lineHeight = 18.sp
        ),
        onClick = { offset ->
            annotatedText.getStringAnnotations(start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
        }
    )
}