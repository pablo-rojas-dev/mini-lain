package com.example.mini_lain.repositories

import com.example.mini_lain.model.Usuario
import com.example.mini_lain.dao.UsuarioDao

class UsuarioRepository(
    private val usuarioDao: UsuarioDao
) {

    suspend fun obtenerUsuario(nombre: String): Usuario? {
        return usuarioDao.buscarPorNombreNormalizado(
            normalizarNombre(nombre)
        )
    }

    suspend fun registrarUsuarioSiNoExiste(nombre: String): Usuario {
        val nombreNormalizado = normalizarNombre(nombre)

        val usuarioExistente = usuarioDao.buscarPorNombreNormalizado(nombreNormalizado)

        if (usuarioExistente != null) {
            return usuarioExistente
        }

        val nuevoUsuario = Usuario(
            nombre = nombre.trim(),
            nombreNormalizado = nombreNormalizado
        )

        usuarioDao.insertar(nuevoUsuario)

        return usuarioDao.buscarPorNombreNormalizado(nombreNormalizado)
            ?: nuevoUsuario
    }

    suspend fun guardarSecreto(
        nombre: String,
        secreto: String,
        contrasena: String
    ): Usuario? {
        val nombreNormalizado = normalizarNombre(nombre)

        usuarioDao.actualizarSecreto(
            nombreNormalizado = nombreNormalizado,
            secreto = secreto,
            contrasena = contrasena
        )

        return usuarioDao.buscarPorNombreNormalizado(nombreNormalizado)
    }

    suspend fun validarContrasena(
        nombre: String,
        contrasena: String
    ): Usuario? {
        val usuario = obtenerUsuario(nombre)

        if (usuario?.contrasena == contrasena && !usuario.secreto.isNullOrBlank()) {
            return usuario
        }

        return null
    }

    private fun normalizarNombre(nombre: String): String {
        return nombre.trim().lowercase()
    }
}