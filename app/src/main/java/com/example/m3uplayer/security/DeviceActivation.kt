package com.example.m3uplayer.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.math.BigInteger
import org.json.JSONObject\nimport okhttp3.MediaType.Companion.toMediaType\nimport okhttp3.RequestBody.Companion.toRequestBody

object DeviceActivation {

    private const val ALIAS = "zorrillo_device_v1"

    private const val API =
        "https://zorrillo-tv-api.bryanjok0909.workers.dev"

    @Synchronized
    fun ensureIdentity() {
        check(Build.VERSION.SDK_INT >= 23) {
            "Dispositivo no compatible con activacion segura"
        }

        val store = KeyStore.getInstance("AndroidKeyStore")
        store.load(null)

        if (!store.containsAlias(ALIAS)) {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )

            generator.initialize(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_SIGN
                )
                    .setAlgorithmParameterSpec(
                        ECGenParameterSpec("secp256r1")
                    )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .build()
            )

            generator.generateKeyPair()
        }
    }

    private fun publicKey(): ECPublicKey {
        ensureIdentity()
        val store = KeyStore.getInstance("AndroidKeyStore")
        store.load(null)
        return store.getCertificate(ALIAS).publicKey as ECPublicKey
    }

    private fun base64(data: ByteArray): String =
        Base64.encodeToString(
            data,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

    private fun coordinate(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        require(value.signum() >= 0 && value.bitLength() <= 256)
        val result = ByteArray(32)
        val size = minOf(raw.size, 32)
        System.arraycopy(
            raw, raw.size - size,
            result, 32 - size, size
        )
        return result
    }

    fun publicJwk(): String {
        val key = publicKey()

        return JSONObject()
            .put("kty", "EC")
            .put("crv", "P-256")
            .put("x", base64(coordinate(key.w.affineX)))
            .put("y", base64(coordinate(key.w.affineY)))
            .toString()
    }

    fun deviceId(): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(publicKey().encoded)

        return "TV-" + base64(hash)
    }

    fun sign(message: String): String {
        ensureIdentity()

        val store = KeyStore.getInstance("AndroidKeyStore")
        store.load(null)

        val privateKey = store.getKey(ALIAS, null)
            as java.security.PrivateKey

        val signer = java.security.Signature
            .getInstance("SHA256withECDSA")

        signer.initSign(privateKey)
        signer.update(message.toByteArray(Charsets.UTF_8))

        return base64(derToRaw(signer.sign()))
    }

    private fun derToRaw(der: ByteArray): ByteArray {
        var pos = 0

        fun next(): Int {
            require(pos < der.size)
            return der[pos++].toInt() and 0xff
        }

        require(next() == 0x30)
        require(next() == der.size - 2)

        fun readInteger(): ByteArray {
            require(next() == 0x02)

            val length = next()
            require(length in 1..33)
            require(pos + length <= der.size)

            val bytes = der.copyOfRange(pos, pos + length)
            pos += length

            return coordinate(BigInteger(bytes))
        }

        val r = readInteger()
        val s = readInteger()

        require(pos == der.size)

        return r + s
    }

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private fun post(
        path: String,
        payload: JSONObject
    ): JSONObject {
        val mediaType = okhttp3.MediaType
            .parse("application/json")!!

        val body = okhttp3.RequestBody.create(
            mediaType,
            payload.toString()
        )

        val request = okhttp3.Request.Builder()
            .url(API + path)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val content = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw ApiException(response.code, content)
            }

            return JSONObject(content)
        }
    }

    fun requestChallenge(): String {
        val result = post(
            "/device/challenge",
            JSONObject()
        )

        return result.getString("challenge")
            .also {
                require(it.matches(Regex("[a-fA-F0-9]{64}")))
            }
    }

    fun registerDevice(): JSONObject {
        val id = deviceId()
        val key = publicJwk()
        val challenge = requestChallenge()

        val message =
            "zorrillo-register-v1\n$id\n$challenge\n$key"

        val signature = sign(message)

        return post(
            "/device/register",
            JSONObject()
                .put("deviceId", id)
                .put("publicKey", key)
                .put("challenge", challenge)
                .put("signature", signature)
        )
    }

    fun deviceStatus(): JSONObject {
        val id = deviceId()
        val challenge = requestChallenge()

        val message =
            "zorrillo-status-v1\n$id\n$challenge"

        return post(
            "/device/status",
            JSONObject()
                .put("deviceId", id)
                .put("challenge", challenge)
                .put("signature", sign(message))
        )
    }

    private class ApiException(
        val code: Int,
        val content: String
    ) : java.io.IOException("HTTP " + code)

    @Synchronized
    fun checkActivation(): String {
        return try {
            ensureIdentity()

            val result = try {
                deviceStatus()
            } catch (e: ApiException) {
                val error = JSONObject(e.content)
                    .optString("error")

                if (
                    e.code == 401 &&
                    error == "Dispositivo no autorizado"
                ) {
                    registerDevice()
                    deviceStatus()
                } else {
                    throw e
                }
            }

            require(result.getString("deviceId") == deviceId())

            when (result.getString("status")) {
                "active" -> "active"
                "pending" -> "pending"
                "blocked" -> "blocked"
                "expired" -> "expired"
                else -> "error"
            }
        } catch (e: Exception) {
            "error"
        }
    }
}
