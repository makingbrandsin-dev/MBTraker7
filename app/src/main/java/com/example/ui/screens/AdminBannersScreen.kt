package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.presentation.components.banner.AppOfferBanner
import com.example.presentation.components.banner.OfferBannerCard
import com.example.presentation.components.banner.OfferBannerSlider
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Admin Panel View for managing the Firebase Firestore 'Banners' collection
 * and Firebase Storage assets for the App Home Screen Pager.
 *
 * Capabilities:
 * 1. Interface with Firestore 'Banners' collection in real-time
 * 2. Upload banner graphics and media assets to Firebase Storage
 * 3. Manage Display Order (Move Up, Move Down, reorder positions)
 * 4. Toggle Visibility (active/paused on home screen Pager)
 * 5. Interactive live Home Screen Pager preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBannersScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allBanners by viewModel.banners.collectAsState()
    val activeBanners by viewModel.activeBanners.collectAsState()
    val isFirestoreConnected by viewModel.isFirestoreBannersConnected.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Active, 2: Paused
    var searchQuery by remember { mutableStateOf("") }
    var showAddBannerDialog by remember { mutableStateOf(false) }
    var bannerToEdit by remember { mutableStateOf<AppOfferBanner?>(null) }
    var bannerToDelete by remember { mutableStateOf<AppOfferBanner?>(null) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }

    // Filter banners based on active tab & search query
    val filteredBanners = remember(allBanners, selectedTab, searchQuery) {
        val tabFiltered = when (selectedTab) {
            1 -> allBanners.filter { it.isActive }
            2 -> allBanners.filter { !it.isActive }
            else -> allBanners
        }
        if (searchQuery.isBlank()) {
            tabFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            tabFiltered.filter {
                it.headline.lowercase().contains(q) ||
                        (it.subtext?.lowercase()?.contains(q) == true) ||
                        (it.badge?.lowercase()?.contains(q) == true) ||
                        it.routeAction.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Home Screen Banners",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isFirestoreConnected) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isFirestoreConnected) StatusGreen else StatusOrange)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isFirestoreConnected) "Firestore Live" else "Cached Mode",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFirestoreConnected) Color(0xFF166534) else Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Firestore 'Banners' Collection & Cloud Storage",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_banners_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetConfirmationDialog = true },
                        modifier = Modifier.testTag("reset_banners_button")
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Reset Defaults",
                            tint = Color(0xFF64748B)
                        )
                    }
                    IconButton(
                        onClick = { showAddBannerDialog = true },
                        modifier = Modifier.testTag("add_banner_appbar_action")
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Add Banner",
                            tint = BrandBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddBannerDialog = true },
                containerColor = BrandBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                text = { Text("Upload Banner", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("upload_banner_fab")
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. Live Home Screen Pager Preview Section
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ViewCarousel,
                                            contentDescription = null,
                                            tint = BrandBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Live Home Screen Carousel Preview",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Shows active banners rendered in HorizontalPager order",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BrandBlue.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${activeBanners.size} Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (activeBanners.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF8FAFC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "No banners currently visible on Home Screen",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        "Toggle on the switch for any banner below to display it in Pager",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        } else {
                            // Renders live OfferBannerSlider with auto-scroll and indicator dots
                            OfferBannerSlider(
                                banners = activeBanners,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 2. Metrics & Status Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Banners",
                        value = "${allBanners.size}",
                        icon = Icons.Default.Collections,
                        color = BrandBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Visible in Pager",
                        value = "${activeBanners.size}",
                        icon = Icons.Default.Visibility,
                        color = StatusGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Hidden / Paused",
                        value = "${allBanners.size - activeBanners.size}",
                        icon = Icons.Default.VisibilityOff,
                        color = StatusOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Search & Filter Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_banner_search"),
                        placeholder = { Text("Search headline, badge, or action target...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    // Tab selector
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text("All (${allBanners.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Text("Active (${activeBanners.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Text("Paused (${allBanners.size - activeBanners.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Instructions & Order Hint
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANAGE DISPLAY ORDER & VISIBILITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Use ▲ ▼ to change order",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 5. Banner Items List
            if (filteredBanners.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FilterListOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "No Banners Match Criteria",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Try adjusting your filters or upload a new banner to Firestore.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddBannerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload New Banner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(filteredBanners, key = { _, banner -> banner.id }) { index, banner ->
                    BannerManagementItemCard(
                        banner = banner,
                        positionIndex = index,
                        totalCount = filteredBanners.size,
                        canMoveUp = index > 0,
                        canMoveDown = index < filteredBanners.size - 1,
                        onMoveUp = {
                            viewModel.moveBannerUp(banner.id)
                            Toast.makeText(context, "Moved '${banner.headline.take(20)}...' Up in Firestore order", Toast.LENGTH_SHORT).show()
                        },
                        onMoveDown = {
                            viewModel.moveBannerDown(banner.id)
                            Toast.makeText(context, "Moved '${banner.headline.take(20)}...' Down in Firestore order", Toast.LENGTH_SHORT).show()
                        },
                        onToggleVisibility = { isChecked ->
                            viewModel.toggleBannerStatus(banner.id, isChecked)
                            val msg = if (isChecked) "Banner activated in Home Screen Pager!" else "Banner hidden from Home Screen Pager"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onEdit = {
                            bannerToEdit = banner
                        },
                        onDelete = {
                            bannerToDelete = banner
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Dialog: Add / Upload New Banner
    if (showAddBannerDialog) {
        AddOrEditBannerDialog(
            existingBanner = null,
            initialOrder = allBanners.size,
            onDismiss = { showAddBannerDialog = false },
            onSave = { headline, subtext, ctaText, badge, routeAction, imageUri, gradient, ctaColor, isActive, order ->
                viewModel.addBanner(
                    headline = headline,
                    subtext = subtext,
                    ctaText = ctaText,
                    badge = badge,
                    routeAction = routeAction,
                    imageUri = imageUri,
                    bgGradientColors = gradient,
                    ctaButtonColor = ctaColor,
                    isActive = isActive,
                    displayOrder = order
                )
                showAddBannerDialog = false
                Toast.makeText(context, "Banner saved to Firestore 'Banners' & synced to Home Screen!", Toast.LENGTH_SHORT).show()
            },
            onUploadImage = { uri, onProgress ->
                viewModel.uploadBannerImageToStorage(uri, onProgress)
            },
            onSaveImageLocally = { uri ->
                viewModel.saveBannerImage(uri)
            }
        )
    }

    // Dialog: Edit Existing Banner
    bannerToEdit?.let { banner ->
        AddOrEditBannerDialog(
            existingBanner = banner,
            initialOrder = banner.displayOrder,
            onDismiss = { bannerToEdit = null },
            onSave = { headline, subtext, ctaText, badge, routeAction, imageUri, gradient, ctaColor, isActive, order ->
                viewModel.editBanner(
                    bannerId = banner.id,
                    headline = headline,
                    subtext = subtext,
                    ctaText = ctaText,
                    badge = badge,
                    routeAction = routeAction,
                    imageUri = imageUri,
                    bgGradientColors = gradient,
                    ctaButtonColor = ctaColor,
                    isActive = isActive,
                    displayOrder = order
                )
                bannerToEdit = null
                Toast.makeText(context, "Banner updated in Firestore 'Banners'!", Toast.LENGTH_SHORT).show()
            },
            onUploadImage = { uri, onProgress ->
                viewModel.uploadBannerImageToStorage(uri, onProgress)
            },
            onSaveImageLocally = { uri ->
                viewModel.saveBannerImage(uri)
            }
        )
    }

    // Dialog: Delete Confirmation
    bannerToDelete?.let { banner ->
        AlertDialog(
            onDismissRequest = { bannerToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Banner?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Are you sure you want to permanently remove \"${banner.headline}\" from Firestore 'Banners' collection? It will be immediately removed from the Home Screen Pager.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBanner(banner.id)
                        bannerToDelete = null
                        Toast.makeText(context, "Banner deleted from Firestore 'Banners'", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bannerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Reset Defaults Confirmation
    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Preset Banners?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("This will restore the 4 default Making Brands promotional banners with normalized display orders (0, 1, 2, 3) and re-seed them to Firestore 'Banners'.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetBannersToDefaults()
                        showResetConfirmationDialog = false
                        Toast.makeText(context, "Banners reset to presets and synced with Firestore", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Reset Defaults", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Metric Card for top statistics row.
 */
@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }
    }
}

/**
 * Individual Banner Management Card with Move Up, Move Down, Visibility Switch,
 * Thumbnail, and Actions.
 */
@Composable
private fun BannerManagementItemCard(
    banner: AppOfferBanner,
    positionIndex: Int,
    totalCount: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasImage = !banner.imageUri.isNullOrBlank()
    val isFirebaseStorage = banner.imageUri?.contains("firebasestorage") == true ||
            banner.imageUri?.startsWith("gs://") == true ||
            banner.imageUri?.startsWith("https://") == true

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            if (banner.isActive) Color(0xFFE2E8F0) else Color(0xFFCBD5E1).copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (banner.isActive) 2.dp else 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("banner_item_${banner.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Position Chip, Reorder Arrows, Visibility Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Order indicator + Up/Down arrows
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (banner.isActive) BrandBlue else Color(0xFF64748B)
                    ) {
                        Text(
                            text = "#${banner.displayOrder + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Move Up Button
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("move_up_${banner.id}")
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Move Up in Order",
                            tint = if (canMoveUp) BrandBlue else Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Move Down Button
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("move_down_${banner.id}")
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Move Down in Order",
                            tint = if (canMoveDown) BrandBlue else Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Visibility Toggle Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (banner.isActive) "Visible" else "Hidden",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (banner.isActive) StatusGreen else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = banner.isActive,
                        onCheckedChange = onToggleVisibility,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StatusGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("visibility_switch_${banner.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Thumbnail Preview + Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Banner Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 65.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(banner.bgGradientColors)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasImage) {
                        AsyncImage(
                            model = banner.imageUri,
                            contentDescription = banner.headline,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = banner.ctaButtonColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (banner.badge != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = banner.ctaButtonColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = banner.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = banner.ctaButtonColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Storage location badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isFirebaseStorage) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    if (isFirebaseStorage) Icons.Default.CloudQueue else Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = if (isFirebaseStorage) BrandBlue else Color(0xFF64748B),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isFirebaseStorage) "Storage" else "Preset",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFirebaseStorage) BrandBlue else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = banner.headline,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!banner.subtext.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = banner.subtext,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Route tag + CTA text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Target: ${banner.routeAction.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "CTA: ${banner.ctaText}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = banner.ctaButtonColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Actions: Edit & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("edit_banner_${banner.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Details", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = StatusRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("delete_banner_${banner.id}")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Dialog for creating or modifying a Banner with Firebase Storage upload support,
 * display order selector, and live preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditBannerDialog(
    existingBanner: AppOfferBanner?,
    initialOrder: Int,
    onDismiss: () -> Unit,
    onSave: (
        headline: String,
        subtext: String?,
        ctaText: String,
        badge: String?,
        routeAction: String,
        imageUri: String?,
        bgGradient: List<Color>,
        ctaColor: Color,
        isActive: Boolean,
        displayOrder: Int
    ) -> Unit,
    onUploadImage: suspend (Uri, ((Float) -> Unit)?) -> Result<String>,
    onSaveImageLocally: (Uri) -> String?
) {
    val coroutineScope = rememberCoroutineScope()

    var headline by remember { mutableStateOf(existingBanner?.headline ?: "") }
    var subtext by remember { mutableStateOf(existingBanner?.subtext ?: "") }
    var ctaText by remember { mutableStateOf(existingBanner?.ctaText ?: "Explore Now") }
    var badge by remember { mutableStateOf(existingBanner?.badge ?: "SPECIAL PROMO") }
    var selectedRoute by remember { mutableStateOf(existingBanner?.routeAction ?: "leads") }
    var isBannerActive by remember { mutableStateOf(existingBanner?.isActive ?: true) }
    var displayOrder by remember { mutableIntStateOf(existingBanner?.displayOrder ?: initialOrder) }

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(existingBanner?.imageUri?.let { Uri.parse(it) })
    }
    var uploadedCloudUrl by remember { mutableStateOf<String?>(existingBanner?.imageUri) }
    var isUploadingToStorage by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    // Color palettes
    val palettes = listOf(
        Triple("Teal & Emerald", listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFFBBF7D0)), Color(0xFF0D9488)),
        Triple("Royal Blue", listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFBFDBFE)), Color(0xFF2563EB)),
        Triple("Sunset Orange", listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFED7AA)), Color(0xFFEA580C)),
        Triple("Royal Purple", listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFFE9D5FF)), Color(0xFF7C3AED)),
        Triple("Rose Pink", listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFFECDD3)), Color(0xFFE11D48)),
        Triple("Dark Charcoal", listOf(Color(0xFF1E293B), Color(0xFF0F172A)), Color(0xFF38BDF8))
    )
    var selectedPaletteIndex by remember { mutableIntStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isUploadingToStorage = true
            uploadProgress = 0.05f

            coroutineScope.launch {
                val result = onUploadImage(uri) { progress ->
                    uploadProgress = progress
                }
                isUploadingToStorage = false
                result.onSuccess { cloudUrl ->
                    uploadedCloudUrl = cloudUrl
                }.onFailure {
                    // Local fallback
                    uploadedCloudUrl = onSaveImageLocally(uri) ?: uri.toString()
                }
            }
        }
    }

    val availableRoutes = listOf(
        "leads" to "CRM Leads Pipeline",
        "tasks" to "My Tasks & Assignments",
        "milo_ai" to "Milo AI Copilot",
        "invoices" to "Quotations & Invoices",
        "attendance" to "Punch-In Attendance",
        "chat" to "Team Chat & Channels",
        "projects" to "Active Projects",
        "vault" to "Enterprise Secure Vault"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (existingBanner == null) Icons.Default.CloudUpload else Icons.Default.Edit,
                    contentDescription = null,
                    tint = BrandBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingBanner == null) "Upload & Add Home Banner" else "Edit Banner Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Image Upload to Firebase Storage
                item {
                    Text(
                        text = "1. Banner Graphic / Image",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isUploadingToStorage) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { uploadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = BrandBlue
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Uploading to Firebase Storage: ${(uploadProgress * 100).toInt()}%...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue
                                    )
                                }
                            } else if (uploadedCloudUrl != null || selectedImageUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = uploadedCloudUrl ?: selectedImageUri,
                                        contentDescription = "Selected Banner Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFECFDF5)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = StatusGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Firebase Storage: Ready",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uploadedCloudUrl != null) "Replace Image in Storage" else "Select Image from Gallery",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Section 2: Banner Details
                item {
                    Text(
                        text = "2. Headline & Copy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text("Headline *") },
                        placeholder = { Text("e.g. Instant Lead Sync & Auto Settlement") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = subtext,
                        onValueChange = { subtext = it },
                        label = { Text("Subtitle / Description") },
                        placeholder = { Text("e.g. Distributed in 15 seconds to active staff") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ctaText,
                            onValueChange = { ctaText = it },
                            label = { Text("CTA Button Text") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = badge,
                            onValueChange = { badge = it },
                            label = { Text("Badge Label") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Section 3: Navigation Target
                item {
                    Text(
                        text = "3. Tap Action Target",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = availableRoutes.firstOrNull { it.first == selectedRoute }?.second ?: selectedRoute,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableRoutes.forEach { (routeKey, routeLabel) ->
                                DropdownMenuItem(
                                    text = { Text(routeLabel, fontSize = 13.sp) },
                                    onClick = {
                                        selectedRoute = routeKey
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 4: Display Order & Visibility Controls
                item {
                    Text(
                        text = "4. Display Order & Home Visibility",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Order selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Position in Pager",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Slide index #${displayOrder + 1}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (displayOrder > 0) displayOrder-- },
                                        enabled = displayOrder > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease Position")
                                    }
                                    Text(
                                        text = "#${displayOrder + 1}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { displayOrder++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase Position")
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                            // Visibility switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Visible on Home Screen",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        if (isBannerActive) "Active in Pager Carousel" else "Hidden / Paused",
                                        fontSize = 11.sp,
                                        color = if (isBannerActive) StatusGreen else StatusOrange
                                    )
                                }
                                Switch(
                                    checked = isBannerActive,
                                    onCheckedChange = { isBannerActive = it }
                                )
                            }
                        }
                    }
                }

                // Section 5: Live Card Preview
                item {
                    Text(
                        text = "5. Live Card Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val selectedPalette = palettes[selectedPaletteIndex]
                    val previewBanner = AppOfferBanner(
                        id = existingBanner?.id ?: "preview",
                        headline = headline.ifBlank { "Instant Lead Settlement & Auto Sync" },
                        subtext = subtext.ifBlank { "Get real-time distributed leads in 15 seconds." },
                        ctaText = ctaText.ifBlank { "Explore Now" },
                        disclaimer = "*SLA guarantee active for all registered employees",
                        badge = badge.ifBlank { "SPECIAL OFFER" },
                        bgGradientColors = selectedPalette.second,
                        ctaButtonColor = selectedPalette.third,
                        routeAction = selectedRoute,
                        imageUri = uploadedCloudUrl ?: selectedImageUri?.toString(),
                        isActive = isBannerActive,
                        displayOrder = displayOrder
                    )

                    OfferBannerCard(
                        banner = previewBanner,
                        onClick = {}
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (headline.isBlank()) return@Button
                    val selectedPalette = palettes[selectedPaletteIndex]
                    onSave(
                        headline.trim(),
                        subtext.trim().takeIf { it.isNotBlank() },
                        ctaText.trim().ifBlank { "Explore Now" },
                        badge.trim().takeIf { it.isNotBlank() },
                        selectedRoute,
                        uploadedCloudUrl ?: selectedImageUri?.toString(),
                        selectedPalette.second,
                        selectedPalette.third,
                        isBannerActive,
                        displayOrder
                    )
                },
                enabled = headline.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                modifier = Modifier.testTag("submit_banner_button")
            ) {
                Icon(
                    if (existingBanner == null) Icons.Default.CloudUpload else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (existingBanner == null) "Save to Firestore 'Banners'" else "Update in Firestore",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
