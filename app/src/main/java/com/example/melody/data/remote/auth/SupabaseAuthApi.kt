package com.example.melody.data.remote.auth

import com.example.melody.data.remote.auth.dto.AuthApiErrorDto
import com.example.melody.data.remote.auth.dto.AuthRecoveryRequestDto
import com.example.melody.data.remote.auth.dto.AuthSessionDto
import com.example.melody.data.remote.auth.dto.AuthSignUpResponseDto
import com.example.melody.data.remote.auth.dto.AuthSignUpRequestDto
import com.example.melody.data.remote.auth.dto.AuthPasswordSignInRequestDto
import com.example.melody.data.remote.auth.dto.AuthRefreshTokenRequestDto
import com.example.melody.data.remote.auth.dto.AuthUpdateUserRequestDto
import com.example.melody.data.remote.auth.dto.AuthUserDto
import com.example.melody.data.remote.auth.dto.AuthVerifyEmailRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface SupabaseAuthApi {
    @POST("auth/v1/signup")
    suspend fun signUp(@Body request: AuthSignUpRequestDto): AuthSignUpResponseDto

    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(@Body request: AuthPasswordSignInRequestDto): AuthSessionDto

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshSession(@Body request: AuthRefreshTokenRequestDto): AuthSessionDto

    @POST("auth/v1/verify")
    suspend fun verifyEmail(@Body request: AuthVerifyEmailRequestDto): AuthSessionDto

    @POST("auth/v1/recover")
    suspend fun requestPasswordRecovery(
        @Body request: AuthRecoveryRequestDto,
        @Query("redirect_to") redirectTo: String? = null,
    ): Response<Unit>

    @PUT("auth/v1/user")
    suspend fun updateUserPassword(
        @Header("Authorization") authorization: String,
        @Body request: AuthUpdateUserRequestDto,
    ): AuthUserDto

    @GET("auth/v1/user")
    suspend fun getCurrentUser(@Header("Authorization") authorization: String): AuthUserDto

    @POST("auth/v1/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String,
        @Query("scope") scope: String = "local",
    ): Response<Unit>
}
