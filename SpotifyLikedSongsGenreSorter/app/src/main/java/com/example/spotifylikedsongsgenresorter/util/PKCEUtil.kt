package com.example.spotifylikedsongsgenresorter.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

fun generateCodeVerifier(): String {
    val secureRandom = SecureRandom()
    val code = ByteArray(64)
    secureRandom.nextBytes(code)
    return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

fun generateCodeChallenge(codeVerifier: String): String {
    val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(bytes)
    val digest = messageDigest.digest()
    return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
