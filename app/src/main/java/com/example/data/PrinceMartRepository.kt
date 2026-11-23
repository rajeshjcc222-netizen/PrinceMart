package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class PrinceMartRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google लॉगिन आवश्यक है।")
    }

    // ---------------------------------------------------------
    // 1. Student Profile (students/{uid})
    // ---------------------------------------------------------

    fun observeStudentProfile(userId: String): Flow<StudentProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "students/$userId"
        emitAll(
            db.collection("students").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            StudentProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, userId, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveStudentProfile(
        name: String,
        mobile: String,
        hostelName: String,
        roomNumber: String,
        deliveryMode: DeliveryMode,
        isExistingProfile: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "students/${uid ?: "unauthenticated"}"
        return try {
            val verifiedUid = requireUserId()
            val cleanName = name.trim()
            val cleanMobile = mobile.trim()
            val cleanHostel = hostelName.trim()
            val cleanRoom = roomNumber.trim()

            require(cleanName.length in 2..60) { "नाम 2 से 60 अक्षरों के बीच होना चाहिए" }
            require(cleanMobile.matches(Regex("^[0-9]{10}$"))) { "कृपया 10 अंकों का सही मोबाइल नंबर डालें" }
            require(cleanHostel.length in 2..80) { "कृपया हॉस्टल का नाम लिखें" }
            require(cleanRoom.length in 1..20) { "कृपया रूम नंबर लिखें" }

            val docRef = db.collection("students").document(verifiedUid)
            if (isExistingProfile) {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "mobile" to cleanMobile,
                        "hostelName" to cleanHostel,
                        "roomNumber" to cleanRoom,
                        "deliveryPreference" to deliveryMode.code,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to verifiedUid,
                        "name" to cleanName,
                        "mobile" to cleanMobile,
                        "hostelName" to cleanHostel,
                        "roomNumber" to cleanRoom,
                        "deliveryPreference" to deliveryMode.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isExistingProfile) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDeliveryPreference(deliveryMode: DeliveryMode): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "students/${uid ?: "unauthenticated"}"
        return try {
            val verifiedUid = requireUserId()
            db.collection("students").document(verifiedUid)
                .update(
                    mapOf(
                        "deliveryPreference" to deliveryMode.code,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // 2. Shops (shops/{shopId})
    // ---------------------------------------------------------

    fun observeListedShops(userId: String): Flow<List<Shop>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "shops"
        emitAll(
            db.collection("shops")
                .whereEqualTo("listed", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Shop::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<Shop> { it.shopOpen }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMyShops(userId: String): Flow<List<Shop>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "shops"
        emitAll(
            db.collection("shops")
                .whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Shop::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun registerShop(
        shopName: String,
        category: String,
        mobile: String,
        distance: String
    ): Result<String> {
        var path = "shops"
        return try {
            val uid = requireUserId()
            val cleanName = shopName.trim()
            val cleanCategory = category.trim()
            val cleanMobile = mobile.trim()
            val cleanDistance = distance.trim()

            require(cleanName.length in 2..80) { "दुकान का नाम कम से कम 2 अक्षरों का हो" }
            require(cleanCategory.length in 2..40) { "कृपया दुकान की कैटेगरी चुनें" }
            require(cleanMobile.matches(Regex("^[0-9]{10}$"))) { "कृपया 10 अंकों का सही मोबाइल नंबर डालें" }
            require(cleanDistance.length in 1..30) { "कृपया हॉस्टल से दूरी लिखें (जैसे 50m)" }

            val docRef = db.collection("shops").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "ownerId" to uid,
                    "shopName" to cleanName,
                    "category" to cleanCategory,
                    "mobile" to cleanMobile,
                    "distance" to cleanDistance,
                    "shopOpen" to true,
                    "listed" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleShopOpen(shopId: String, open: Boolean): Result<Unit> {
        val path = "shops/$shopId"
        return try {
            requireUserId()
            require(shopId.isNotBlank()) { "Shop ID blank" }
            db.collection("shops").document(shopId)
                .update(
                    mapOf(
                        "shopOpen" to open,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // 3. Orders (orders/{orderId})
    // ---------------------------------------------------------

    fun observeStudentOrders(userId: String): Flow<List<Order>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("studentUid", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeOpenCommonOrders(userId: String): Flow<List<Order>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("broadcastOpen", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeDirectOrdersForOwner(userId: String): Flow<List<Order>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("targetShopOwnerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAcceptedOrdersForOwner(userId: String): Flow<List<Order>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("acceptedShopOwnerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun placeOrder(
        student: StudentProfile,
        orderMode: OrderMode,
        targetShop: Shop?,
        deliveryMode: DeliveryMode,
        paymentMethod: PaymentMethod,
        itemsSummary: List<String>,
        totalAmount: Int,
        customNote: String,
        voiceNoteBase64: String,
        voiceNoteDurationSec: Int
    ): Result<String> {
        var path = "orders"
        return try {
            val uid = requireUserId()
            require(student.hasCompleteDetails()) { "पहले स्टूडेंट प्रोफ़ाइल पूरी करें" }

            val cleanNote = customNote.trim().take(500)
            val boundedItems = itemsSummary.take(30)
            val cleanVoice = voiceNoteBase64.trim()
            val boundedDuration = voiceNoteDurationSec.coerceIn(0, 60)

            require(boundedItems.isNotEmpty() || cleanNote.isNotEmpty() || cleanVoice.isNotEmpty()) {
                "कम से कम एक आइटम चुनें, टेक्स्ट लिखें या वॉइस नोट रिकॉर्ड करें"
            }
            require(cleanVoice.length <= 300_000) {
                "वॉइस नोट बहुत बड़ा है, कृपया 30 सेकंड के अंदर रिकॉर्ड करें"
            }

            if (orderMode == OrderMode.DIRECT) {
                require(targetShop != null && targetShop.id.isNotBlank() && targetShop.ownerId.isNotBlank()) {
                    "कृपया एक खुली दुकान चुनें"
                }
            }

            val pin = Random.nextInt(1000, 10000).toString()
            val docRef = db.collection("orders").document()
            path = docRef.path

            val payload = mapOf(
                "studentUid" to uid,
                "studentName" to student.name.trim().take(60),
                "studentMobile" to student.mobile.trim(),
                "hostelName" to student.hostelName.trim().take(80),
                "roomNumber" to student.roomNumber.trim().take(20),
                "orderMode" to orderMode.code,
                "broadcastOpen" to (orderMode == OrderMode.COMMON),
                "deliveryMode" to deliveryMode.code,
                "paymentMethod" to paymentMethod.code,
                "pickupPin" to pin,
                "itemsSummary" to boundedItems,
                "totalAmount" to totalAmount.coerceIn(0, 50000).toLong(),
                "customNote" to cleanNote,
                "voiceNoteBase64" to cleanVoice,
                "voiceNoteDurationSec" to boundedDuration.toLong(),
                "targetShopId" to if (orderMode == OrderMode.DIRECT) targetShop!!.id else "",
                "targetShopName" to if (orderMode == OrderMode.DIRECT) targetShop!!.shopName.take(80) else "",
                "targetShopOwnerId" to if (orderMode == OrderMode.DIRECT) targetShop!!.ownerId else "",
                "status" to OrderStatus.PENDING.code,
                "acceptedShopId" to "",
                "acceptedShopName" to "",
                "acceptedShopMobile" to "",
                "acceptedShopOwnerId" to "",
                "acceptedShopDistance" to "",
                "skippedShopIds" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(pin)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptOrder(orderId: String, shop: Shop): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            val uid = requireUserId()
            require(orderId.isNotBlank() && shop.id.isNotBlank()) { "ऑर्डर या दुकान की जानकारी अधूरी है" }
            check(shop.ownerId == uid) { "आप केवल अपनी दुकान से ऑर्डर स्वीकार कर सकते हैं" }

            val orderRef = db.collection("orders").document(orderId)
            db.runTransaction { tx ->
                val snap = tx.get(orderRef)
                val currentStatus = snap.getString("status") ?: ""
                if (currentStatus != OrderStatus.PENDING.code) {
                    throw IllegalStateException("यह ऑर्डर पहले ही स्वीकार या रद्द हो चुका है।")
                }
                tx.update(
                    orderRef,
                    mapOf(
                        "status" to OrderStatus.ACCEPTED.code,
                        "broadcastOpen" to false,
                        "acceptedShopId" to shop.id,
                        "acceptedShopName" to shop.shopName.take(80),
                        "acceptedShopMobile" to shop.mobile,
                        "acceptedShopOwnerId" to uid,
                        "acceptedShopDistance" to shop.distance.take(30),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun skipCommonOrder(orderId: String, shopId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank() && shopId.isNotBlank())
            db.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "skippedShopIds" to FieldValue.arrayUnion(shopId),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun declineDirectOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank())
            db.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.DECLINED.code,
                        "broadcastOpen" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceOrderStatus(orderId: String, nextStatus: OrderStatus): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank())
            require(nextStatus in listOf(OrderStatus.PACKING, OrderStatus.AT_GATE, OrderStatus.DELIVERED)) {
                "अमान्य ऑर्डर स्टेटस"
            }
            db.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to nextStatus.code,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelPendingOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank())
            db.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.CANCELLED.code,
                        "broadcastOpen" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfSignedIn(
        error: Throwable,
        uid: String,
        operationType: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
