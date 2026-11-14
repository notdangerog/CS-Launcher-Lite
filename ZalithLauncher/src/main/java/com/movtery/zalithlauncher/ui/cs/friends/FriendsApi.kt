/*
 * CS Launcher Lite — CS Friends client
 * Ported from JoooaoooDVD/CSL2 (app_cs/src/main/java/com/craftstudio/cslauncher/friends/FriendsApi.java)
 *
 * Thin RPC client for the CS friends backend (Supabase PostgREST, anon role, token in the body).
 */

package com.movtery.zalithlauncher.ui.cs.friends

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Thin RPC client for the CS friends backend (Supabase PostgREST, anon role, token in the body).
 */
object FriendsApi {
    /** Project origin (REST, storage and edge functions hang off it). */
    const val BASE = "https://ygteavpkursyeyfthlxm.supabase.co"
    private const val RPC = BASE + "/rest/v1/rpc/"

    /** Supabase anon role key (public, safe to ship). */
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlndGVhdnBrdXJzeWV5ZnRobHhtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI2ODk5Nks" +
        "iLCJleHAiOjIxMDgyNjU5Njl9.wqCioElyk_GlIkeaiRHlls7hk-WDABhigwb1EGy8iJU"

    /** A backend refusal in plain English (wrong password, unknown code, ...). */
    class ApiException(message: String) : IOException(message)

    /** Calls [fn] with [args]; returns the JSON result (object, or wrapped primitive). */
    @JvmStatic
    fun call(@NonNull fn: String, @NonNull args: JSONObject): JSONObject {
        val raw = post(fn, args.toString()) ?: ""
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "null") return JSONObject()
        return try {
            if (trimmed.startsWith("{")) JSONObject(trimmed)
            else JSONObject().put("value", if (trimmed.startsWith("\"")) org.json.JSONTokener(trimmed).nextValue() else trimmed)
        } catch (e: JSONException) {
            throw ApiException("Bad reply from the friends service")
        }
    }

    private fun post(fn: String, body: String): String? {
        val c = URL(RPC + fn).openConnection() as HttpURLConnection
        c.connectTimeout = 12000
        c.readTimeout = 15000
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("apikey", ANON_KEY)
        c.setRequestProperty("Authorization", "Bearer $ANON_KEY")
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("Accept", "application/json")
        val out = body.toByteArray(StandardCharsets.UTF_8)
        c.outputStream.use { it.write(out) }
        return try {
            val code = c.responseCode
            val inStream = if (code >= 400) c.errorStream else c.inputStream
            read(inStream).takeIf { code < 400 }?.also { if (code >= 400) throw ApiException(parseError(it, code)) }
        } finally {
            c.disconnect()
        }
    }

    private fun parseError(text: String, code: Int): String {
        return try {
            val err = JSONObject(text)
            err.optString("message", null)?.takeIf { it.isNotEmpty() }
                ?: err.optString("hint", null)?.takeIf { it.isNotEmpty() }
                ?: "Friends service error ($code)"
        } catch (_: JSONException) {
            "Friends service error ($code)"
        }
    }

    private fun read(@Nullable inStream: InputStream?): String {
        inStream ?: return ""
        ByteArrayOutputStream().use { bos ->
            val buf = ByteArray(8192)
            var n: Int
            while (true) {
                n = inStream.read(buf)
                if (n <= 0) break
                bos.write(buf, 0, n)
            }
            return String(bos.toByteArray(), StandardCharsets.UTF_8)
        }
    }
}
