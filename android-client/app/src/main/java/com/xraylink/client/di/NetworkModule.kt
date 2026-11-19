package com.xraylink.client.di

import com.xraylink.client.BuildConfig
import com.xraylink.client.data.EncryptedTokenStore
import com.xraylink.client.data.XRayApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private class TokenRefreshAuthenticator(
        private val tokenStore: EncryptedTokenStore
    ) : Authenticator {
        private val refreshClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        override fun authenticate(route: Route?, response: Response): Request? {
            if (responseCount(response) >= 2) return null
            val path = response.request.url.encodedPath
            if (path.endsWith("/auth/refresh") || path.endsWith("/auth/pin-login")) return null

            val refreshToken = tokenStore.getRefreshToken() ?: return null
            synchronized(this) {
                val latestAccess = tokenStore.getAccessToken()
                val reqHeader = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
                if (!latestAccess.isNullOrBlank() && latestAccess != reqHeader) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $latestAccess")
                        .build()
                }

                val bodyJson = JSONObject().put("refreshToken", refreshToken).toString()
                val refreshReq = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}auth/refresh")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .build()

                return try {
                    refreshClient.newCall(refreshReq).execute().use { res ->
                        if (!res.isSuccessful) {
                            tokenStore.clear()
                            return null
                        }
                        val payload = JSONObject(res.body?.string().orEmpty())
                        val newAccess = payload.optString("accessToken", "")
                        val newRefresh = payload.optString("refreshToken", "")
                        if (newAccess.isBlank() || newRefresh.isBlank()) {
                            tokenStore.clear()
                            return null
                        }
                        tokenStore.saveTokens(newAccess, newRefresh, tokenStore.getPinCode().orEmpty())
                        response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccess")
                            .build()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        private fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenStore: EncryptedTokenStore): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = tokenStore.getAccessToken()
            val req = if (!token.isNullOrBlank() && original.header("Authorization") == null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(req)
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(TokenRefreshAuthenticator(tokenStore))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): XRayApiService =
        retrofit.create(XRayApiService::class.java)
}
