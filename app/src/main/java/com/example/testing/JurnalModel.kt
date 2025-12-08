package com.example.testing
//ini kyk isi isi yg hrus ada di jurnalny
data class JurnalModel(
    val id: String = "",
    val emoji: String = "",
    val mood: String = "",
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val location: String = "",
    var isEdited: Boolean = false
)

