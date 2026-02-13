package com.example.cloakvault

// Firestore requires an empty constructor, which default values provide automatically
data class SecretNote(
    val title: String = "",
    val encryptedData: String = "", // Holds IV + CipherText
    val timestamp: Long = 0L
)