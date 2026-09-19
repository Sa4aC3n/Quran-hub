package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioQualityLevel
import com.example.data.model.PlayerState
import com.example.data.model.Reciter
import com.example.data.model.Surah
import com.example.data.model.SurahAudioItem
import com.example.haram.HaramLocation
import com.example.haram.HaramNowPlaying
import com.example.ui.components.IslamicGeometricBackground
import com.example.ui.components.IslamicGeometricCornerDecorations
import com.example.ui.components.IslamicGradientCard
import com.example.ui.components.IslamicOrnamentCard
import com.example.ui.components.IslamicOrnamentalDivider
import com.example.ui.components.ReciterAvatar
import com.example.ui.components.haram.HaramLiveBannerCard
import com.example.ui.components.haram.IslamicCornerOrnaments
import com.example.i18n.Language
import com.example.i18n.LocaleManager
import com.example.ui.components.LanguageHeaderButton
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold600
import com.example.ui.theme.IslamicLightPrimary
import com.example.ui.theme.IslamicLightPrimaryContainer
import com.example.ui.theme.IslamicTheme

@Composable
fun HomeScreen(
    reciters: List<Reciter>,
    surahs: List<Surah>,
    favoriteReciterIds: Set<String>,
    favoriteSurahNumbers: Set<Int>,
    lastPlayedItem: SurahAudioItem?,
    lastPlayedPositionMs: Long,
    playerState: PlayerState,
    preferredQuality: AudioQualityLevel,
    makkahNowPlaying: HaramNowPlaying? = null,
    madinahNowPlaying: HaramNowPlaying? = null,
    isHaramNotificationsEnabled: Boolean = false,
    onPlayLiveHaram: (HaramLocation) -> Unit = {},
    onOpenHaramSettings: () -> Unit = {},
    onResumeLastPlayed: (SurahAudioItem) -> Unit,
    onTogglePlayPause: () -> Unit,
    onReciterClick: (Reciter) -> Unit,
    onQuickPlayReciter: (Reciter) -> Unit,
    onSurahClick: (Surah) -> Unit,
    onNavigateToReciters: () -> Unit,
    onNavigateToSurahs: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenStorageDialog: () -> Unit,
    onPlayRandomRecitation: () -> Unit,
    onPlayDailySuggestion: () -> Unit,
    onNavigateToReader: (Int) -> Unit = {},
    currentLanguage: Language = Language.ARABIC,
    onOpenLanguageModal: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onRateApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient Subtle Islamic Geometric Pattern Background
        IslamicGeometricBackground(
            modifier = Modifier.matchParentSize(),
            patternColor = IslamicTheme.colors.ornamentColor.copy(alpha = if (IslamicTheme.isDark) 0.032f else 0.045f),
            spacing = 46.dp
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header (القرآن الكريم + أيقونة السماعة + أزرار البحث والإعدادات واللغة)
            item {
                HomeHeaderSection(
                    currentLanguage = currentLanguage,
                    onLanguageClick = onOpenLanguageModal,
                    onSearchClick = onNavigateToSearch,
                    onSettingsClick = onNavigateToSettings
                )
            }

            // 2. Continue Listening (أكمل الاستماع - إذا وُجد آخر مقطع مشغل)
            if (lastPlayedItem != null) {
                item {
                    ContinueListeningSection(
                        lastPlayedItem = lastPlayedItem,
                        lastPlayedPositionMs = lastPlayedPositionMs,
                        playerState = playerState,
                        onResume = { onResumeLastPlayed(lastPlayedItem) },
                        onTogglePlayPause = onTogglePlayPause
                    )
                }
            }

            // 4. Quick Actions (الوصول السريع + البطاقات الذهبية الست وبنر المصحف)
            item {
                QuickActionsSection(
                    onRecitersClick = onNavigateToReciters,
                    onSurahsClick = onNavigateToSurahs,
                    onFavoritesClick = onNavigateToFavorites,
                    onDownloadsClick = onOpenStorageDialog,
                    onRandomClick = onPlayRandomRecitation,
                    onDailyClick = onPlayDailySuggestion,
                    onReaderClick = { onNavigateToReader(1) }
                )
            }

            // 5. Quranic Quote Card (ألا بذكر الله تطمئن القلوب)
            item {
                QuranicReminderBanner(
                    onDailyClick = onPlayDailySuggestion
                )
            }

            item {
                IslamicOrnamentalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Gold400,
                    alpha = 0.35f
                )
            }

            // 6. Popular Reciters (استمع الآن - كبار القراء)
            item {
                PopularRecitersSection(
                    reciters = reciters.take(8),
                    playerState = playerState,
                    onReciterClick = onReciterClick,
                    onQuickPlayReciter = onQuickPlayReciter,
                    onViewAll = onNavigateToReciters
                )
            }

            item {
                IslamicOrnamentalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Gold400,
                    alpha = 0.35f
                )
            }

            // 7. Quick Surahs Access (السور المباركة)
            item {
                QuickSurahsSection(
                    surahs = surahs.take(12),
                    playerState = playerState,
                    onSurahClick = onSurahClick,
                    onViewAll = onNavigateToSurahs
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                HomeShareAndRateCard(
                    onShareApp = onShareApp,
                    onRateApp = onRateApp
                )
            }
        }
    }
}

@Composable
fun HomeShareAndRateCard(
    onShareApp: () -> Unit,
    onRateApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, IslamicTheme.colors.cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IslamicTheme.colors.heroGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "انشر تؤجر • شارك تطبيق القرآن الكريم",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = IslamicTheme.colors.heroAccent,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "قال ﷺ: «مَن دلَّ على خيرٍ فله مثلُ أجرِ فاعله»",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = IslamicTheme.colors.onHero.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Share App Button
                    Surface(
                        onClick = onShareApp,
                        shape = RoundedCornerShape(12.dp),
                        color = if (IslamicTheme.isDark) Color(0xFF134533) else Color(0xFF093E2C),
                        border = BorderStroke(1.dp, IslamicTheme.colors.heroAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("home_share_app_btn")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = IslamicTheme.colors.heroAccent,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مشاركة التطبيق",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = IslamicTheme.colors.onHero
                            )
                        }
                    }

                    // Rate App Button
                    Surface(
                        onClick = onRateApp,
                        shape = RoundedCornerShape(12.dp),
                        color = if (IslamicTheme.isDark) Color(0xFF1D5A42) else Color(0xFF0F4E38),
                        border = BorderStroke(1.dp, IslamicTheme.colors.heroAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("home_rate_app_btn")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = IslamicTheme.colors.heroAccent,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تقييم التطبيق",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = IslamicTheme.colors.heroAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeaderSection(
    currentLanguage: Language = Language.ARABIC,
    onLanguageClick: () -> Unit = {},
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Enforce physical direction regardless of system/language direction:
    // Logo & App Name are ALWAYS on the RIGHT.
    // Settings, Search, and Language icons are ALWAYS on the LEFT.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IslamicTheme.colors.headerGradient)
                .border(
                    BorderStroke(width = 0.5.dp, color = IslamicTheme.colors.headerBorder)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Physical LEFT: Action Icons (Settings, Search, Language) pinned tightly with no gaps
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Settings button
                    Surface(
                        onClick = onSettingsClick,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = if (IslamicTheme.isDark) 0.08f else 0.15f),
                        border = BorderStroke(0.7.dp, IslamicTheme.colors.goldAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("home_header_settings_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = LocaleManager.t("nav_settings", "الإعدادات"),
                                tint = Color(0xFFF7E2B5),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Search button
                    Surface(
                        onClick = onSearchClick,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = if (IslamicTheme.isDark) 0.08f else 0.15f),
                        border = BorderStroke(0.7.dp, IslamicTheme.colors.goldAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("home_header_search_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = LocaleManager.t("search", "بحث"),
                                tint = Color(0xFFF7E2B5),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Language Selector
                    LanguageHeaderButton(
                        currentLanguage = currentLanguage,
                        onClick = onLanguageClick
                    )
                }

                // Physical RIGHT: Title & Subtitle + Luxurious Gold Headphone Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = LocaleManager.t("app_title", "القرآن الكريم"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFFF7E2B5)
                        )
                        Text(
                            text = LocaleManager.t("app_subtitle", "كتابك للهداية، صوتاً وتدبراً"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Circular Luxury Gold Headphone Logo
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFF7E2B5),
                                        Color(0xFFD4AF37),
                                        Color(0xFFA67C1E)
                                    )
                                )
                            )
                            .border(1.2.dp, Color(0xFFFFF4D4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "القرآن الكريم",
                            tint = Color(0xFF09291D),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueListeningSection(
    lastPlayedItem: SurahAudioItem,
    lastPlayedPositionMs: Long,
    playerState: PlayerState,
    onResume: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    val isCurrentItemPlaying = playerState.isPlaying && playerState.currentItem?.surahNumber == lastPlayedItem.surahNumber && playerState.currentItem?.reciterId == lastPlayedItem.reciterId
    val progress = if (playerState.durationMs > 0 && playerState.currentItem?.surahNumber == lastPlayedItem.surahNumber) {
        playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()
    } else 0.35f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("card_continue_listening"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, IslamicTheme.colors.cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IslamicTheme.colors.cardGradient)
                .clickable {
                    if (playerState.currentItem?.surahNumber == lastPlayedItem.surahNumber && playerState.currentItem?.reciterId == lastPlayedItem.reciterId) {
                        onTogglePlayPause()
                    } else {
                        onResume()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            IslamicCornerOrnaments(
                color = IslamicTheme.colors.goldAccent.copy(alpha = 0.25f),
                modifier = Modifier.matchParentSize()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right side in RTL: Compact Play button + Text Details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Gold400, Gold600)
                                )
                            )
                            .testTag("btn_resume_play_large"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentItemPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isCurrentItemPlaying) "إيقاف مؤقت" else "متابعة الاستماع",
                            tint = Color(0xFF1B1605),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "أكمل: سورة ${lastPlayedItem.surahName}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = if (IslamicTheme.isDark) Color(0xFFF7E2B5) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = IslamicTheme.colors.badgeContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${lastPlayedItem.surahNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = IslamicTheme.colors.badgeContent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "بصوت ${lastPlayedItem.reciterName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (IslamicTheme.isDark) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Left side in RTL: Mini progress or status badge
                Surface(
                    color = if (IslamicTheme.isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, IslamicTheme.colors.goldAccent.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = if (isCurrentItemPlaying) "يُتلى الآن ✦" else "متابعة ▶",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        color = IslamicTheme.colors.goldText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onRecitersClick: () -> Unit,
    onSurahsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onRandomClick: () -> Unit,
    onDailyClick: () -> Unit,
    onReaderClick: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Section Title: الوصول السريع (Right aligned in warm gold)
        Text(
            text = "الوصول السريع",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = IslamicTheme.colors.goldText,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Featured Luxury Islamic Banner: المصحف الشريف والتفسير
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onReaderClick() }
                .testTag("quick_action_reader_banner"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, IslamicTheme.colors.cardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IslamicTheme.colors.cardGradient)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                IslamicCornerOrnaments(
                    color = IslamicTheme.colors.goldAccent.copy(alpha = 0.25f),
                    modifier = Modifier.matchParentSize()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right in RTL: Illuminated Golden Book Icon + Texts
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFFF7E2B5),
                                            Color(0xFFC9A227)
                                        )
                                    )
                                )
                                .border(1.dp, Color(0xFFFFF3D0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF061E15),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "المصحف الشريف والتفسير",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (IslamicTheme.isDark) Color(0xFFF7E2B5) else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "قراءة الآيات مع التفسير الميسر ومواضع التلاوة",
                                fontSize = 11.5.sp,
                                color = if (IslamicTheme.isDark) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Left in RTL: "اقرأ الآن ✦" pill button with gold accent
                    Surface(
                        color = IslamicTheme.colors.badgeContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, IslamicTheme.colors.goldAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { onReaderClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اقرأ الآن ✦",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = IslamicTheme.colors.goldText
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6 Islamic Action Cards Grid (3 columns x 2 rows)
        // Row 1: القراء (Right) | السور (Middle) | المفضلة (Left)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IslamicQuickActionCard(
                title = "القراء",
                subtitle = "أشهر القراء",
                icon = Icons.Default.Person,
                onClick = onRecitersClick,
                modifier = Modifier.weight(1f)
            )

            IslamicQuickActionCard(
                title = "السور",
                subtitle = "114 سورة",
                icon = Icons.Default.MenuBook,
                onClick = onSurahsClick,
                modifier = Modifier.weight(1f)
            )

            IslamicQuickActionCard(
                title = "المفضلة",
                subtitle = "قائمتك المفضلة",
                icon = Icons.Default.Star,
                onClick = onFavoritesClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: التنزيلات (Right) | ورد اليوم (Middle) | عشوائي (Left)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IslamicQuickActionCard(
                title = "التنزيلات",
                subtitle = "استمع بدون نت",
                icon = Icons.Default.CloudDownload,
                onClick = onDownloadsClick,
                modifier = Modifier.weight(1f)
            )

            IslamicQuickActionCard(
                title = "ورد اليوم",
                subtitle = "حصنك اليومي",
                icon = Icons.Default.WbSunny,
                onClick = onDailyClick,
                modifier = Modifier.weight(1f)
            )

            IslamicQuickActionCard(
                title = "عشوائي",
                subtitle = "تسميع لآية/سورة",
                icon = Icons.Default.Shuffle,
                onClick = onRandomClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun IslamicQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(116.dp)
            .clickable { onClick() }
            .testTag("quick_action_${title}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, IslamicTheme.colors.cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IslamicTheme.colors.cardGradient)
                .padding(horizontal = 6.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            IslamicCornerOrnaments(
                color = IslamicTheme.colors.ornamentColor.copy(alpha = 0.18f),
                modifier = Modifier.matchParentSize()
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (IslamicTheme.isDark) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF134533),
                                        Color(0xFF0B2B1E)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        IslamicLightPrimaryContainer,
                                        Color(0xFFE2F3EB)
                                    )
                                )
                            }
                        )
                        .border(1.dp, IslamicTheme.colors.goldAccent.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (IslamicTheme.isDark) Gold400 else IslamicLightPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = if (IslamicTheme.isDark) Color(0xFFF7E2B5) else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = if (IslamicTheme.isDark) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun QuranicReminderBanner(
    onDailyClick: () -> Unit
) {
    Card(
        modifier = modifierOrEmpty()
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, IslamicTheme.colors.cardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IslamicTheme.colors.cardGradient)
                .clickable { onDailyClick() }
                .padding(14.dp)
        ) {
            IslamicCornerOrnaments(
                color = IslamicTheme.colors.goldAccent.copy(alpha = 0.25f),
                modifier = Modifier.matchParentSize()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = IslamicTheme.colors.goldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = if (IslamicTheme.isDark) Color(0xFFF7E2B5) else IslamicTheme.colors.goldText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = IslamicTheme.colors.goldAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun modifierOrEmpty(): Modifier = Modifier

@Composable
fun PopularRecitersSection(
    reciters: List<Reciter>,
    playerState: PlayerState,
    onReciterClick: (Reciter) -> Unit,
    onQuickPlayReciter: (Reciter) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "استمع الآن (كبار القراء)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = IslamicTheme.colors.goldText
            )

            Text(
                text = "عرض الكل ←",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = IslamicTheme.colors.goldText,
                modifier = Modifier
                    .clickable { onViewAll() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reciters, key = { "pop_${it.id}" }) { reciter ->
                val isPlaying = playerState.currentItem?.reciterId == reciter.id
                ReciterCompactCard(
                    reciter = reciter,
                    isPlaying = isPlaying,
                    onClick = { onReciterClick(reciter) },
                    onPlay = { onQuickPlayReciter(reciter) }
                )
            }
        }
    }
}

@Composable
fun ReciterCompactCard(
    reciter: Reciter,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(145.dp)
            .clickable { onClick() }
            .testTag("popular_reciter_${reciter.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) IslamicTheme.colors.activeContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isPlaying) IslamicTheme.colors.goldAccent else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReciterAvatar(
                reciter = reciter,
                isPlaying = isPlaying,
                size = 58.dp,
                borderWidth = 2.dp,
                elevation = 3.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reciter.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = reciter.riwayah,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (IslamicTheme.isDark) Color(0xFF124734) else IslamicLightPrimaryContainer,
                    contentColor = if (IslamicTheme.isDark) Gold400 else IslamicLightPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "استماع", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun QuickSurahsSection(
    surahs: List<Surah>,
    playerState: PlayerState,
    onSurahClick: (Surah) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "السور المباركة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = IslamicTheme.colors.goldText
            )

            Text(
                text = "كل السور (114) ←",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = IslamicTheme.colors.goldText,
                modifier = Modifier
                    .clickable { onViewAll() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(surahs, key = { "home_surah_${it.number}" }) { surah ->
                val isPlaying = playerState.currentItem?.surahNumber == surah.number
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .clickable { onSurahClick(surah) }
                        .testTag("home_surah_card_${surah.number}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) IslamicTheme.colors.activeContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isPlaying) IslamicTheme.colors.goldAccent else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = if (isPlaying) IslamicTheme.colors.selectedBadgeContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isPlaying) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "جاري التشغيل",
                                        tint = IslamicTheme.colors.selectedBadgeContent,
                                        modifier = Modifier
                                            .size(11.dp)
                                            .padding(end = 2.dp)
                                    )
                                }
                                Text(
                                    text = "${surah.number}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = if (isPlaying) IslamicTheme.colors.selectedBadgeContent else IslamicTheme.colors.goldText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "سورة ${surah.name}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isPlaying) (if (IslamicTheme.isDark) IslamicTheme.colors.goldAccent else IslamicLightPrimary) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${surah.ayahs} آية • ${surah.type}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
