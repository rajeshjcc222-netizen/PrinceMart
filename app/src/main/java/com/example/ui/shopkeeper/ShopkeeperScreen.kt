package com.example.ui.shopkeeper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.HostelCatalog
import com.example.data.Order
import com.example.data.OrderMode
import com.example.data.OrderStatus
import com.example.data.Shop
import com.example.ui.components.PrinceMartFooter
import com.example.ui.components.VoiceNotePlayerPill
import com.example.ui.components.dialPhoneNumber
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusBlueContainer
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.viewmodel.PrinceMartUiState
import com.example.viewmodel.StreamState

@Composable
fun ShopkeeperScreen(
    uiState: PrinceMartUiState,
    myShopsState: StreamState<List<Shop>>,
    incomingOrdersState: StreamState<List<Order>>,
    acceptedOrdersState: StreamState<List<Order>>,
    onOpenRegisterShop: () -> Unit,
    onSelectOwnedShop: (String) -> Unit,
    onToggleShopOpen: (Shop) -> Unit,
    onAcceptOrder: (Order, Shop) -> Unit,
    onSkipOrder: (Order, Shop) -> Unit,
    onAdvanceOrderStatus: (Order, OrderStatus) -> Unit,
    onTogglePlayVoice: (String, String) -> Unit
) {
    val context = LocalContext.current
    val myShops = (myShopsState as? StreamState.Success)?.data.orEmpty()
    val activeShop = myShops.firstOrNull { it.id == uiState.selectedShopId } ?: myShops.firstOrNull()
    val incomingOrders = (incomingOrdersState as? StreamState.Success)?.data.orEmpty()
    val acceptedOrders = (acceptedOrdersState as? StreamState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Register / Manage Shop Card + Big 1-Tap OPEN/CLOSED Switch
        item(key = "shop_control_panel") {
            ShopControlHeaderCard(
                myShopsState = myShopsState,
                activeShop = activeShop,
                onSelectShop = onSelectOwnedShop,
                onOpenRegisterShop = onOpenRegisterShop,
                onToggleShopOpen = onToggleShopOpen
            )
        }

        // 2. Incoming Orders List
        item(key = "incoming_orders_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔔 नए ऑर्डर (Incoming)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (incomingOrders.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(50), color = AmberAccent) {
                        Text(
                            text = "${incomingOrders.size} पेंडिंग",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberOnContainer,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (activeShop == null) {
            item(key = "need_shop_notice") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AmberContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ऑर्डर स्वीकार करने के लिए पहले ऊपर '+ नई दुकान रजिस्टर करें' बटन से अपनी दुकान जोड़ें।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmberOnContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else if (incomingOrdersState is StreamState.Loading) {
            item(key = "incoming_loading") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                }
            }
        } else if (incomingOrders.isEmpty()) {
            item(key = "incoming_empty") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "अभी कोई नया पेंडिंग ऑर्डर नहीं है",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "जैसे ही कोई स्टूडेंट ऑर्डर भेजेगा, यहाँ लाइव अलर्ट और आवाज़ के साथ दिखेगा।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(incomingOrders, key = { "incoming_${it.id}" }) { order ->
                IncomingOrderCard(
                    order = order,
                    playingAudioKey = uiState.playingAudioKey,
                    submitting = uiState.submitting,
                    onPlayVoice = { onTogglePlayVoice("in_${order.id}", order.voiceNoteBase64) },
                    onCallStudent = { dialPhoneNumber(context, order.studentMobile) },
                    onAccept = { onAcceptOrder(order, activeShop) },
                    onSkip = { onSkipOrder(order, activeShop) }
                )
            }
        }

        // 3. Accepted Orders List
        item(key = "accepted_orders_header") {
            Text(
                text = "📦 स्वीकार किए गए ऑर्डर (${acceptedOrders.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (acceptedOrders.isEmpty()) {
            item(key = "accepted_empty") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "आपके द्वारा स्वीकार किए गए ऑर्डर यहाँ दिखेंगे — 1-टैप में पैकिंग से डिलीवरी तक अपडेट करें।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(acceptedOrders, key = { "accepted_${it.id}" }) { order ->
                AcceptedOrderCard(
                    order = order,
                    playingAudioKey = uiState.playingAudioKey,
                    onPlayVoice = { onTogglePlayVoice("acc_${order.id}", order.voiceNoteBase64) },
                    onCallStudent = { dialPhoneNumber(context, order.studentMobile) },
                    onAdvanceStatus = { next -> onAdvanceOrderStatus(order, next) }
                )
            }
        }

        // 4. Footer: Developed by Prince
        item(key = "shopkeeper_footer") {
            PrinceMartFooter(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun ShopControlHeaderCard(
    myShopsState: StreamState<List<Shop>>,
    activeShop: Shop?,
    onSelectShop: (String) -> Unit,
    onOpenRegisterShop: () -> Unit,
    onToggleShopOpen: (Shop) -> Unit
) {
    val myShops = (myShopsState as? StreamState.Success)?.data.orEmpty()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "मेरी दुकान",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onOpenRegisterShop,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = AmberOnContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("register_shop_button")
                ) {
                    Icon(Icons.Rounded.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ नई दुकान रजिस्टर करें",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (myShops.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(myShops, key = { "own_${it.id}" }) { shop ->
                        FilterChip(
                            selected = shop.id == activeShop?.id,
                            onClick = { onSelectShop(shop.id) },
                            label = { Text(shop.shopName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldLightContainer,
                                selectedLabelColor = EmeraldDark
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeShop == null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "अपनी दुकान 10 सेकंड में ऑनलाइन लाएँ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "दुकान रजिस्टर करते ही हॉस्टल के सभी स्टूडेंट को आपकी दुकान दिखने लगेगी और आप लाइव ऑर्डर ले पाएँगे।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Big 1-Tap OPEN / CLOSED Switch Card
                Surface(
                    onClick = { onToggleShopOpen(activeShop) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (activeShop.shopOpen) EmeraldLightContainer else StatusDangerContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = if (activeShop.shopOpen) EmeraldPrimary else StatusDanger,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("shop_open_switch")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeShop.shopName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (activeShop.shopOpen) EmeraldDark else StatusDanger
                            )
                            Text(
                                text = "${activeShop.category} • 📍 ${activeShop.distance} • 📱 ${activeShop.mobile}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (activeShop.shopOpen) {
                                    "🟢 दुकान खुली है (OPEN) — ऑर्डर आ रहे हैं"
                                } else {
                                    "🔴 दुकान बंद है (CLOSED) — खोलने के लिए टैप करें"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (activeShop.shopOpen) EmeraldDark else StatusDanger,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = activeShop.shopOpen,
                            onCheckedChange = { onToggleShopOpen(activeShop) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = StatusDanger
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingOrderCard(
    order: Order,
    playingAudioKey: String?,
    submitting: Boolean,
    onPlayVoice: () -> Unit,
    onCallStudent: () -> Unit,
    onAccept: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, AmberAccent, RoundedCornerShape(18.dp))
            .testTag("incoming_order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (order.parsedOrderMode() == OrderMode.COMMON) AmberContainer else StatusBlueContainer
                ) {
                    Text(
                        text = if (order.parsedOrderMode() == OrderMode.COMMON) {
                            "📢 Common Broadcast"
                        } else {
                            "🎯 Direct ऑर्डर"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (order.parsedOrderMode() == OrderMode.COMMON) AmberOnContainer else StatusBlue,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${order.parsedDeliveryMode().labelHi} • ${order.parsedPaymentMethod().labelHi}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Student details + OLX-style Call button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎓 ${order.studentName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🏠 ${order.hostelName}, रूम ${order.roomNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "📱 ${order.studentMobile}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = onCallStudent,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("call_student_${order.id}")
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "स्टूडेंट को कॉल करें",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items / Note / Voice Note
            if (order.itemsSummary.isNotEmpty()) {
                order.itemsSummary.forEach { line ->
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (order.totalAmount > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "अनुमानित बिल: ₹${order.totalAmount}",
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldDark,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (order.customNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 नोट: ${order.customNote}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (order.hasVoiceNote()) {
                Spacer(modifier = Modifier.height(8.dp))
                VoiceNotePlayerPill(
                    durationSec = order.voiceNoteDurationSec.toInt(),
                    playing = playingAudioKey == "in_${order.id}",
                    onClick = onPlayVoice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_incoming_voice_${order.id}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Big Accept & Skip Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAccept,
                    enabled = !submitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("accept_order_${order.id}")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "मेरे पास है (Accept)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                OutlinedButton(
                    onClick = onSkip,
                    enabled = !submitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(52.dp)
                        .testTag("skip_order_${order.id}")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "नहीं है (Skip)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AcceptedOrderCard(
    order: Order,
    playingAudioKey: String?,
    onPlayVoice: () -> Unit,
    onCallStudent: () -> Unit,
    onAdvanceStatus: (OrderStatus) -> Unit
) {
    val status = order.parsedStatus()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (status == OrderStatus.DELIVERED) MaterialTheme.colorScheme.outline else EmeraldPrimary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("accepted_order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldLightContainer
                ) {
                    Text(
                        text = "स्टेटस: ${status.labelHi}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldDark,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = AmberAccent) {
                    Text(
                        text = "PIN: ${order.pickupPin}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberOnContainer,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎓 ${order.studentName} (${order.hostelName}, रूम ${order.roomNumber})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "📱 ${order.studentMobile} • ${order.parsedDeliveryMode().labelHi} • ${order.parsedPaymentMethod().labelHi}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onCallStudent,
                    modifier = Modifier
                        .size(48.dp)
                        .background(EmeraldLightContainer, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "स्टूडेंट को कॉल करें",
                        tint = EmeraldDark
                    )
                }
            }

            if (order.itemsSummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = order.itemsSummary.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (order.customNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
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
                    playing = playingAudioKey == "acc_${order.id}",
                    onClick = onPlayVoice
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1-Tap Status Progression Button: पैक हो गया -> हॉस्टल गेट पर आ गया -> डिलीवर हो गया
            when (status) {
                OrderStatus.ACCEPTED -> {
                    Button(
                        onClick = { onAdvanceStatus(OrderStatus.PACKING) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("mark_packing_${order.id}")
                    ) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📦 पैक हो गया", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }

                OrderStatus.PACKING -> {
                    Button(
                        onClick = { onAdvanceStatus(OrderStatus.AT_GATE) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberAccent,
                            contentColor = AmberOnContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("mark_at_gate_${order.id}")
                    ) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📍 हॉस्टल गेट पर आ गया", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }

                OrderStatus.AT_GATE -> {
                    Button(
                        onClick = { onAdvanceStatus(OrderStatus.DELIVERED) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("mark_delivered_${order.id}")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✓ डिलीवर हो गया", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }

                OrderStatus.DELIVERED -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldLightContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✅ यह ऑर्डर सफलतापूर्वक डिलीवर हो चुका है",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterShopModal(
    submitting: Boolean,
    onDismiss: () -> Unit,
    onRegister: (shopName: String, category: String, mobile: String, distance: String) -> Unit
) {
    var shopName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(HostelCatalog.shopCategories.first()) }
    var mobile by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("50m") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🏪 नई दुकान रजिस्टर करें",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "बंद करें")
                    }
                }

                Text(
                    text = "रजिस्टर करते ही आपकी दुकान स्टूडेंट ऐप में लाइव दिखने लगेगी।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it.take(80); validationError = null },
                    label = { Text("दुकान का नाम (Shop Name)") },
                    placeholder = { Text("जैसे: शर्मा किराना व जनरल स्टोर") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "दुकान की कैटेगरी चुनें",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HostelCatalog.shopCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { input ->
                        mobile = input.filter { it.isDigit() }.take(10)
                        validationError = null
                    },
                    label = { Text("10 अंकों का मोबाइल नंबर") },
                    placeholder = { Text("9123456789") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_mobile_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it.take(30); validationError = null },
                    label = { Text("हॉस्टल गेट से दूरी (Distance)") },
                    placeholder = { Text("जैसे: 50m या 100m") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_distance_input")
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusDanger,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        when {
                            shopName.trim().length < 2 -> validationError = "कृपया दुकान का नाम लिखें"
                            !mobile.matches(Regex("^[0-9]{10}$")) -> validationError = "कृपया 10 अंकों का सही मोबाइल नंबर डालें"
                            distance.trim().isEmpty() -> validationError = "कृपया हॉस्टल से दूरी लिखें"
                            else -> onRegister(shopName.trim(), category, mobile.trim(), distance.trim())
                        }
                    },
                    enabled = !submitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_register_shop_button")
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "✓ दुकान रजिस्टर करें",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
