/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.tamed.music.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tamed.music.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarDialog(
    onDismissRequest: () -> Unit,
    onStar: () -> Unit,
    onLater: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Support development", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(
                    text = "If you enjoy using Tamed, you can support the project by giving it a star. Thanks for being part of the journey and helping the app keep improving.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://tamed.app")
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.telegram),
                    contentDescription = "Telegram",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Telegram")
            }
            FilledTonalButton(
                onClick = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://tamed.app")
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onStar()
                },
                colors = ButtonDefaults.buttonColors(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.star),
                    contentDescription = "Star",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Star")
            }
        },
        dismissButton = {
            TextButton(onClick = onLater, shapes = ButtonDefaults.shapes()) {
                Text(text = "Later")
            }
        }
    )
}
