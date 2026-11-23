package com.example.data

import android.content.Context

class LocalPrefsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveStudentDraft(uid: String, profile: StudentProfile) {
        prefs.edit()
            .putString("${uid}_name", profile.name)
            .putString("${uid}_mobile", profile.mobile)
            .putString("${uid}_hostel", profile.hostelName)
            .putString("${uid}_room", profile.roomNumber)
            .putString("${uid}_delivery", profile.deliveryPreference)
            .apply()
    }

    fun loadStudentDraft(uid: String): StudentProfile? {
        val name = prefs.getString("${uid}_name", null) ?: return null
        val mobile = prefs.getString("${uid}_mobile", "") ?: ""
        val hostel = prefs.getString("${uid}_hostel", "") ?: ""
        val room = prefs.getString("${uid}_room", "") ?: ""
        val delivery = prefs.getString("${uid}_delivery", DeliveryMode.HOSTEL_GATE.code)
            ?: DeliveryMode.HOSTEL_GATE.code
        return StudentProfile(
            id = uid,
            userId = uid,
            name = name,
            mobile = mobile,
            hostelName = hostel,
            roomNumber = room,
            deliveryPreference = delivery
        )
    }

    fun saveSelectedShopId(uid: String, shopId: String) {
        prefs.edit().putString("${uid}_active_shop_id", shopId).apply()
    }

    fun loadSelectedShopId(uid: String): String {
        return prefs.getString("${uid}_active_shop_id", "") ?: ""
    }

    private companion object {
        const val PREFS_NAME = "princemart_local_prefs"
    }
}
