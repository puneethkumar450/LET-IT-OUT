package com.letitout.app.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.letitout.app.BuildConfig
import com.letitout.app.data.AuthGateway
import com.letitout.app.data.FriendlyException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val OFFLINE = "Couldn't reach the server. Check your connection and try again."
private const val GOOGLE_FAILED = "Google sign-in didn't go through. Please try again."

@Singleton
class AuthManager @Inject constructor(@ApplicationContext private val context: Context) : AuthGateway {
    override fun isConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()
    override fun isSignedIn(): Boolean = isConfigured() && FirebaseAuth.getInstance().currentUser != null
    override fun userName(): String? =
        if (isConfigured()) FirebaseAuth.getInstance().currentUser?.displayName?.takeIf { it.isNotBlank() } else null

    override suspend fun signInGuest() {
        if (!isConfigured()) throw FriendlyException("Sign-in isn't set up in this build yet.")
        try {
            FirebaseAuth.getInstance().signInAnonymously().await()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: FirebaseNetworkException) {
            throw FriendlyException(OFFLINE, e)
        } catch (e: Exception) {
            throw FriendlyException("Couldn't start a guest session just now. Please try again.", e)
        }
    }

    override suspend fun signInGoogle(activity: Activity): Boolean {
        if (!isConfigured() || BuildConfig.WEB_CLIENT_ID.isBlank()) {
            throw FriendlyException("Google sign-in isn't set up in this build yet.")
        }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            return false
        } catch (e: NoCredentialException) {
            throw FriendlyException("No Google account found on this phone. Add one in your phone's settings, or continue as a guest.", e)
        } catch (e: GetCredentialException) {
            throw FriendlyException(GOOGLE_FAILED, e)
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw FriendlyException(GOOGLE_FAILED)
        }
        try {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(google.idToken, null)
            val auth = FirebaseAuth.getInstance()
            val current = auth.currentUser
            if (current?.isAnonymous == true) {
                try {
                    // Keep the guest identity when upgrading to Google.
                    current.linkWithCredential(firebaseCredential).await()
                } catch (e: FirebaseAuthUserCollisionException) {
                    // This Google account already has its own profile, so switch to it instead. Remove the guest first
                    // (it holds nothing on our server) so it isn't left behind; deleting it after the switch could sign
                    // the Google account out again.
                    try {
                        current.delete().await()
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (ignored: Exception) {
                        // Best effort: switching accounts still works if the delete fails.
                    }
                    auth.signInWithCredential(e.updatedCredential ?: firebaseCredential).await()
                }
            } else {
                auth.signInWithCredential(firebaseCredential).await()
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: FirebaseNetworkException) {
            throw FriendlyException(OFFLINE, e)
        } catch (e: Exception) {
            throw FriendlyException(GOOGLE_FAILED, e)
        }
        return true
    }

    override suspend fun signOut() {
        if (isConfigured()) {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user?.isAnonymous == true) {
                // A guest account can't be signed back into, so remove it instead of leaving it behind.
                try {
                    user.delete().await()
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (ignored: Exception) {
                    // Best effort: signing out still works if the delete fails.
                }
            }
            auth.signOut()
        }
        clearGoogleCredential()
    }

    override suspend fun deleteAccount() {
        val user = (if (isConfigured()) FirebaseAuth.getInstance().currentUser else null)
            ?: throw FriendlyException("You're not signed in right now, so there's no account to delete.")
        try {
            user.delete().await()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw FriendlyException("For your security, please sign out, sign in again, and then delete your account.", e)
        } catch (e: FirebaseNetworkException) {
            throw FriendlyException(OFFLINE, e)
        } catch (e: Exception) {
            throw FriendlyException("Couldn't delete your account just now. Please try again.", e)
        }
        clearGoogleCredential()
    }

    private suspend fun clearGoogleCredential() {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (ignored: Exception) {
            // Nothing to clear, or Play services is unavailable; the Firebase sign-out already happened.
        }
    }
}
