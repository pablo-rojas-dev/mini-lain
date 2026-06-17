package com.example.mini_lain

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mini_lain.databinding.ActivityMainBinding
import com.example.mini_lain.ui.LainFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var introduccionTerminada: Boolean = false

    companion object {
        private const val CLAVE_INTRODUCCION_TERMINADA = "introduccion_terminada"

        private const val VOLUMEN_VIDEO_INTRO = 0.85f
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
            ocultarVideoIntro()
            mostrarJuegoSiHaceFalta()
        } else {
            reproducirVideoIntro()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CLAVE_INTRODUCCION_TERMINADA, introduccionTerminada)
    }

    override fun onDestroy() {
        binding.vvVideoIntro.setOnPreparedListener(null)
        binding.vvVideoIntro.setOnCompletionListener(null)
        binding.vvVideoIntro.setOnErrorListener(null)
        binding.vvVideoIntro.stopPlayback()

        super.onDestroy()
    }

    private fun reproducirVideoIntro() {
        binding.flContenedorVideoIntro.visibility = View.VISIBLE
        binding.flContenedorVideoIntro.bringToFront()

        binding.vvVideoIntro.visibility = View.VISIBLE
        binding.vvVideoIntro.setZOrderOnTop(true)

        val uriVideo = Uri.parse(
            "android.resource://$packageName/${R.raw.lain_intro}"
        )

        binding.vvVideoIntro.setVideoURI(uriVideo)

        binding.vvVideoIntro.setOnPreparedListener { reproductor ->
            reproductor.isLooping = false
            reproductor.setVolume(VOLUMEN_VIDEO_INTRO, VOLUMEN_VIDEO_INTRO)

            binding.vvVideoIntro.establecerTamanioVideo(
                ancho = reproductor.videoWidth,
                alto = reproductor.videoHeight
            )

            binding.vvVideoIntro.post {
                binding.vvVideoIntro.start()
            }
        }

        binding.vvVideoIntro.setOnCompletionListener {
            introduccionTerminada = true
            ocultarVideoIntro()
            mostrarJuegoSiHaceFalta()
        }

        binding.vvVideoIntro.setOnErrorListener { _, _, _ ->
            introduccionTerminada = true
            ocultarVideoIntro()
            mostrarJuegoSiHaceFalta()
            true
        }
    }

    private fun ocultarVideoIntro() {
        binding.vvVideoIntro.setOnPreparedListener(null)
        binding.vvVideoIntro.setOnCompletionListener(null)
        binding.vvVideoIntro.setOnErrorListener(null)

        binding.vvVideoIntro.stopPlayback()
        binding.vvVideoIntro.setZOrderOnTop(false)
        binding.vvVideoIntro.visibility = View.GONE
        binding.flContenedorVideoIntro.visibility = View.GONE
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