package com.example.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DeliveryMode
import com.example.data.HostelCatalog
import com.example.data.OrderMode
import com.example.data.PaymentMethod
import com.example.data.Shop
import com.example.data.StudentProfile
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary

@Composable
fun StudentProfileModal(
    initialProfile: StudentProfile?,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, mobile: String, hostelName: String, roomNumber: String) -> Unit
) {
    var name by remember(initialProfile) { mutableStateOf(initialProfile?.name.orEmpty()) }
    var mobile by remember(initialProfile) { mutableStateOf(initialProfile?.mobile.orEmpty()) }
    var hostelName by remember(initialProfile) { mutableStateOf(initialProfile?.hostelName.orEmpty()) }
    var roomNumber by remember(initialProfile) { mutableStateOf(initialProfile?.roomNumber.orEmpty()) }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎓 स्टूडेंट प्रोफ़ाइल",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "बंद करें")
                    }
                }

                Text(
                    text = "दुकानदार आपसे संपर्क और हॉस्टल गेट पर डिलीवरी के लिए यह जानकारी देखेंगे।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60); validationError = null },
                    label = { Text("आपका नाम (Name)") },
                    placeholder = { Text("जैसे: राहुल शर्मा") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { input ->
                        mobile = input.filter { it.isDigit() }.take(10)
                        validationError = null
                    },
                    label = { Text("10 अंकों का मोबाइल नंबर") },
                    placeholder = { Text("9876543210") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_mobile_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = hostelName,
                    onValueChange = { hostelName = it.take(80); validationError = null },
                    label = { Text("हॉस्टल का नाम (Hostel Name)") },
                    placeholder = { Text("जैसे: टैगोर भवन / बॉयज़ हॉस्टल 2") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_hostel_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it.take(20); validationError = null },
                    label = { Text("रूम नंबर (Room Number)") },
                    placeholder = { Text("जैसे: 204 या B-12") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_room_input")
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        when {
                            name.trim().length < 2 -> validationError = "कृपया पूरा नाम लिखें (कम से कम 2 अक्षर)"
                            !mobile.matches(Regex("^[0-9]{10}$")) -> validationError = "कृपया सही 10 अंकों का मोबाइल नंबर डालें"
                            hostelName.trim().length < 2 -> validationError = "कृपया हॉस्टल का नाम लिखें"
                            roomNumber.trim().isEmpty() -> validationError = "कृपया रूम नंबर लिखें"
                            else -> onSave(name.trim(), mobile.trim(), hostelName.trim(), roomNumber.trim())
                        }
                    },
                    enabled = !submitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_student_profile_button")
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "✓ प्रोफ़ाइल सेव करें",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutModal(
    cartQuantities: Map<String, Int>,
    totalAmount: Int,
    shops: List<Shop>,
    initialOrderMode: OrderMode,
    initialTargetShopId: String,
    deliveryMode: DeliveryMode,
    submitting: Boolean,
    onChangeDeliveryMode: (DeliveryMode) -> Unit,
    onDismiss: () -> Unit,
    onConfirmOrder: (
        orderMode: OrderMode,
        targetShop: Shop?,
        paymentMethod: PaymentMethod,
        extraNote: String
    ) -> Unit
) {
    var orderMode by remember { mutableStateOf(initialOrderMode) }
    var targetShopId by remember { mutableStateOf(initialTargetShopId) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.UPI_GATE) }
    var extraNote by remember { mutableStateOf("") }

    val selectedShop = shops.firstOrNull { it.id == targetShopId }
    val cartLines = remember(cartQuantities) {
        cartQuantities.mapNotNull { (itemId, qty) ->
            val item = HostelCatalog.items.firstOrNull { it.id == itemId } ?: return@mapNotNull null
            Triple(item.nameHi, qty, item.priceInr * qty)
        }
    }

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
                        text = "🛒 ऑर्डर चेकआउट",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "बंद करें")
                    }
                }

                // Cart items summary box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        cartLines.forEach { (itemName, qty, subtotal) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$itemName × $qty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹$subtotal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "अनुमानित कुल राशि",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "₹$totalAmount",
                                style = MaterialTheme.typography.titleLarge,
                                color = EmeraldDark,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "किसे भेजें?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = orderMode == OrderMode.COMMON,
                        onClick = {
                            orderMode = OrderMode.COMMON
                            targetShopId = ""
                        },
                        label = { Text("📢 सभी खुली दुकानों को (Common)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberAccent,
                            selectedLabelColor = AmberOnContainer
                        ),
                        modifier = Modifier.testTag("checkout_target_common")
                    )
                    shops.filter { it.shopOpen }.forEach { shop ->
                        FilterChip(
                            selected = orderMode == OrderMode.DIRECT && targetShopId == shop.id,
                            onClick = {
                                orderMode = OrderMode.DIRECT
                                targetShopId = shop.id
                            },
                            label = { Text("🏪 ${shop.shopName}") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = EmeraldDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "डिलीवरी विकल्प",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeliveryMode.entries.forEach { mode ->
                        val selected = deliveryMode == mode
                        Surface(
                            onClick = { onChangeDeliveryMode(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = mode.labelHi,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) EmeraldDark else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "पेमेंट तरीका",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentOptionCard(
                        title = "UPI on Gate",
                        subtitle = "GPay / PhonePe",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        selected = paymentMethod == PaymentMethod.UPI_GATE,
                        onClick = { paymentMethod = PaymentMethod.UPI_GATE },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_upi_option")
                    )
                    PaymentOptionCard(
                        title = "Cash (COD)",
                        subtitle = "गेट पर नकद दें",
                        icon = Icons.Rounded.Payments,
                        selected = paymentMethod == PaymentMethod.CASH_COD,
                        onClick = { paymentMethod = PaymentMethod.CASH_COD },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_cod_option")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = extraNote,
                    onValueChange = { extraNote = it.take(300) },
                    label = { Text("कोई अतिरिक्त निर्देश (वैकल्पिक)") },
                    placeholder = { Text("जैसे: गेट पर पहुँचकर कॉल कर देना") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onConfirmOrder(
                            orderMode,
                            if (orderMode == OrderMode.DIRECT) selectedShop else null,
                            paymentMethod,
                            extraNote
                        )
                    },
                    enabled = !submitting && (orderMode == OrderMode.COMMON || selectedShop != null),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_checkout_button")
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ऑर्डर कन्फर्म करें • ₹$totalAmount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) EmeraldDark else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
