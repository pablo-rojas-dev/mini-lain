package com.example.mini_lain.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mini_lain.model.Usuario

@Dao
interface UsuarioDao {

    @Query(
        """
        SELECT * FROM usuarios
        WHERE nombreNormalizado = :nombreNormalizado
        LIMIT 1
        """
    )
    suspend fun buscarPorNombreNormalizado(nombreNormalizado: String): Usuario?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(usuario: Usuario): Long

    @Query(
        """
        UPDATE usuarios
        SET secreto = :secreto,
            contrasena = :contrasena
        WHERE nombreNormalizado = :nombreNormalizado
        """
    )
    suspend fun actualizarSecreto(
        nombreNormalizado: String,
        secreto: String,
        contrasena: String
    )
}