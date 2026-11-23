package com.example.ui.student

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.AppRole
import com.example.data.DeliveryMode
import com.example.data.HostelCatalog
import com.example.data.Order
import com.example.data.OrderMode
import com.example.data.OrderStatus
import com.example.data.QuickItem
import com.example.data.Shop
import com.example.data.StudentProfile
import com.example.ui.components.PrinceMartFooter
import com.example.ui.components.VoiceNotePlayerPill
import com.example.ui.components.dialPhoneNumber
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.viewmodel.PrinceMartUiState
import com.example.viewmodel.StreamState
import com.example.viewmodel.ToastTone

@Composable
fun StudentScreen(
    uiState: PrinceMartUiState,
    studentProfileState: StreamState<StudentProfile?>,
    shopsState: StreamState<List<Shop>>,
    ordersState: StreamState<List<Order>>,
    onSetDeliveryMode: (DeliveryMode) -> Unit,
    onOpenProfileModal: () -> Unit,
    onUpdateComposerNote: (String) -> Unit,
    onSelectCommonTarget: () -> Unit,
    onSelectSpecificShop: (Shop) -> Unit,
    onToggleVoiceRecording: () -> Unit,
    onClearVoiceNote: () -> Unit,
    onTogglePlayAudio: (String, String) -> Unit,
    onSubmitVoiceTextOrder: () -> Unit,
    onCancelPendingOrder: (String) -> Unit,
    onUpdateSearch: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onIncrementCart: (String) -> Unit,
    onDecrementCart: (String) -> Unit,
    onOpenCheckout: () -> Unit,
    onSwitchToShopkeeper: () -> Unit,
    onShowToast: (String, ToastTone) -> Unit
) {
    val context = LocalContext.current
    val studentProfile = (studentProfileState as? StreamState.Success)?.data
    val shops = (shopsState as? StreamState.Success)?.data.orEmpty()
    val orders = (ordersState as? StreamState.Success)?.data.orEmpty()
    val activeOrders = remember(orders) {
        orders.filter {
            val st = it.parsedStatus()
            st != OrderStatus.CANCELLED && st != OrderStatus.DECLINED
        }.take(5)
    }

    val filteredCatalog = remember(uiState.searchQuery, uiState.selectedCategory) {
        val q = uiState.searchQuery.trim().lowercase()
        HostelCatalog.items.filter { item ->
            val catMatch = uiState.selectedCategory == "सभी" || item.category == uiState.selectedCategory
            val queryMatch = q.isEmpty() ||
                item.nameHi.lowercase().contains(q) ||
                item.searchKeywords.lowercase().contains(q) ||
                item.category.lowercase().contains(q)
            catMatch && queryMatch
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleVoiceRecording()
        } else {
            onShowToast("वॉइस नोट के लिए माइक की अनुमति ज़रूरी है", ToastTone.ERROR)
        }
    }

    val handleRecordClick: () -> Unit = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            onToggleVoiceRecording()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = if (uiState.totalCartItems > 0) 112.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Banner + Student Profile Pill + Delivery Mode Toggle
            item(key = "hero_and_delivery") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroBannerWithProfile(
                        profile = studentProfile,
                        openShopCount = shops.count { it.shopOpen },
                        onOpenProfileModal = onOpenProfileModal
                    )
                    DeliveryModeToggleBar(
                        selected = uiState.deliveryMode,
                        onSelect = onSetDeliveryMode
                    )
                }
            }

            // 2. Live Order Tracking Banner (when student has placed orders)
            if (activeOrders.isNotEmpty()) {
                item(key = "live_tracking_header") {
                    Text(
                        text = "📍 लाइव ऑर्डर ट्रैकिंग",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(items = activeOrders, key = { "track_${it.id}" }) { order ->
                    LiveOrderTrackingCard(
                        order = order,
                        playingAudioKey = uiState.playingAudioKey,
                        onCallShop = { dialPhoneNumber(context, order.acceptedShopMobile) },
                        onPlayVoice = { onTogglePlayAudio("order_${order.id}", order.voiceNoteBase64) },
                        onCancelOrder = { onCancelPendingOrder(order.id) }
                    )
                }
            }

            // 3. Voice Note + Text Ordering Composer (Common Broadcast OR Specific Shop)
            item(key = "voice_text_composer") {
                VoiceAndTextOrderCard(
                    uiState = uiState,
                    shops = shops,
                    onUpdateNote = onUpdateComposerNote,
                    onSelectCommon = onSelectCommonTarget,
                    onSelectShop = onSelectSpecificShop,
                    onRecordClick = handleRecordClick,
                    onPlayPreview = {
                        val voice = uiState.recordedVoiceNote
                        if (voice != null) {
                            onTogglePlayAudio("composer_preview", voice.base64)
                        }
                    },
                    onClearVoice = onClearVoiceNote,
                    onSubmitOrder = onSubmitVoiceTextOrder
                )
            }

            // 4. Nearby Shops Cards (OLX-style Call / Inquiry + Direct Order Select)
            item(key = "nearby_shops_section") {
                NearbyShopsSection(
                    shopsState = shopsState,
                    selectedShopId = if (uiState.composerOrderMode == OrderMode.DIRECT) uiState.composerTargetShopId else "",
                    onCallShop = { shop -> dialPhoneNumber(context, shop.mobile) },
                    onSelectShopForOrder = onSelectSpecificShop,
                    onSwitchToShopkeeper = onSwitchToShopkeeper
                )
            }

            // 5. Blinkit-Style Quick Grid Header + Search + Categories
            item(key = "quick_grid_controls") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ तुरंत मँगवाएँ (Quick Grid)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredCatalog.size} आइटम",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onUpdateSearch,
                        placeholder = { Text("मैगी, चाय, चिप्स, पेन, दवाई खोजें...") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = EmeraldPrimary)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onUpdateSearch("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "खोज साफ करें")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalog_search_input")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(HostelCatalog.categories) { category ->
                            val selected = uiState.selectedCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCategory(category) },
                                label = {
                                    Text(
                                        text = category,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 6. Responsive 2-column (or 3-column on tablet) Quick Grid rows
            item(key = "quick_grid_items") {
                if (filteredCatalog.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "कोई आइटम नहीं मिला — ऊपर लिखकर या वॉइस नोट से मँगवाएँ!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val columns = if (maxWidth > 600.dp) 3 else 2
                        val rows = filteredCatalog.chunked(columns)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        QuickCatalogItemCard(
                                            item = item,
                                            quantity = uiState.cartQuantities[item.id] ?: 0,
                                            onIncrement = { onIncrementCart(item.id) },
                                            onDecrement = { onDecrementCart(item.id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Footer: Developed by Prince
            item(key = "student_footer") {
                PrinceMartFooter(
                    modifier = if (uiState.totalCartItems == 0) Modifier.navigationBarsPadding() else Modifier
                )
            }
        }

        // Sticky Bottom Cart Bar
        AnimatedVisibility(
            visible = uiState.totalCartItems > 0,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                onClick = onOpenCheckout,
                shape = RoundedCornerShape(18.dp),
                color = EmeraldPrimary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag("open_checkout_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${uiState.totalCartItems} आइटम चुने गए",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "₹${uiState.totalCartAmount}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberAccent)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "चेकआउट करें",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberOnContainer,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = AmberOnContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBannerWithProfile(
    profile: StudentProfile?,
    openShopCount: Int,
    onOpenProfileModal: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hostel_banner),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.68f),
                                    Color.Black.copy(alpha = 0.18f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        color = AmberAccent,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (openShopCount > 0) "🟢 $openShopCount दुकानें अभी खुली हैं" else "⚡ हाइपरलोकल हॉस्टल सर्विस",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberOnContainer,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "बगल की दुकान से मिनटों में!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "बोलकर, लिखकर या Quick Grid से ऑर्डर करें",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Student Profile Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProfileModal)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("student_profile_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldLightContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        if (profile != null && profile.hasCompleteDetails()) {
                            Text(
                                text = "${profile.name} • ${profile.hostelName}, रूम ${profile.roomNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "📱 ${profile.mobile}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "स्टूडेंट प्रोफ़ाइल सेट करें",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "नाम, मोबाइल, हॉस्टल और रूम नंबर डालें",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldLightContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "प्रोफ़ाइल बदलें",
                            tint = EmeraldDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (profile?.hasCompleteDetails() == true) "बदलें" else "सेट करें",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryModeToggleBar(
    selected: DeliveryMode,
    onSelect: (DeliveryMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DeliveryMode.entries.forEach { mode ->
            val isSelected = selected == mode
            val tag = if (mode == DeliveryMode.HOSTEL_GATE) "toggle_hostel_gate" else "toggle_shop_pickup"
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .testTag(tag)
            ) {
                Text(
                    text = if (mode == DeliveryMode.HOSTEL_GATE) "🛵 ${mode.labelHi}" else "🏪 ${mode.labelHi}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveOrderTrackingCard(
    order: Order,
    playingAudioKey: String?,
    onCallShop: () -> Unit,
    onPlayVoice: () -> Unit,
    onCancelOrder: () -> Unit
) {
    val status = order.parsedStatus()
    val steps = listOf(
        OrderStatus.PENDING to "पेंडिंग",
        OrderStatus.ACCEPTED to "स्वीकार",
        OrderStatus.PACKING to "पैकिंग",
        OrderStatus.AT_GATE to "गेट पर",
        OrderStatus.DELIVERED to "डिलीवर"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = if (status == OrderStatus.AT_GATE) AmberAccent else EmeraldPrimary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("live_tracking_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (status == OrderStatus.PENDING) AmberContainer else EmeraldLightContainer
                ) {
                    Text(
                        text = when (status) {
                            OrderStatus.PENDING -> "⏳ दुकानदार का इंतज़ार..."
                            OrderStatus.ACCEPTED -> "✓ ऑर्डर स्वीकार हो गया"
                            OrderStatus.PACKING -> "📦 सामान पैक हो रहा है"
                            OrderStatus.AT_GATE -> "📍 हॉस्टल गेट पर पहुँच गया!"
                            OrderStatus.DELIVERED -> "✅ डिलीवर हो गया"
                            else -> status.labelHi
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (status == OrderStatus.PENDING) AmberOnContainer else EmeraldDark,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // 4-Digit Pickup PIN Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberAccent
                ) {
                    Text(
                        text = "PIN: ${order.pickupPin}",
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberOnContainer,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5-Step Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { idx, (stepStatus, shortLabel) ->
                    val reached = status.stepIndex >= stepStatus.stepIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (reached) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (reached) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reached) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order items & custom text summary
            if (order.itemsSummary.isNotEmpty()) {
                Text(
                    text = order.itemsSummary.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            if (order.customNote.isNotBlank()) {
                Text(
                    text = "📝 ${order.customNote}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (order.hasVoiceNote()) {
                Spacer(modifier = Modifier.height(8.dp))
                VoiceNotePlayerPill(
                    durationSec = order.voiceNoteDurationSec.toInt(),
                    playing = playingAudioKey == "order_${order.id}",
                    onClick = onPlayVoice
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Accepted shopkeeper info + OLX-style Call button OR pending status
            if (order.acceptedShopName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldLightContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🏪 ${order.acceptedShopName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📱 ${order.acceptedShopMobile} • ${order.acceptedShopDistance}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onCallShop,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("call_accepted_shop_${order.id}")
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "दुकानदार को कॉल करें",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (order.parsedOrderMode() == OrderMode.COMMON) {
                            "📢 सभी खुली दुकानों को भेजा गया (Common)"
                        } else {
                            "🎯 ${order.targetShopName} को भेजा गया"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (status == OrderStatus.PENDING) {
                        TextButton(onClick = onCancelOrder) {
                            Text("रद्द करें", color = StatusDanger, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceAndTextOrderCard(
    uiState: PrinceMartUiState,
    shops: List<Shop>,
    onUpdateNote: (String) -> Unit,
    onSelectCommon: () -> Unit,
    onSelectShop: (Shop) -> Unit,
    onRecordClick: () -> Unit,
    onPlayPreview: () -> Unit,
    onClearVoice: () -> Unit,
    onSubmitOrder: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🎤 बोलकर या लिखकर ऑर्डर करें",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "सीधे किसी एक दुकान या सभी खुली दुकानों को एक साथ भेजें",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Target selector: Common Broadcast OR Specific Shop
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "common_chip") {
                    val isCommon = uiState.composerOrderMode == OrderMode.COMMON
                    FilterChip(
                        selected = isCommon,
                        onClick = onSelectCommon,
                        label = {
                            Text(
                                text = "📢 सभी खुली दुकानों को भेजें (Common)",
                                fontWeight = if (isCommon) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberAccent,
                            selectedLabelColor = AmberOnContainer
                        ),
                        modifier = Modifier.testTag("composer_common_target")
                    )
                }
                items(shops, key = { "target_${it.id}" }) { shop ->
                    val selected = uiState.composerOrderMode == OrderMode.DIRECT &&
                        uiState.composerTargetShopId == shop.id
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectShop(shop) },
                        label = {
                            Text(
                                text = "🏪 ${shop.shopName} ${if (shop.shopOpen) "• OPEN" else "• CLOSED"}",
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldLightContainer,
                            selectedLabelColor = EmeraldDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.composerNote,
                onValueChange = onUpdateNote,
                placeholder = { Text("सामान लिखें — जैसे: 2 मैगी, 1 दूध पैकेट, 1 काला पेन...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_text_order_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Voice Recorder Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRecordClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.recordingActive) StatusDanger else AmberAccent,
                        contentColor = if (uiState.recordingActive) Color.White else AmberOnContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("record_voice_note_button")
                ) {
                    Icon(
                        imageVector = if (uiState.recordingActive) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = stringResource(R.string.cd_record_voice),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.recordingActive) {
                            "🔴 रोकें (${uiState.recordingSeconds}s / 30s)"
                        } else if (uiState.recordedVoiceNote != null) {
                            "🎤 फिर से रिकॉर्ड करें"
                        } else {
                            "🎤 वॉइस नोट रिकॉर्ड करें"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.recordedVoiceNote != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VoiceNotePlayerPill(
                        durationSec = uiState.recordedVoiceNote.durationSec,
                        playing = uiState.playingAudioKey == "composer_preview",
                        onClick = onPlayPreview,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onClearVoice,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusDangerContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "वॉइस नोट हटाएँ",
                            tint = StatusDanger
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSubmitOrder,
                enabled = !uiState.submitting &&
                    !uiState.recordingActive &&
                    (uiState.composerNote.isNotBlank() || uiState.recordedVoiceNote != null),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("send_quick_voice_order_button")
            ) {
                if (uiState.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.composerOrderMode == OrderMode.COMMON) {
                            "सभी खुली दुकानों को ऑर्डर भेजें"
                        } else {
                            "चुनी गई दुकान को ऑर्डर भेजें"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyShopsSection(
    shopsState: StreamState<List<Shop>>,
    selectedShopId: String,
    onCallShop: (Shop) -> Unit,
    onSelectShopForOrder: (Shop) -> Unit,
    onSwitchToShopkeeper: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "🏪 पास की दुकानें (कॉल / इंक्वायरी)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        when (shopsState) {
            is StreamState.Loading -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("पास की दुकानें लोड हो रही हैं...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            is StreamState.Error -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StatusDangerContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = shopsState.message,
                        color = StatusDanger,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            is StreamState.Success -> {
                val shops = shopsState.data
                if (shops.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Storefront,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "अभी कोई दुकान रजिस्टर नहीं है",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "दुकानदार पैनल से बगल की दुकान जोड़ें — वह तुरंत यहाँ लाइव दिखेगी!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onSwitchToShopkeeper,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("🏪 दुकानदार पैनल खोलें", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(shops, key = { it.id }) { shop ->
                            NearbyShopCard(
                                shop = shop,
                                isSelected = shop.id == selectedShopId,
                                onCall = { onCallShop(shop) },
                                onSelectForOrder = { onSelectShopForOrder(shop) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyShopCard(
    shop: Shop,
    isSelected: Boolean,
    onCall: () -> Unit,
    onSelectForOrder: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(268.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (shop.shopOpen) EmeraldLightContainer else StatusDangerContainer
                ) {
                    Text(
                        text = if (shop.shopOpen) "● OPEN" else "● CLOSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (shop.shopOpen) EmeraldDark else StatusDanger,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "📍 ${shop.distance}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = shop.shopName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${shop.category} • 📱 ${shop.mobile}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCall,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("call_shop_${shop.id}")
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "कॉल / इंक्वायरी",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onSelectForOrder,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if (isSelected) "✓ चुना" else "चुनें",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCatalogItemCard(
    item: QuickItem,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier.border(
            width = if (quantity > 0) 1.5.dp else 1.dp,
            color = if (quantity > 0) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.nameHi,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 36.dp)
            )
            Text(
                text = item.unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${item.priceInr}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (quantity == 0) {
                    Surface(
                        onClick = onIncrement,
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldLightContainer,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .border(1.dp, EmeraldPrimary, RoundedCornerShape(10.dp))
                            .testTag("add_item_${item.id}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "+ ADD",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldDark,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldPrimary)
                            .heightIn(min = 40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(onClick = onDecrement)
                                .testTag("dec_item_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "कम करें",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(onClick = onIncrement)
                                .testTag("inc_item_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "बढ़ाएँ",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
