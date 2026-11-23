package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.VoiceNoteManager
import com.example.auth.signOut
import com.example.data.AppRole
import com.example.data.LocalPrefsStore
import com.example.data.PrinceMartRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.components.InAppToastBanner
import com.example.ui.components.PrinceMartHeader
import com.example.ui.shopkeeper.RegisterShopModal
import com.example.ui.shopkeeper.ShopkeeperScreen
import com.example.ui.student.CheckoutModal
import com.example.ui.student.StudentProfileModal
import com.example.ui.student.StudentScreen
import com.example.ui.theme.PrinceMartTheme
import com.example.viewmodel.PrinceMartViewModel
import com.example.viewmodel.StreamState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrinceMartTheme {
                PrinceMartRoot()
            }
        }
    }
}

@Composable
fun PrinceMartRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedPrinceMartApp(
                currentUserId = uid,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedPrinceMartApp(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: PrinceMartViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PrinceMartViewModel(
                    repository = PrinceMartRepository(db),
                    localPrefs = LocalPrefsStore(app),
                    voiceNoteManager = VoiceNoteManager(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val studentProfileState by viewModel.studentProfileState.collectAsStateWithLifecycle()
    val listedShopsState by viewModel.listedShopsState.collectAsStateWithLifecycle()
    val myShopsState by viewModel.myShopsState.collectAsStateWithLifecycle()
    val studentOrdersState by viewModel.studentOrdersState.collectAsStateWithLifecycle()
    val incomingOrdersState by viewModel.incomingShopOrdersState.collectAsStateWithLifecycle()
    val acceptedOrdersState by viewModel.acceptedShopOrdersState.collectAsStateWithLifecycle()

    val pendingOrderCount = (incomingOrdersState as? StreamState.Success)?.data?.size ?: 0
    val listedShops = (listedShopsState as? StreamState.Success)?.data.orEmpty()
    val studentProfile = (studentProfileState as? StreamState.Success)?.data

    // Back press from Shopkeeper Panel returns smoothly to Student App
    BackHandler(enabled = uiState.activeRole == AppRole.SHOPKEEPER) {
        viewModel.switchRole(AppRole.STUDENT)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrinceMartHeader(
                activeRole = uiState.activeRole,
                pendingOrderCount = pendingOrderCount,
                onSelectRole = viewModel::switchRole,
                onSignOutClick = {
                    viewModel.stopAudioPlayback()
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope,
                        onSignOutComplete = onSignedOut
                    )
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.activeRole) {
                    AppRole.STUDENT -> {
                        StudentScreen(
                            uiState = uiState,
                            studentProfileState = studentProfileState,
                            shopsState = listedShopsState,
                            ordersState = studentOrdersState,
                            onSetDeliveryMode = viewModel::setDeliveryMode,
                            onOpenProfileModal = viewModel::openStudentProfileModal,
                            onUpdateComposerNote = viewModel::updateComposerNote,
                            onSelectCommonTarget = viewModel::selectCommonTarget,
                            onSelectSpecificShop = viewModel::selectSpecificShopTarget,
                            onToggleVoiceRecording = viewModel::toggleVoiceRecording,
                            onClearVoiceNote = viewModel::clearRecordedVoiceNote,
                            onTogglePlayAudio = viewModel::togglePlayVoiceNote,
                            onSubmitVoiceTextOrder = { viewModel.submitVoiceOrTextOrder() },
                            onCancelPendingOrder = viewModel::cancelPendingOrder,
                            onUpdateSearch = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectCategory,
                            onIncrementCart = viewModel::incrementCartItem,
                            onDecrementCart = viewModel::decrementCartItem,
                            onOpenCheckout = viewModel::openCheckoutModal,
                            onSwitchToShopkeeper = { viewModel.switchRole(AppRole.SHOPKEEPER) },
                            onShowToast = viewModel::showToast
                        )
                    }

                    AppRole.SHOPKEEPER -> {
                        ShopkeeperScreen(
                            uiState = uiState,
                            myShopsState = myShopsState,
                            incomingOrdersState = incomingOrdersState,
                            acceptedOrdersState = acceptedOrdersState,
                            onOpenRegisterShop = viewModel::openRegisterShopModal,
                            onSelectOwnedShop = viewModel::selectActiveOwnedShop,
                            onToggleShopOpen = viewModel::toggleShopOpen,
                            onAcceptOrder = viewModel::acceptIncomingOrder,
                            onSkipOrder = viewModel::skipOrDeclineIncomingOrder,
                            onAdvanceOrderStatus = viewModel::advanceOrderStatus,
                            onTogglePlayVoice = viewModel::togglePlayVoiceNote
                        )
                    }
                }
            }
        }

        // In-App Toast Banner (Zero browser/system alert popups)
        InAppToastBanner(
            toast = uiState.toast,
            onDismiss = viewModel::dismissToast,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }

    // Student Profile Modal
    if (uiState.showStudentProfileModal) {
        StudentProfileModal(
            initialProfile = studentProfile,
            submitting = uiState.submitting,
            onDismiss = viewModel::closeStudentProfileModal,
            onSave = viewModel::saveStudentProfile
        )
    }

    // Cart Checkout Modal
    if (uiState.showCheckoutModal) {
        CheckoutModal(
            cartQuantities = uiState.cartQuantities,
            totalAmount = uiState.totalCartAmount,
            shops = listedShops,
            initialOrderMode = uiState.composerOrderMode,
            initialTargetShopId = uiState.composerTargetShopId,
            deliveryMode = uiState.deliveryMode,
            submitting = uiState.submitting,
            onChangeDeliveryMode = viewModel::setDeliveryMode,
            onDismiss = viewModel::closeCheckoutModal,
            onConfirmOrder = viewModel::submitCartCheckout
        )
    }

    // Shopkeeper Registration Modal
    if (uiState.showRegisterShopModal) {
        RegisterShopModal(
            submitting = uiState.submitting,
            onDismiss = viewModel::closeRegisterShopModal,
            onRegister = viewModel::registerShop
        )
    }
}
