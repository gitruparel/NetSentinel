package com.netsentinel.app.database

import android.content.Context
import android.util.Log
import com.netsentinel.app.data.model.AuditSession
import com.netsentinel.app.data.model.Incident
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages Supabase PostgreSQL operations via PostgREST API.
 * Synchronizes incidents and audit sessions directly to your cloud PostgreSQL database.
 */
class SupabaseManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(): String = SupabaseConfig.SUPABASE_URL.trimEnd('/')
    private fun getAnonKey(): String = SupabaseConfig.SUPABASE_ANON_KEY

    suspend fun saveIncident(incident: Incident): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            Log.d("SupabaseManager", "ℹ️ Supabase credentials not set yet. Local Room SQLite active. Paste credentials in SupabaseConfig.kt.")
            return@withContext false
        }

        try {
            val json = JSONObject().apply {
                put("id", incident.id)
                put("audit_session_id", incident.auditSessionId.ifEmpty { JSONObject.NULL })
                put("title", incident.title)
                put("timestamp_formatted", incident.timestampFormatted)
                put("threat_score", incident.threatScore)
                put("severity", incident.severity.name)
                put("status", incident.status)
                put("ai_analysis", incident.aiAnalysis)
                put("ssid", incident.networkDetails.ssid)
                put("bssid", incident.networkDetails.bssid)
                put("gateway_ip", incident.networkDetails.gatewayIp)
                put("security_type", incident.networkDetails.securityType)
                put("gps_coordinates", incident.gpsCoordinatesFormatted)
                put("photo_uri", incident.evidencePhotoPlaceholder)
                put("synced_at_ms", System.currentTimeMillis())
            }

            val requestBody = json.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("${getBaseUrl()}/rest/v1/incidents")
                .header("apikey", getAnonKey())
                .header("Authorization", "Bearer ${getAnonKey()}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("SupabaseManager", "✅ Incident synced to Supabase PostgreSQL: ${incident.id} (${incident.title})")
                    true
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.w("SupabaseManager", "⚠️ Supabase incident sync response code ${response.code}: $errorBody")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error syncing incident to Supabase: ${e.message}", e)
            false
        }
    }

    suspend fun saveAuditSession(session: AuditSession): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext false
        }

        try {
            val json = JSONObject().apply {
                put("id", session.id)
                put("start_time_ms", session.startTimeMs)
                put("end_time_ms", session.endTimeMs)
                put("trust_score", session.trustScore)
                put("ssid", session.initialSnapshot.ssid)
                put("bssid", session.initialSnapshot.bssid)
                put("status", session.status)
                put("synced_at_ms", System.currentTimeMillis())
            }

            val requestBody = json.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("${getBaseUrl()}/rest/v1/audit_sessions")
                .header("apikey", getAnonKey())
                .header("Authorization", "Bearer ${getAnonKey()}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("SupabaseManager", "✅ Audit session synced to Supabase PostgreSQL: ${session.id} (Score: ${session.trustScore})")
                    true
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.w("SupabaseManager", "⚠️ Supabase audit sync response code ${response.code}: $errorBody")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error syncing audit session to Supabase: ${e.message}", e)
            false
        }
    }

    // =========================================================================
    // POSTGREST SQL QUERY OPERATIONS (DEMONSTRATION METHODS)
    // =========================================================================

    /**
     * Query incidents by severity level (Equivalent SQL: SELECT * FROM incidents WHERE severity = :severity)
     */
    suspend fun queryIncidentsBySeverity(severity: String): List<JSONObject> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("${getBaseUrl()}/rest/v1/incidents?severity=eq.$severity&order=threat_score.desc")
                .header("apikey", getAnonKey())
                .header("Authorization", "Bearer ${getAnonKey()}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body?.string() ?: "[]")
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(jsonArray.getJSONObject(i))
                    }
                    list
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Query incidents by severity failed: ${e.message}")
            emptyList()
        }
    }
}
