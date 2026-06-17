package com.example.mini_lain.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mini_lain.model.Usuario

@Database(
    entities = [Usuario::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtenerBaseDatos(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                val nuevaInstancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mini_lain.db"
                ).build()

                instancia = nuevaInstancia
                nuevaInstancia
            }
        }
    }
}