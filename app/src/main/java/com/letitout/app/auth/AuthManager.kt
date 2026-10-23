package com.letitout.app.auth

import android.app.Activity
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(@ApplicationContext private val context: Context) {
    fun isConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()
    fun isGoogleConfigured(): Boolean = isConfigured() && webClientId().isNotBlank()
    fun isSignedIn(): Boolean = isConfigured() && FirebaseAuth.getInstance().currentUser != null
    fun isGuest(): Boolean = isSignedIn() && FirebaseAuth.getInstance().currentUser?.isAnonymous == true
    fun userName(): String? = if (isConfigured()) FirebaseAuth.getInstance().currentUser?.displayName else null
    fun userEmail(): String? = if (isConfigured()) FirebaseAuth.getInstance().currentUser?.email else null

    suspend fun signInGuest() {
        check(isConfigured()) { "Add Firebase configuration to enable guest sign-in." }
        FirebaseAuth.getInstance().signInAnonymously().await()
    }

    suspend fun signInGoogle(activity: Activity) {
        check(isConfigured()) { "Add Firebase configuration to enable Google sign-in." }
        val clientId = webClientId()
        check(clientId.isNotBlank()) { "Enable Google sign-in in Firebase and download the updated google-services.json." }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
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
        val auth = if (isConfigured()) FirebaseAuth.getInstance() else null
        val hadGoogleAccount = auth?.currentUser?.isAnonymous == false
        auth?.signOut()
        if (hadGoogleAccount) clearGoogleCredentialState()
    }

    suspend fun deleteAccount() {
        check(isSignedIn()) { "No authenticated account to delete." }
        val user = FirebaseAuth.getInstance().currentUser!!
        val hadGoogleAccount = !user.isAnonymous
        user.delete().await()
        if (hadGoogleAccount) clearGoogleCredentialState()
    }

    private suspend fun clearGoogleCredentialState() {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            // Firebase sign-out/deletion has already succeeded. Credential providers are optional.
            Log.w("AuthManager", "Could not clear the Google credential state", error)
        }
    }

    private fun webClientId(): String {
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val generatedId = if (resourceId != 0) context.getString(resourceId) else ""
        return generatedId.ifBlank { BuildConfig.WEB_CLIENT_ID }
    }
}
