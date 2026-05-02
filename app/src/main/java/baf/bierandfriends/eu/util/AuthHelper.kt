package baf.bierandfriends.eu.util

import com.google.firebase.auth.FirebaseAuth

object AuthHelper {

    private val auth = FirebaseAuth.getInstance()

    fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User ist nicht eingeloggt")
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
