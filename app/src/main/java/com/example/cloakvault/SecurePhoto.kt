package com.example.cloakvault

data class SecurePhoto(
    val downloadUrl: String = "",
    val iv: String = "",           // Initialization Vector (Needed for decryption)
    val filename: String = "",
    val timestamp: Long = 0
)