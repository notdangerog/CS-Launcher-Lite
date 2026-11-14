/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package com.movtery.zalithlauncher.friends

import android.util.Log
import com.movtery.zalithlauncher.path.createOkHttpClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin RPC transport for the CS friends backend (Supabase PostgREST, anon role, session token
 * carried in the request body rather than in a header).
 *
 * This is the same service CS Launcher Plus talks to, at the same project, with the same function
 * names and arguments — so a friend added in one launcher shows up in the other, and a world
 * hosted from either can be joined from either. Interoperability is the point: the friends list is
 * only useful if the people on it are reachable.
 *
 * Only the **anon** key is embedded here. It is a publishable client credential by design — the
 * backend exposes nothing but the `cs_*` RPC functions to that role, and every function takes the
 * caller's session token as an argument and checks it server-side. No service-role key, admin key
 * or database password belongs in a client build, and none is referenced anywhere in this package.
 */
object FriendsApi {

    private const val TAG = "FriendsApi"

    /** Project origin — REST, storage and edge functions all hang off it. */
    const val BASE: String = "https://ygteavpkursyeyfthlxm.supabase.co"

    private const val RPC: String = "$BASE/rest/v1/rpc/"

    /** Storage bucket for chat photos; objects are readable through the public path. */
    private const val CHAT_BUCKET: String = "cs-chat"

    /**
     * Publishable anon key for this project. Safe to ship: it grants no more than an anonymous
     * visitor to the `cs_*` functions, which authenticate on the per-call token argument.
     */
    const val ANON_KEY: String =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlndGVhdnBrdXJzeWV5ZnRobHhtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI2ODk5NjksImV4cCI6MjEwODI2NTk2OX0." +
                "wqCioElyk_GlIkeaiRHlls7hk-WDABhigwb1EGy8iJU"

    /** The skin-texture host is whitelisted for authlib-injector, so remote skins load in game. */
    const val SKIN_TEXTURE_DOMAIN: String = "ygteavpkursyeyfthlxm.supabase.co"

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Reuses the launcher's OkHttp configuration, so friends traffic inherits the resilient DNS
     * fallback and the pinned TLS setup already applied to every other request the launcher makes.
     */
    private val client: OkHttpClient by lazy {
        createOkHttpClientBuilder { builder ->
            builder
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
        }.build()
    }

    /** A refusal from the backend, already translated into something safe to show the user. */
    class FriendsApiException(message: String) : IOException(message)

    /**
     * Calls RPC function [fn] with [args] and returns the JSON result.
     *
     * PostgREST returns a bare JSON scalar for functions whose return type is not composite, so a
     * non-object reply is wrapped under `"value"` — callers can then treat every result uniformly.
     *
     * @throws FriendsApiException when the backend answered with an error; the message is the
     *   server's own `message` (or `hint`), which is already written for end users.
     * @throws IOException on transport failure.
     */
    suspend fun call(fn: String, args: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val raw = post(RPC + fn, args.toString()).trim()
        if (raw.isEmpty() || raw == "null") return@withContext JSONObject()
        try {
            if (raw.startsWith("{")) JSONObject(raw)
            else JSONObject().put(
                "value",
                if (raw.startsWith("\"")) JSONTokener(raw).nextValue() else raw
            )
        } catch (e: JSONException) {
            throw FriendsApiException("Bad reply from the friends service")
        }
    }

    /** Convenience overload for functions that take no arguments beyond the token. */
    suspend fun call(fn: String): JSONObject = call(fn, JSONObject())

    /**
     * Uploads one chat attachment into the `cs-chat` bucket and returns its public URL.
     *
     * Uses `upsert` so a retried upload with the same object name replaces rather than duplicates,
     * which keeps the bucket clean when a send fails half-way on a flaky mobile connection.
     *
     * @return the public URL, suitable for passing as `p_image` to `cs_send`.
     */
    suspend fun uploadChatObject(
        name: String,
        bytes: ByteArray,
        contentType: String = "image/png"
    ): String = withContext(Dispatchers.IO) {
        val url = "$BASE/storage/v1/object/$CHAT_BUCKET/$name"
        val request = Request.Builder()
            .url(url)
            .header("apikey", ANON_KEY)
            .header("Authorization", "Bearer $ANON_KEY")
            .header("Content-Type", contentType)
            .header("x-upsert", "true")
            .header("Cache-Control", "max-age=31536000")
            .post(bytes.toRequestBody(contentType.toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw FriendsApiException("Could not upload the image (${response.code})")
            }
        }
        publicChatUrl(name)
    }

    /** The public read URL for an object already in the `cs-chat` bucket. */
    fun publicChatUrl(name: String): String = "$BASE/storage/v1/object/public/$CHAT_BUCKET/$name"

    private fun post(url: String, body: String): String {
        val request = Request.Builder()
            .url(url)
            .header("apikey", ANON_KEY)
            .header("Authorization", "Bearer $ANON_KEY")
            .header("Accept", "application/json")
            .post(body.toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            // OkHttp 4 exposes `body` as nullable and OkHttp 5 as non-null; the safe call keeps this
            // compiling against either while the project's OkHttp version settles.
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // PostgREST puts a user-facing explanation in "message", with "hint" as a fallback.
                val message = try {
                    val err = JSONObject(text)
                    err.optString("message").ifEmpty { err.optString("hint") }
                } catch (ignored: JSONException) {
                    ""
                }
                val final = message.ifEmpty { "Friends service error (${response.code})" }
                Log.w(TAG, "rpc $url -> ${response.code}: $final")
                throw FriendsApiException(final)
            }
            return text
        }
    }
}

// ---------------------------------------------------------------------------
// JSON helpers
// ---------------------------------------------------------------------------

/**
 * Reads a nullable text column.
 *
 * Postgres `NULL` and an empty string both mean "not set" here, and Android's `optString` collapses
 * a JSON null to `""`, so the two have to be handled together to avoid inventing empty addresses.
 */
internal fun JSONObject.strOrNull(name: String): String? =
    if (isNull(name)) null else optString(name, "").ifEmpty { null }

/** Reads a text column that is never null, defaulting to [fallback]. */
internal fun JSONObject.str(name: String, fallback: String = ""): String =
    if (isNull(name)) fallback else optString(name, fallback)

/**
 * Reads a timestamp that may arrive either as epoch milliseconds or as an ISO-8601 string.
 *
 * PostgREST returns `bigint` columns as JSON numbers and `timestamptz` columns as
 * `2026-11-06T08:15:30.123456+00:00`, and different `cs_*` functions use different types for the
 * same conceptual field, so both have to be accepted.
 */
internal fun JSONObject.millis(name: String): Long {
    if (isNull(name)) return 0L
    val raw = opt(name) ?: return 0L
    if (raw is Number) return raw.toLong()
    val text = raw.toString()
    text.toLongOrNull()?.let { return it }
    return parseIso8601Millis(text)
}

/** Parses the first 19 characters of a Postgres `timestamptz` as UTC. */
internal fun parseIso8601Millis(text: String): Long = try {
    val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
    format.timeZone = java.util.TimeZone.getTimeZone("UTC")
    format.parse(if (text.length >= 19) text.substring(0, 19) else text)?.time ?: 0L
} catch (ignored: Exception) {
    0L
}

/** Converts a JSON array into a list, skipping any non-object entries. */
internal inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        out += transform(o)
    }
    return out
}
