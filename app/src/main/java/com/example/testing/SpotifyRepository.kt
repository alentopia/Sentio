package com.example.spotifydemo.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpotifyRepository(
    private val clientId: String,
    private val clientSecret: String
) {
    private var token: String? = null
    private var expiryTime: Long = 0

    private val authApi = Retrofit.Builder()
        .baseUrl("https://accounts.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyAuthApi::class.java)

    private val webApi = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyWebApi::class.java)

    // Masih bisa dipakai kalau mau default playlist
    suspend fun getMyPlaylist(): PlaylistResponse = withContext(Dispatchers.IO) {
        val token = getValidToken()
        val response = webApi.getPlaylist("Bearer $token", "04vFVSHKvDSOVC16f2QDtH")
        println(" Playlist: ${response.name}, ${response.tracks.items.size} lagu")
        response
    }

    // 👉 Fungsi baru: ambil playlist by ID (buat mood-based)
    suspend fun getPlaylistById(playlistId: String): PlaylistResponse = withContext(Dispatchers.IO) {
        val token = getValidToken()
        val response = webApi.getPlaylist("Bearer $token", playlistId)
        println(" Playlist ($playlistId): ${response.name}, ${response.tracks.items.size} lagu")
        response
    }

    private suspend fun getValidToken(): String {
        val now = System.currentTimeMillis()
        if (token == null || now > expiryTime) {
            val creds = "$clientId:$clientSecret"
            val encoded = Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)
            val result = authApi.getAccessToken("Basic $encoded")
            println(" Token baru dari Spotify: ${result.access_token.take(30)}...")
            token = result.access_token
            expiryTime = now + (result.expires_in * 1000)
        }
        return token!!
    }
}
