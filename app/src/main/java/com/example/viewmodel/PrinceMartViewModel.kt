package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RecordedVoiceNote
import com.example.audio.VoiceNoteManager
import com.example.data.AppRole
import com.example.data.DeliveryMode
import com.example.data.HostelCatalog
import com.example.data.LocalPrefsStore
import com.example.data.Order
import com.example.data.OrderMode
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PrinceMartRepository
import com.example.data.Shop
import com.example.data.StudentProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ToastTone {
    SUCCESS,
    INFO,
    ERROR
}

data class ToastMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val tone: ToastTone = ToastTone.INFO
)

sealed interface StreamState<out T> {
    data object Loading : StreamState<Nothing>
    data class Success<T>(val data: T) : StreamState<T>
    data class Error(val message: String) : StreamState<Nothing>
}

data class PrinceMartUiState(
    val activeRole: AppRole = AppRole.STUDENT,
    val deliveryMode: DeliveryMode = DeliveryMode.HOSTEL_GATE,
    val searchQuery: String = "",
    val selectedCategory: String = "सभी",
    val cartQuantities: Map<String, Int> = emptyMap(),
    val composerNote: String = "",
    val composerOrderMode: OrderMode = OrderMode.COMMON,
    val composerTargetShopId: String = "",
    val recordedVoiceNote: RecordedVoiceNote? = null,
    val recordingActive: Boolean = false,
    val recordingSeconds: Int = 0,
    val playingAudioKey: String? = null,
    val selectedShopId: String = "",
    val showStudentProfileModal: Boolean = false,
    val showCheckoutModal: Boolean = false,
    val showRegisterShopModal: Boolean = false,
    val submitting: Boolean = false,
    val toast: ToastMessage? = null
) {
    val totalCartItems: Int
        get() = cartQuantities.values.sum()

    val totalCartAmount: Int
        get() = cartQuantities.entries.sumOf { (itemId, qty) ->
            val price = HostelCatalog.items.firstOrNull { it.id == itemId }?.priceInr ?: 0
            price * qty
        }
}

class PrinceMartViewModel(
    private val repository: PrinceMartRepository,
    private val localPrefs: LocalPrefsStore,
    private val voiceNoteManager: VoiceNoteManager,
    private val currentUserId: String
) : ViewModel() {

    private val initialDraft = localPrefs.loadStudentDraft(currentUserId)
    private val _uiState = MutableStateFlow(
        PrinceMartUiState(
            deliveryMode = DeliveryMode.fromCode(initialDraft?.deliveryPreference),
            selectedShopId = localPrefs.loadSelectedShopId(currentUserId)
        )
    )
    val uiState: StateFlow<PrinceMartUiState> = _uiState.asStateFlow()

    val studentProfileState: StateFlow<StreamState<StudentProfile?>> =
        repository.observeStudentProfile(currentUserId)
            .map<StudentProfile?, StreamState<StudentProfile?>> { remote ->
                if (remote != null) {
                    localPrefs.saveStudentDraft(currentUserId, remote)
                    _uiState.update { state ->
                        state.copy(deliveryMode = DeliveryMode.fromCode(remote.deliveryPreference))
                    }
                    StreamState.Success(remote)
                } else {
                    StreamState.Success(localPrefs.loadStudentDraft(currentUserId))
                }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing student profile", rootError)
                emit(StreamState.Error(rootError.localizedMessage ?: "प्रोफ़ाइल लोड नहीं हो सकी"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StreamState.Success(initialDraft)
            )

    val listedShopsState: StateFlow<StreamState<List<Shop>>> =
        repository.observeListedShops(currentUserId)
            .map<List<Shop>, StreamState<List<Shop>>> { StreamState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing listed shops", rootError)
                emit(StreamState.Error(rootError.localizedMessage ?: "दुकानें लोड नहीं हो सकीं"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StreamState.Loading
            )

    val myShopsState: StateFlow<StreamState<List<Shop>>> =
        repository.observeMyShops(currentUserId)
            .map<List<Shop>, StreamState<List<Shop>>> { shops ->
                if (shops.isNotEmpty()) {
                    val currentSelected = _uiState.value.selectedShopId
                    if (shops.none { it.id == currentSelected }) {
                        val firstId = shops.first().id
                        localPrefs.saveSelectedShopId(currentUserId, firstId)
                        _uiState.update { it.copy(selectedShopId = firstId) }
                    }
                }
                StreamState.Success(shops)
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing owned shops", rootError)
                emit(StreamState.Error(rootError.localizedMessage ?: "आपकी दुकान लोड नहीं हो सकी"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StreamState.Loading
            )

    val studentOrdersState: StateFlow<StreamState<List<Order>>> =
        repository.observeStudentOrders(currentUserId)
            .map<List<Order>, StreamState<List<Order>>> { StreamState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing student orders", rootError)
                emit(StreamState.Error(rootError.localizedMessage ?: "आपके ऑर्डर लोड नहीं हो सके"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StreamState.Loading
            )

    private var seenIncomingIds: Set<String> = emptySet()
    private var initialIncomingLoaded = false

    val incomingShopOrdersState: StateFlow<StreamState<List<Order>>> =
        combine(
            repository.observeOpenCommonOrders(currentUserId),
            repository.observeDirectOrdersForOwner(currentUserId),
            myShopsState,
            _uiState
        ) { commonOrders, directOrders, myShopsStream, state ->
            val myShops = (myShopsStream as? StreamState.Success)?.data.orEmpty()
            val activeShop = myShops.firstOrNull { it.id == state.selectedShopId } ?: myShops.firstOrNull()
            val activeShopId = activeShop?.id.orEmpty()

            val merged = (commonOrders + directOrders)
                .distinctBy { it.id }
                .filter { order ->
                    if (order.status != OrderStatus.PENDING.code) return@filter false
                    if (order.orderMode == OrderMode.COMMON.code) {
                        activeShopId.isBlank() || activeShopId !in order.skippedShopIds
                    } else {
                        activeShopId.isBlank() || order.targetShopId == activeShopId
                    }
                }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }

            val currentIds = merged.map { it.id }.toSet()
            if (initialIncomingLoaded) {
                val brandNew = currentIds - seenIncomingIds
                if (brandNew.isNotEmpty() && myShops.isNotEmpty()) {
                    voiceNoteManager.playIncomingOrderAlert()
                    if (state.activeRole == AppRole.SHOPKEEPER) {
                        showToast("🔔 नया ऑर्डर आया है!", ToastTone.INFO)
                    }
                }
            } else {
                initialIncomingLoaded = true
            }
            seenIncomingIds = currentIds
            StreamState.Success(merged) as StreamState<List<Order>>
        }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing incoming shop orders", rootError)
                emit(StreamState.Error(rootError.localizedMessage ?: "नए ऑर्डर लोड नहीं हो सके"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StreamState.Loading
            )

    val acceptedShopOrdersState: StateFlow<StreamState<List<Order>>> =
        repository.observeAcceptedOrdersForOwner(currentUserId)
            .map<List<Order>, StreamState<List<Order>>> { StreamState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing accepted orders", rootError)
                emit(StreamState.Error(rootError.localizedMessage ?: "स्वीकार ऑर्डर लोड नहीं हो सके"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = StreamState.Loading
            )

    private var toastJob: Job? = null
    private var recordingTimerJob: Job? = null

    fun switchRole(role: AppRole) {
        stopAudioPlayback()
        _uiState.update { it.copy(activeRole = role) }
    }

    fun setDeliveryMode(mode: DeliveryMode) {
        _uiState.update { it.copy(deliveryMode = mode) }
        val profile = (studentProfileState.value as? StreamState.Success)?.data
        if (profile != null && profile.hasCompleteDetails()) {
            localPrefs.saveStudentDraft(currentUserId, profile.copy(deliveryPreference = mode.code))
            viewModelScope.launch {
                repository.updateDeliveryPreference(mode)
            }
        }
        showToast("डिलीवरी मोड: ${mode.labelHi}", ToastTone.INFO)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun incrementCartItem(itemId: String) {
        _uiState.update { state ->
            val current = state.cartQuantities[itemId] ?: 0
            if (current >= 20) return@update state
            state.copy(cartQuantities = state.cartQuantities + (itemId to (current + 1)))
        }
    }

    fun decrementCartItem(itemId: String) {
        _uiState.update { state ->
            val current = state.cartQuantities[itemId] ?: 0
            val next = if (current <= 1) {
                state.cartQuantities - itemId
            } else {
                state.cartQuantities + (itemId to (current - 1))
            }
            state.copy(cartQuantities = next)
        }
    }

    fun updateComposerNote(text: String) {
        _uiState.update { it.copy(composerNote = text.take(500)) }
    }

    fun selectCommonTarget() {
        _uiState.update {
            it.copy(composerOrderMode = OrderMode.COMMON, composerTargetShopId = "")
        }
    }

    fun selectSpecificShopTarget(shop: Shop) {
        _uiState.update {
            it.copy(composerOrderMode = OrderMode.DIRECT, composerTargetShopId = shop.id)
        }
        showToast("दुकान चुनी गई: ${shop.shopName}", ToastTone.INFO)
    }

    fun selectActiveOwnedShop(shopId: String) {
        localPrefs.saveSelectedShopId(currentUserId, shopId)
        _uiState.update { it.copy(selectedShopId = shopId) }
    }

    fun openStudentProfileModal() {
        _uiState.update { it.copy(showStudentProfileModal = true) }
    }

    fun closeStudentProfileModal() {
        _uiState.update { it.copy(showStudentProfileModal = false) }
    }

    fun openCheckoutModal() {
        val profile = (studentProfileState.value as? StreamState.Success)?.data
        if (profile == null || !profile.hasCompleteDetails()) {
            _uiState.update { it.copy(showStudentProfileModal = true) }
            showToast("ऑर्डर करने से पहले अपनी हॉस्टल प्रोफ़ाइल भरें", ToastTone.INFO)
            return
        }
        _uiState.update { it.copy(showCheckoutModal = true) }
    }

    fun closeCheckoutModal() {
        _uiState.update { it.copy(showCheckoutModal = false) }
    }

    fun openRegisterShopModal() {
        _uiState.update { it.copy(showRegisterShopModal = true) }
    }

    fun closeRegisterShopModal() {
        _uiState.update { it.copy(showRegisterShopModal = false) }
    }

    // ---------------------------------------------------------
    // Audio Recording & Playback
    // ---------------------------------------------------------

    fun toggleVoiceRecording() {
        if (_uiState.value.recordingActive) {
            finishVoiceRecording()
        } else {
            startVoiceRecording()
        }
    }

    private fun startVoiceRecording() {
        stopAudioPlayback()
        val started = voiceNoteManager.startRecording()
        if (!started) {
            showToast("माइक शुरू नहीं हो सका, कृपया अनुमति जांचें", ToastTone.ERROR)
            return
        }
        _uiState.update { it.copy(recordingActive = true, recordingSeconds = 0) }
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            var sec = 0
            while (isActive && sec < 30) {
                delay(1000L)
                sec++
                _uiState.update { it.copy(recordingSeconds = sec) }
            }
            if (_uiState.value.recordingActive) {
                finishVoiceRecording()
            }
        }
    }

    private fun finishVoiceRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        val recorded = voiceNoteManager.stopRecording()
        _uiState.update {
            it.copy(
                recordingActive = false,
                recordingSeconds = 0,
                recordedVoiceNote = recorded ?: it.recordedVoiceNote
            )
        }
        if (recorded != null) {
            showToast("🎤 वॉइस नोट तैयार है (${recorded.durationSec}s)", ToastTone.SUCCESS)
        } else {
            showToast("वॉइस नोट सेव नहीं हो सका, फिर से बोलें", ToastTone.ERROR)
        }
    }

    fun clearRecordedVoiceNote() {
        stopAudioPlayback()
        _uiState.update { it.copy(recordedVoiceNote = null) }
    }

    fun togglePlayVoiceNote(audioKey: String, base64Audio: String) {
        if (_uiState.value.playingAudioKey == audioKey) {
            stopAudioPlayback()
            return
        }
        if (_uiState.value.recordingActive) {
            finishVoiceRecording()
        }
        _uiState.update { it.copy(playingAudioKey = audioKey) }
        voiceNoteManager.playBase64(
            base64Audio = base64Audio,
            onCompleted = {
                _uiState.update { state ->
                    if (state.playingAudioKey == audioKey) state.copy(playingAudioKey = null) else state
                }
            },
            onError = {
                _uiState.update { it.copy(playingAudioKey = null) }
                showToast("ऑडियो चलाने में दिक्कत आई", ToastTone.ERROR)
            }
        )
    }

    fun stopAudioPlayback() {
        voiceNoteManager.stopPlayback()
        _uiState.update { it.copy(playingAudioKey = null) }
    }

    // ---------------------------------------------------------
    // Firestore Mutations (All Crash-Safe with Result<T>)
    // ---------------------------------------------------------

    fun saveStudentProfile(
        name: String,
        mobile: String,
        hostelName: String,
        roomNumber: String
    ) {
        val currentRemote = (studentProfileState.value as? StreamState.Success)?.data
        val isExistingRemote = currentRemote?.createdAt != null
        val mode = _uiState.value.deliveryMode

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            val result = repository.saveStudentProfile(
                name = name,
                mobile = mobile,
                hostelName = hostelName,
                roomNumber = roomNumber,
                deliveryMode = mode,
                isExistingProfile = isExistingRemote
            )
            _uiState.update { it.copy(submitting = false) }
            result.onSuccess {
                localPrefs.saveStudentDraft(
                    currentUserId,
                    StudentProfile(
                        id = currentUserId,
                        userId = currentUserId,
                        name = name.trim(),
                        mobile = mobile.trim(),
                        hostelName = hostelName.trim(),
                        roomNumber = roomNumber.trim(),
                        deliveryPreference = mode.code
                    )
                )
                _uiState.update { it.copy(showStudentProfileModal = false) }
                showToast("✓ स्टूडेंट प्रोफ़ाइल सेव हो गई!", ToastTone.SUCCESS)
            }.onFailure { err ->
                showToast(err.message ?: "प्रोफ़ाइल सेव नहीं हो सकी", ToastTone.ERROR)
            }
        }
    }

    fun submitVoiceOrTextOrder(paymentMethod: PaymentMethod = PaymentMethod.UPI_GATE) {
        val state = _uiState.value
        val profile = (studentProfileState.value as? StreamState.Success)?.data
        if (profile == null || !profile.hasCompleteDetails()) {
            _uiState.update { it.copy(showStudentProfileModal = true) }
            showToast("पहले अपनी स्टूडेंट प्रोफ़ाइल (नाम, मोबाइल, हॉस्टल) भरें", ToastTone.INFO)
            return
        }
        val note = state.composerNote.trim()
        val voice = state.recordedVoiceNote
        if (note.isBlank() && voice == null) {
            showToast("कृपया सामान लिखें या वॉइस नोट रिकॉर्ड करें", ToastTone.ERROR)
            return
        }

        val shops = (listedShopsState.value as? StreamState.Success)?.data.orEmpty()
        val targetShop = if (state.composerOrderMode == OrderMode.DIRECT) {
            shops.firstOrNull { it.id == state.composerTargetShopId }
        } else {
            null
        }
        if (state.composerOrderMode == OrderMode.DIRECT && targetShop == null) {
            showToast("कृपया एक दुकान चुनें या Common Broadcast चुनें", ToastTone.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            val result = repository.placeOrder(
                student = profile,
                orderMode = state.composerOrderMode,
                targetShop = targetShop,
                deliveryMode = state.deliveryMode,
                paymentMethod = paymentMethod,
                itemsSummary = emptyList(),
                totalAmount = 0,
                customNote = note,
                voiceNoteBase64 = voice?.base64.orEmpty(),
                voiceNoteDurationSec = voice?.durationSec ?: 0
            )
            _uiState.update { it.copy(submitting = false) }
            result.onSuccess { pin ->
                stopAudioPlayback()
                _uiState.update {
                    it.copy(composerNote = "", recordedVoiceNote = null)
                }
                showToast("✓ ऑर्डर भेज दिया गया! आपका पिकअप PIN: $pin", ToastTone.SUCCESS)
            }.onFailure { err ->
                showToast(err.message ?: "ऑर्डर नहीं भेजा जा सका", ToastTone.ERROR)
            }
        }
    }

    fun submitCartCheckout(
        orderMode: OrderMode,
        targetShop: Shop?,
        paymentMethod: PaymentMethod,
        extraNote: String
    ) {
        val state = _uiState.value
        val profile = (studentProfileState.value as? StreamState.Success)?.data
        if (profile == null || !profile.hasCompleteDetails()) {
            _uiState.update { it.copy(showCheckoutModal = false, showStudentProfileModal = true) }
            showToast("पहले स्टूडेंट प्रोफ़ाइल भरें", ToastTone.INFO)
            return
        }
        val lines = state.cartQuantities.mapNotNull { (itemId, qty) ->
            val item = HostelCatalog.items.firstOrNull { it.id == itemId } ?: return@mapNotNull null
            "${item.nameHi} x$qty (₹${item.priceInr * qty})"
        }
        if (lines.isEmpty()) {
            showToast("कार्ट खाली है", ToastTone.ERROR)
            return
        }
        if (orderMode == OrderMode.DIRECT && targetShop == null) {
            showToast("कृपया दुकान चुनें या Common Broadcast चुनें", ToastTone.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            val result = repository.placeOrder(
                student = profile,
                orderMode = orderMode,
                targetShop = targetShop,
                deliveryMode = state.deliveryMode,
                paymentMethod = paymentMethod,
                itemsSummary = lines,
                totalAmount = state.totalCartAmount,
                customNote = extraNote.trim(),
                voiceNoteBase64 = state.recordedVoiceNote?.base64.orEmpty(),
                voiceNoteDurationSec = state.recordedVoiceNote?.durationSec ?: 0
            )
            _uiState.update { it.copy(submitting = false) }
            result.onSuccess { pin ->
                stopAudioPlayback()
                _uiState.update {
                    it.copy(
                        cartQuantities = emptyMap(),
                        recordedVoiceNote = null,
                        showCheckoutModal = false
                    )
                }
                showToast("🎉 ऑर्डर कन्फर्म! पिकअप PIN: $pin", ToastTone.SUCCESS)
            }.onFailure { err ->
                showToast(err.message ?: "चेकआउट विफल रहा", ToastTone.ERROR)
            }
        }
    }

    fun registerShop(
        shopName: String,
        category: String,
        mobile: String,
        distance: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            val result = repository.registerShop(shopName, category, mobile, distance)
            _uiState.update { it.copy(submitting = false) }
            result.onSuccess { newShopId ->
                localPrefs.saveSelectedShopId(currentUserId, newShopId)
                _uiState.update {
                    it.copy(selectedShopId = newShopId, showRegisterShopModal = false)
                }
                showToast("🏪 दुकान रजिस्टर हो गई! अब स्टूडेंट ऐप में लाइव है", ToastTone.SUCCESS)
            }.onFailure { err ->
                showToast(err.message ?: "दुकान रजिस्टर नहीं हो सकी", ToastTone.ERROR)
            }
        }
    }

    fun toggleShopOpen(shop: Shop) {
        val nextOpen = !shop.shopOpen
        viewModelScope.launch {
            val result = repository.toggleShopOpen(shop.id, nextOpen)
            result.onSuccess {
                showToast(
                    if (nextOpen) "🟢 ${shop.shopName} अब OPEN है" else "🔴 ${shop.shopName} अब CLOSED है",
                    ToastTone.INFO
                )
            }.onFailure { err ->
                showToast(err.message ?: "स्टेटस नहीं बदल सका", ToastTone.ERROR)
            }
        }
    }

    fun acceptIncomingOrder(order: Order, shop: Shop) {
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            val result = repository.acceptOrder(order.id, shop)
            _uiState.update { it.copy(submitting = false) }
            result.onSuccess {
                showToast("✓ ऑर्डर स्वीकार कर लिया गया! अब पैकिंग शुरू करें", ToastTone.SUCCESS)
            }.onFailure { err ->
                showToast(err.message ?: "ऑर्डर स्वीकार नहीं हो सका", ToastTone.ERROR)
            }
        }
    }

    fun skipOrDeclineIncomingOrder(order: Order, shop: Shop) {
        viewModelScope.launch {
            val result = if (order.parsedOrderMode() == OrderMode.COMMON) {
                repository.skipCommonOrder(order.id, shop.id)
            } else {
                repository.declineDirectOrder(order.id)
            }
            result.onSuccess {
                showToast("ऑर्डर स्किप कर दिया गया", ToastTone.INFO)
            }.onFailure { err ->
                showToast(err.message ?: "ऑर्डर स्किप नहीं हो सका", ToastTone.ERROR)
            }
        }
    }

    fun advanceOrderStatus(order: Order, nextStatus: OrderStatus) {
        viewModelScope.launch {
            val result = repository.advanceOrderStatus(order.id, nextStatus)
            result.onSuccess {
                showToast("स्टेटस अपडेट: ${nextStatus.labelHi}", ToastTone.SUCCESS)
            }.onFailure { err ->
                showToast(err.message ?: "स्टेटस अपडेट नहीं हो सका", ToastTone.ERROR)
            }
        }
    }

    fun cancelPendingOrder(orderId: String) {
        viewModelScope.launch {
            val result = repository.cancelPendingOrder(orderId)
            result.onSuccess {
                showToast("ऑर्डर रद्द कर दिया गया", ToastTone.INFO)
            }.onFailure { err ->
                showToast(err.message ?: "ऑर्डर रद्द नहीं हो सका", ToastTone.ERROR)
            }
        }
    }

    fun showToast(message: String, tone: ToastTone = ToastTone.INFO) {
        toastJob?.cancel()
        val toast = ToastMessage(text = message, tone = tone)
        _uiState.update { it.copy(toast = toast) }
        toastJob = viewModelScope.launch {
            delay(3200L)
            _uiState.update { state ->
                if (state.toast?.id == toast.id) state.copy(toast = null) else state
            }
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _uiState.update { it.copy(toast = null) }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        toastJob?.cancel()
        voiceNoteManager.releaseAll()
    }

    private companion object {
        const val TAG = "PrinceMartVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
