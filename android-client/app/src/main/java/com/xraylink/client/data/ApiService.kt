package com.xraylink.client.data

import com.google.gson.annotations.SerializedName
import com.xraylink.client.vpn.TunnelProvisioningConfig
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class PinLoginRequest(
    val pin: String,
    val deviceFingerprint: String,
    val deviceName: String,
    val platform: String = "android",
    val appVersion: String = "1.0.0"
)

data class PinUserDto(
    val id: String,
    val username: String,
    val displayName: String,
    val pinCode: String,
    val expiresAt: String,
    val deviceLimit: Int,
    val status: String,
    val configVersion: Int
)

data class PinLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val user: PinUserDto
)

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

data class ServerDto(
    val id: String,
    val name: String,
    @SerializedName("country_code") val countryCode: String,
    val location: String,
    val endpoint: String,
    val port: Int,
    val protocol: String,
    @SerializedName("latency_ms") val latencyMs: Int,
    val isEnabled: Boolean
)

data class ServersResponse(
    val servers: List<ServerDto>,
    val configVersion: Int
)

data class ProfileDto(
    val id: String,
    val name: String,
    val protocol: String,
    @SerializedName("authorized_hostname") val authorizedHostname: String,
    @SerializedName("sni_host") val sniHost: String?,
    @SerializedName("ws_path") val wsPath: String?,
    val description: String?,
    val isEnabled: Boolean
)

data class ProfilesResponse(
    val profiles: List<ProfileDto>,
    val configVersion: Int
)

data class SessionRecordDto(
    val id: String,
    @SerializedName("server_name") val serverName: String,
    @SerializedName("profile_name") val profileName: String,
    val status: String,
    @SerializedName("client_tunnel_ip") val clientTunnelIp: String,
    @SerializedName("bytes_up") val bytesUp: Long,
    @SerializedName("bytes_down") val bytesDown: Long,
    @SerializedName("connected_at") val connectedAt: String
)

data class SessionsResponse(
    val sessions: List<SessionRecordDto>
)

data class AnnouncementDto(
    val id: String,
    val title: String,
    val body: String,
    val severity: String,
    @SerializedName("created_at") val createdAt: String
)

data class NotificationsResponse(
    val announcements: List<AnnouncementDto>
)

data class SessionInitiateResponse(
    val sessionId: String,
    val status: String,
    val tunnelProvisioning: TunnelProvisioningConfig
)

data class HeartbeatResponse(
    val action: String,
    val configVersion: Int? = null
)

data class MeUserDto(
    val id: String,
    val username: String,
    @SerializedName("display_name") val displayName: String? = null,
    val status: String? = null,
    @SerializedName("device_limit") val deviceLimit: Int = 1,
    @SerializedName("expires_at") val expiresAt: String = ""
)

data class MePinDto(
    val id: String,
    @SerializedName("pin_code") val pinCode: String,
    val status: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null
)

data class MeResponse(
    val user: MeUserDto? = null,
    val pin: MePinDto? = null,
    val configVersion: Int = 0
)

interface XRayApiService {
    @POST("auth/pin-login")
    suspend fun pinLogin(@Body body: PinLoginRequest): PinLoginResponse

    @POST("auth/refresh")
    suspend fun refreshTokens(@Body body: Map<String, String>): RefreshResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: Map<String, String>): Map<String, Any>

    @GET("auth/me")
    suspend fun me(): MeResponse

    @GET("servers")
    suspend fun listServers(): ServersResponse

    @GET("profiles")
    suspend fun listProfiles(): ProfilesResponse

    @GET("sessions")
    suspend fun listSessions(): SessionsResponse

    @GET("notifications")
    suspend fun listNotifications(): NotificationsResponse

    @POST("sessions/connect")
    suspend fun initiateSession(@Body body: Map<String, Any>): SessionInitiateResponse

    @POST("sessions/{id}/confirm")
    suspend fun confirmSession(@Path("id") id: String, @Body body: Map<String, Boolean>): Map<String, Any>

    @POST("sessions/{id}/heartbeat")
    suspend fun sendHeartbeat(@Path("id") id: String, @Body body: Map<String, Long>): HeartbeatResponse

    @POST("sessions/{id}/disconnect")
    suspend fun disconnectSession(@Path("id") id: String, @Body body: Map<String, String>): Map<String, Any>
}
