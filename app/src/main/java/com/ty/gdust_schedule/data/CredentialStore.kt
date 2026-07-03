package com.ty.gdust_schedule.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted credential storage using Android Keystore.
 * Stores studentId and password encrypted with AES-256-GCM.
 */
object CredentialStore {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "classapp_credentials"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val Context.credDataStore: DataStore<Preferences> by preferencesDataStore(name = "credentials")
    private val KEY_STUDENT_ID = stringPreferencesKey("enc_student_id")
    private val KEY_PASSWORD = stringPreferencesKey("enc_password")
    private val KEY_IV = stringPreferencesKey("enc_iv")

    private fun getOrCreateKey(): SecretKey {
        val ks = java.security.KeyStore.getInstance(KEYSTORE)
        ks.load(null)
        ks.getEntry(KEY_ALIAS, null)?.let { return (it as java.security.KeyStore.SecretKeyEntry).secretKey }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        kg.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    fun encrypt(plain: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val enc = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(enc) to Base64.getEncoder().encodeToString(cipher.iv)
    }

    fun decrypt(encB64: String, ivB64: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, Base64.getDecoder().decode(ivB64))
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        return String(cipher.doFinal(Base64.getDecoder().decode(encB64)), Charsets.UTF_8)
    }

    suspend fun save(context: Context, studentId: String, password: String) {
        val (encId, iv1) = encrypt(studentId)
        val (encPwd, iv2) = encrypt(password)
        context.credDataStore.edit { prefs ->
            prefs[KEY_STUDENT_ID] = "$iv1:$encId"
            prefs[KEY_PASSWORD] = "$iv2:$encPwd"
        }
    }

    fun loadStudentId(context: Context): Flow<String> = context.credDataStore.data.map { prefs ->
        val raw = prefs[KEY_STUDENT_ID] ?: return@map ""
        val parts = raw.split(":", limit = 2)
        if (parts.size == 2) try { decrypt(parts[1], parts[0]) } catch (_: Exception) { "" } else ""
    }

    fun loadPassword(context: Context): Flow<String> = context.credDataStore.data.map { prefs ->
        val raw = prefs[KEY_PASSWORD] ?: return@map ""
        val parts = raw.split(":", limit = 2)
        if (parts.size == 2) try { decrypt(parts[1], parts[0]) } catch (_: Exception) { "" } else ""
    }

    fun hasCredentials(context: Context): Flow<Boolean> = context.credDataStore.data.map { prefs ->
        prefs[KEY_STUDENT_ID]?.isNotEmpty() == true && prefs[KEY_PASSWORD]?.isNotEmpty() == true
    }

    suspend fun clear(context: Context) {
        context.credDataStore.edit { it.clear() }
    }
}
