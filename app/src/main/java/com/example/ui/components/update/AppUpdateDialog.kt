package com.example.ui.components.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Gold500
import com.example.update.AppUpdateState

/**
 * Modern Islamic M3 themed dialog displaying the current In-App Update flow.
 */
@Composable
fun AppUpdateDialog(
    updateState: AppUpdateState,
    onStartUpdate: () -> Unit,
    onCompleteUpdate: () -> Unit,
    onOpenPlayStore: () -> Unit,
    onDismiss: () -> Unit
) {
    if (updateState is AppUpdateState.Idle) return

    Dialog(
        onDismissRequest = {
            // Prevent dismissing during active downloading
            if (updateState !is AppUpdateState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = updateState !is AppUpdateState.Downloading,
            dismissOnClickOutside = updateState !is AppUpdateState.Downloading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (updateState) {
                    is AppUpdateState.Checking -> {
                        CheckingContent()
                    }

                    is AppUpdateState.UpdateAvailable -> {
                        UpdateAvailableContent(
                            currentVersion = updateState.currentVersionName,
                            onUpdateClick = onStartUpdate,
                            onDismissClick = onDismiss
                        )
                    }

                    is AppUpdateState.UpToDate -> {
                        UpToDateContent(
                            versionName = updateState.versionName,
                            versionCode = updateState.versionCode,
                            onDismiss = onDismiss
                        )
                    }

                    is AppUpdateState.NoInternet -> {
                        NoInternetContent(
                            message = updateState.message,
                            onOpenStore = onOpenPlayStore,
                            onDismiss = onDismiss
                        )
                    }

                    is AppUpdateState.Error -> {
                        ErrorContent(
                            title = updateState.title,
                            message = updateState.message,
                            canOpenStore = updateState.canOpenStoreDirectly,
                            onOpenStore = onOpenPlayStore,
                            onDismiss = onDismiss
                        )
                    }

                    is AppUpdateState.Downloading -> {
                        DownloadingContent(
                            progressPercent = updateState.progressPercent
                        )
                    }

                    is AppUpdateState.Downloaded -> {
                        DownloadedContent(
                            onInstallClick = onCompleteUpdate,
                            onDismissClick = onDismiss
                        )
                    }

                    AppUpdateState.Idle -> Unit
                }
            }
        }
    }
}

@Composable
private fun CheckingContent() {
    StatusIconBadge(
        icon = Icons.Default.Sync,
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))
    CircularProgressIndicator(
        modifier = Modifier.size(36.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "جاري التحقق من وجود تحديث...",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "فحص آخر إصدار رسمي منشور على Google Play",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun UpdateAvailableContent(
    currentVersion: String,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    StatusIconBadge(
        icon = Icons.Default.NewReleases,
        tint = Gold500
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "يتوفر تحديث جديد 🎉",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "توجد نسخة أحدث من التطبيق، قم بالتحديث الآن للحصول على أحدث المميزات والتحسينات.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onDismissClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Text(
                text = "لاحقًا",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onUpdateClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "تحديث الآن",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun UpToDateContent(
    versionName: String,
    versionCode: Long,
    onDismiss: () -> Unit
) {
    StatusIconBadge(
        icon = Icons.Default.CheckCircle,
        tint = Color(0xFF4CAF50)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "أنت تستخدم أحدث إصدار ✅",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "تطبيق القرآن الكريم محدث لآخر نسخة رسمية.\nالإصدار: $versionName (بناء $versionCode)",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )
    Spacer(modifier = Modifier.height(20.dp))
    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Text(
            text = "حسناً",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun NoInternetContent(
    message: String,
    onOpenStore: () -> Unit,
    onDismiss: () -> Unit
) {
    StatusIconBadge(
        icon = Icons.Default.WifiOff,
        tint = Color(0xFFE57373)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "تعذر التحقق من وجود تحديث",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "إغلاق")
        }

        Button(
            onClick = onOpenStore,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "فتح المتجر")
        }
    }
}

@Composable
private fun ErrorContent(
    title: String,
    message: String,
    canOpenStore: Boolean,
    onOpenStore: () -> Unit,
    onDismiss: () -> Unit
) {
    StatusIconBadge(
        icon = Icons.Default.WifiOff,
        tint = Color(0xFFE57373)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "إغلاق")
        }

        if (canOpenStore) {
            Button(
                onClick = onOpenStore,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "صفحة المتجر")
            }
        }
    }
}

@Composable
private fun DownloadingContent(
    progressPercent: Int
) {
    StatusIconBadge(
        icon = Icons.Default.CloudDownload,
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "جاري تنزيل التحديث...",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "$progressPercent% تم تنزيله من Google Play",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    LinearProgressIndicator(
        progress = { progressPercent / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    )
}

@Composable
private fun DownloadedContent(
    onInstallClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    StatusIconBadge(
        icon = Icons.Default.CheckCircle,
        tint = Color(0xFF4CAF50)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "اكتمل تنزيل التحديث 🚀",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "التحديث جاهز للتثبيت، اضغط الآن لإعادة التشغيل وتطبيق التحديث.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onDismissClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "لاحقًا")
        }

        Button(
            onClick = onInstallClick,
            modifier = Modifier.weight(1.3f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "تثبيت الآن",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun StatusIconBadge(
    icon: ImageVector,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
    }
}
