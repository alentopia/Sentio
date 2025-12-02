package com.example.testing

data class UserProfile(
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val profileImage: String = "",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val updatedAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)