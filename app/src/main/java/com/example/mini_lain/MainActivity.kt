package com.example.mini_lain

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mini_lain.databinding.ActivityMainBinding
import com.example.mini_lain.ui.LainFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var introduccionTerminada: Boolean = false
    private var reproductorAudioIntro: MediaPlayer? = null

    companion object {
        private const val CLAVE_INTRODUCCION_TERMINADA = "introduccion_terminada"

        private const val VOLUMEN_AUDIO_INTRO = 0.85f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        introduccionTerminada = savedInstanceState?.getBoolean(
            CLAVE_INTRODUCCION_TERMINADA,
            false
        ) ?: false

        if (introduccionTerminada) {
            ocultarIntro()
            mostrarJuegoSiHaceFalta()
        } else {
            reproducirIntro()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CLAVE_INTRODUCCION_TERMINADA, introduccionTerminada)
    }

    override fun onDestroy() {
        liberarAudioIntro()
        Glide.with(this).clear(binding.ivIntro)

        super.onDestroy()
    }

    private fun reproducirIntro() {
        binding.flContenedorIntro.visibility = View.VISIBLE
        binding.flContenedorIntro.bringToFront()

        binding.ivIntro.visibility = View.VISIBLE

        Glide.with(this)
            .asGif()
            .load(R.drawable.lain_intro)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.ivIntro)

        reproductorAudioIntro = MediaPlayer.create(this, R.raw.lain_intro)?.apply {
            isLooping = false
            setVolume(VOLUMEN_AUDIO_INTRO, VOLUMEN_AUDIO_INTRO)

            setOnCompletionListener {
                terminarIntro()
            }

            setOnErrorListener { reproductor, _, _ ->
                reproductor.release()
                reproductorAudioIntro = null
                terminarIntro()
                true
            }

            start()
        }

        if (reproductorAudioIntro == null) {
            terminarIntro()
        }
    }

    private fun ocultarIntro() {
        liberarAudioIntro()

        Glide.with(this).clear(binding.ivIntro)

        binding.ivIntro.visibility = View.GONE
        binding.flContenedorIntro.visibility = View.GONE
    }

    private fun terminarIntro() {
        if (introduccionTerminada) return

        introduccionTerminada = true
        ocultarIntro()
        mostrarJuegoSiHaceFalta()
    }

    private fun liberarAudioIntro() {
        reproductorAudioIntro?.setOnCompletionListener(null)
        reproductorAudioIntro?.setOnErrorListener(null)
        reproductorAudioIntro?.release()
        reproductorAudioIntro = null
    }

    private fun mostrarJuegoSiHaceFalta() {
        val fragmentoActual = supportFragmentManager.findFragmentById(
            R.id.fcvContenedorFragmento
        )

        if (fragmentoActual == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fcvContenedorFragmento, LainFragment())
                .commit()
        }
    }
}