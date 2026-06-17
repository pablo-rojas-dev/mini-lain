package com.example.mini_lain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [
        Index(value = ["nombreNormalizado"], unique = true)
    ]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val nombreNormalizado: String,
    val secreto: String? = null,
    val contrasena: String? = null
)