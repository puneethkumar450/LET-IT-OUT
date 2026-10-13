package com.letitout.app.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.letitout.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(@ApplicationContext private val context: Context) {
    fun isConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()
    fun isSignedIn(): Boolean = isConfigured() && FirebaseAuth.getInstance().currentUser != null
    fun userName(): String? = if (isConfigured()) FirebaseAuth.getInstance().currentUser?.displayName else null

    suspend fun signInGuest() {
        check(isConfigured()) { "Add Firebase configuration to enable guest sign-in." }
        FirebaseAuth.getInstance().signInAnonymously().await()
    }

    suspend fun signInGoogle(activity: Activity) {
        check(isConfigured()) { "Add Firebase configuration to enable Google sign-in." }
        check(BuildConfig.WEB_CLIENT_ID.isNotBlank()) { "Configure webClientId in local gradle.properties." }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "No Google ID token was returned."
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(google.idToken, null)
        val auth = FirebaseAuth.getInstance()
        val current = auth.currentUser
        if (current?.isAnonymous == true) {
            // Preserve anonymous identity/data when upgrading the account.
            current.linkWithCredential(firebaseCredential).await()
        } else {
            auth.signInWithCredential(firebaseCredential).await()
        }
    }

    suspend fun signOut() {
        if (isConfigured()) FirebaseAuth.getInstance().signOut()
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }

    suspend fun deleteAccount() {
        check(isSignedIn()) { "No authenticated account to delete." }
        FirebaseAuth.getInstance().currentUser!!.delete().await()
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
