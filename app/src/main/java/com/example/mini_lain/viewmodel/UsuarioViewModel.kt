package com.example.mini_lain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.mini_lain.dao.AppDatabase
import com.example.mini_lain.model.Usuario
import com.example.mini_lain.repositories.UsuarioRepository

class LainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: UsuarioRepository

    init {
        val dao = AppDatabase
            .obtenerBaseDatos(application)
            .usuarioDao()

        repository = UsuarioRepository(dao)
    }

    suspend fun obtenerUsuario(nombre: String): Usuario? {
        return repository.obtenerUsuario(nombre)
    }

    suspend fun registrarUsuario(nombre: String): Usuario {
        return repository.registrarUsuarioSiNoExiste(nombre)
    }

    suspend fun guardarSecreto(
        nombre: String,
        secreto: String,
        contrasena: String
    ): Usuario? {
        return repository.guardarSecreto(
            nombre = nombre,
            secreto = secreto,
            contrasena = contrasena
        )
    }

    suspend fun validarContrasena(
        nombre: String,
        contrasena: String
    ): Usuario? {
        return repository.validarContrasena(
            nombre = nombre,
            contrasena = contrasena
        )
    }
}