package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppRole {
    STUDENT,
    SHOPKEEPER
}

enum class DeliveryMode(val code: String, val labelHi: String) {
    HOSTEL_GATE("HOSTEL_GATE", "हॉस्टल गेट डिलीवरी"),
    SHOP_PICKUP("SHOP_PICKUP", "दुकान से पिकअप");

    companion object {
        fun fromCode(raw: String?): DeliveryMode =
            entries.firstOrNull { it.code == raw } ?: HOSTEL_GATE
    }
}

enum class OrderMode(val code: String) {
    COMMON("COMMON"),
    DIRECT("DIRECT");

    companion object {
        fun fromCode(raw: String?): OrderMode =
            entries.firstOrNull { it.code == raw } ?: COMMON
    }
}

enum class PaymentMethod(val code: String, val labelHi: String) {
    UPI_GATE("UPI_GATE", "UPI on Gate"),
    CASH_COD("CASH_COD", "Cash (COD)");

    companion object {
        fun fromCode(raw: String?): PaymentMethod =
            entries.firstOrNull { it.code == raw } ?: UPI_GATE
    }
}

enum class OrderStatus(val code: String, val labelHi: String, val stepIndex: Int) {
    PENDING("PENDING", "पेंडिंग", 0),
    ACCEPTED("ACCEPTED", "स्वीकार", 1),
    PACKING("PACKING", "पैक हो रहा", 2),
    AT_GATE("AT_GATE", "हॉस्टल गेट पर", 3),
    DELIVERED("DELIVERED", "डिलीवर", 4),
    CANCELLED("CANCELLED", "रद्द किया गया", -1),
    DECLINED("DECLINED", "दुकान ने मना किया", -1);

    companion object {
        fun fromCode(raw: String?): OrderStatus =
            entries.firstOrNull { it.code == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val mobile: String = "",
    val hostelName: String = "",
    val roomNumber: String = "",
    val deliveryPreference: String = DeliveryMode.HOSTEL_GATE.code,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasCompleteDetails(): Boolean =
        name.trim().length >= 2 &&
            mobile.matches(Regex("^[0-9]{10}$")) &&
            hostelName.trim().length >= 2 &&
            roomNumber.trim().isNotEmpty()
}

@Keep
@IgnoreExtraProperties
data class Shop(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val shopName: String = "",
    val category: String = "",
    val mobile: String = "",
    val distance: String = "",
    val shopOpen: Boolean = true,
    val listed: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val studentMobile: String = "",
    val hostelName: String = "",
    val roomNumber: String = "",
    val orderMode: String = OrderMode.COMMON.code,
    val broadcastOpen: Boolean = true,
    val deliveryMode: String = DeliveryMode.HOSTEL_GATE.code,
    val paymentMethod: String = PaymentMethod.UPI_GATE.code,
    val pickupPin: String = "0000",
    val itemsSummary: List<String> = emptyList(),
    val totalAmount: Long = 0L,
    val customNote: String = "",
    val voiceNoteBase64: String = "",
    val voiceNoteDurationSec: Long = 0L,
    val targetShopId: String = "",
    val targetShopName: String = "",
    val targetShopOwnerId: String = "",
    val status: String = OrderStatus.PENDING.code,
    val acceptedShopId: String = "",
    val acceptedShopName: String = "",
    val acceptedShopMobile: String = "",
    val acceptedShopOwnerId: String = "",
    val acceptedShopDistance: String = "",
    val skippedShopIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedStatus(): OrderStatus = OrderStatus.fromCode(status)
    fun parsedOrderMode(): OrderMode = OrderMode.fromCode(orderMode)
    fun parsedDeliveryMode(): DeliveryMode = DeliveryMode.fromCode(deliveryMode)
    fun parsedPaymentMethod(): PaymentMethod = PaymentMethod.fromCode(paymentMethod)
    fun hasVoiceNote(): Boolean = voiceNoteBase64.isNotBlank()
}

data class QuickItem(
    val id: String,
    val nameHi: String,
    val searchKeywords: String,
    val category: String,
    val unit: String,
    val priceInr: Int,
    val emoji: String
)

object HostelCatalog {
    val categories = listOf(
        "सभी",
        "नाश्ता",
        "ड्रिंक्स",
        "डेयरी",
        "स्टेशनरी",
        "हेल्थ",
        "डेली ज़रूरत"
    )

    val shopCategories = listOf(
        "किराना व जनरल",
        "कैंटीन व चाय",
        "स्टेशनरी व प्रिंट",
        "मेडिकल स्टोर",
        "स्नैक्स व बेकरी",
        "डेयरी व फ्रूट्स"
    )

    val items = listOf(
        QuickItem("maggi", "Maggi Masala", "maggi noodles मैगी नूडल्स", "नाश्ता", "70g पैक", 14, "🍜"),
        QuickItem("chai", "अदरक स्पेशल चाय", "tea chai चाय अदरक", "ड्रिंक्स", "1 कप", 15, "☕"),
        QuickItem("coffee", "हॉट कॉफ़ी", "coffee कॉफी", "ड्रिंक्स", "1 कप", 25, "🧋"),
        QuickItem("lays", "Lay's Magic Masala", "lays chips चिप्स", "नाश्ता", "50g पैक", 20, "🥔"),
        QuickItem("kurkure", "Kurkure Masala", "kurkure कुरकुरे स्नैक्स", "नाश्ता", "82g पैक", 20, "🌶️"),
        QuickItem("parleg", "Parle-G बिस्किट", "parle biscuit बिस्किट", "नाश्ता", "100g पैक", 10, "🍪"),
        QuickItem("colddrink", "Cold Drink (ThumsUp/Sprite)", "cold drink coke sprite pepsi कोल्ड ड्रिंक", "ड्रिंक्स", "750 ml", 45, "🥤"),
        QuickItem("water", "बिसलेरी पानी बोतल", "water bisleri पानी बोतल", "ड्रिंक्स", "1 लीटर", 20, "💧"),
        QuickItem("milk", "Amul ताज़ा दूध", "milk amul दूध डेयरी", "डेयरी", "500 ml", 28, "🥛"),
        QuickItem("bread", "ब्रेड पैकेट", "bread ब्रेड", "डेयरी", "1 पैक", 40, "🍞"),
        QuickItem("eggs", "उबले अंडे (2 पीस)", "egg boiled अंडे", "नाश्ता", "2 पीस", 24, "🥚"),
        QuickItem("samosa", "गरमा-गरम समोसा", "samosa समोसा कचौड़ी", "नाश्ता", "2 पीस", 30, "🥟"),
        QuickItem("pen", "बॉल पेन (नीला/काला)", "pen ball पेन", "स्टेशनरी", "1 पीस", 10, "🖊️"),
        QuickItem("notebook", "Classmate कॉपी", "notebook copy कॉपी रजिस्टर", "स्टेशनरी", "172 पेज", 65, "📓"),
        QuickItem("a4sheet", "A4 असाइनमेंट शीट", "a4 paper sheet print पेपर", "स्टेशनरी", "20 शीट", 30, "📄"),
        QuickItem("paracetamol", "Paracetamol 500mg", "paracetamol dolo fever medicine दवाई बुखार", "हेल्थ", "1 पत्ता", 18, "💊"),
        QuickItem("ors", "Electral ORS पैक", "ors electral ग्लूकोज", "हेल्थ", "1 पैकेट", 22, "🩹"),
        QuickItem("toothpaste", "टूथपेस्ट (छोटा)", "toothpaste colgate पेस्ट ब्रश", "डेली ज़रूरत", "50g ट्यूब", 35, "🪥"),
        QuickItem("soap", "नहाने का साबुन", "soap bathing साबुन", "डेली ज़रूरत", "1 पीस", 32, "🧼"),
        QuickItem("detergent", "सर्फ़ / डिटर्जेंट पाउच", "detergent surf washing सर्फ", "डेली ज़रूरत", "100g पैक", 15, "🧺")
    )
}
