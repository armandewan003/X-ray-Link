package com.xraylink.admin.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AdminApiService {
    @POST("auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Map<String, Any>

    @POST("auth/owner-login")
    suspend fun ownerLogin(@Body credentials: Map<String, String>): Map<String, Any>

    @POST("auth/logout")
    suspend fun logout(@Body body: Map<String, String>): Map<String, Any>

    @GET("dashboard/overview")
    suspend fun getOverview(): Map<String, Any>

    @GET("config")
    suspend fun getConfig(): Map<String, Any>

    @GET("users")
    suspend fun listUsers(): Map<String, Any>

    @POST("users")
    suspend fun createUser(@Body body: Map<String, Any>): Map<String, Any>

    @PATCH("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: Map<String, Any>): Map<String, Any>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Map<String, Any>

    @POST("pins/generate")
    suspend fun generatePins(@Body body: Map<String, Any>): Map<String, Any>

    @GET("resellers")
    suspend fun listResellers(): Map<String, Any>

    @POST("credits/adjust")
    suspend fun adjustCredits(@Body body: Map<String, Any>): Map<String, Any>

    @GET("servers")
    suspend fun listServers(): Map<String, Any>

    @PATCH("servers/{id}")
    suspend fun updateServer(@Path("id") id: String, @Body body: Map<String, Any>): Map<String, Any>

    @GET("sessions")
    suspend fun listSessions(): Map<String, Any>

    @POST("sessions/{id}/revoke")
    suspend fun revokeSession(@Path("id") id: String): Map<String, Any>

    @PATCH("config/{key}")
    suspend fun updateSetting(@Path("key") key: String, @Body body: Map<String, Any>): Map<String, Any>

    @GET("audit-logs")
    suspend fun getAuditLogs(): Map<String, Any>
}
