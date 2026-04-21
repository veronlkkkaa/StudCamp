package com.example.studcampapp.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.studcampapp.model.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object UserStore {
    private val json = Json { ignoreUnknownKeys = true }
    private var prefs: SharedPreferences? = null

    var currentUser by mutableStateOf<User?>(null)
        private set
    var isGuest by mutableStateOf(false)
        private set
    var localAvatarUri by mutableStateOf<Uri?>(null)
        private set

    val isLoggedIn: Boolean get() = currentUser != null || isGuest

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences("user_store", Context.MODE_PRIVATE)
        load()
    }

    fun loginAsGuest() {
        currentUser = null
        isGuest = true
        localAvatarUri = null
        prefs?.edit()?.putBoolean("is_guest", true)?.remove("user_json")?.apply()
    }

    fun login(user: User) {
        currentUser = user
        isGuest = false
        localAvatarUri = null
        val userJson = runCatching { json.encodeToString(user) }.getOrNull()
        prefs?.edit()
            ?.putBoolean("is_guest", false)
            ?.putString("user_json", userJson)
            ?.apply()
    }

    fun logout() {
        currentUser = null
        isGuest = false
        localAvatarUri = null
        prefs?.edit()?.clear()?.apply()
    }

    fun updateAvatar(uri: Uri) {
        localAvatarUri = uri
    }

    fun updateProfile(login: String, phone: String) {
        val updated = currentUser?.copy(login = login, phone = phone) ?: return
        currentUser = updated
        val userJson = runCatching { json.encodeToString(updated) }.getOrNull()
        prefs?.edit()?.putString("user_json", userJson)?.apply()
    }

    private fun load() {
        val prefs = prefs ?: return
        if (prefs.getBoolean("is_guest", false)) {
            isGuest = true
            return
        }
        val userJson = prefs.getString("user_json", null) ?: return
        currentUser = runCatching { json.decodeFromString<User>(userJson) }.getOrNull()
    }
}
