package com.example.mini_lain.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mini_lain.R
import com.example.mini_lain.databinding.FragmentLainBinding
import com.example.mini_lain.model.Usuario
import com.example.mini_lain.viewmodel.LainViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class LainFragment : Fragment() {

    // Binding

    private var _binding: FragmentLainBinding? = null
    private val binding get() = _binding!!

    // ViewModel

    private val viewModel: LainViewModel by viewModels()

    // Estado del juego

    private var nombreJugador: String = ""
    private var numeroSecreto: Int = 0
    private var escenaActual: Int = R.drawable.lain_base
    private var estadoActual: EstadoFlujo = EstadoFlujo.INICIO
    private var textoGrandeActual: String = ""

    private var usuarioActual: Usuario? = null
    private var usuarioEsNuevo: Boolean = false
    private var secretoPendiente: String = ""

    // Corrutinas y tiempos

    private var trabajoSecuencia: Job? = null
    private val retrasoAutomaticoMs = 2000L

    // Audio

    private var reproductorMusicaFondo: MediaPlayer? = null

    private var reproductorEfectos: SoundPool? = null
    private var idSonidoRisa: Int = 0
    private var risaCargada: Boolean = false
    private var risaReproducida: Boolean = false

    // Modelos de Lain

    private data class Opcion(
        val texto: String,
        val accion: () -> Unit
    )

    private enum class EstadoFlujo {
        INICIO,
        PREGUNTAR_NOMBRE,
        PRESENTACION,
        PREGUNTAR_IDENTIDAD,
        PREGUNTA_FINAL,
        MENU_USUARIO_EXISTENTE,
        PREGUNTAR_NUMERO,
        NOTIFICACION_INCORRECTA,
        PREGUNTAR_GUARDAR_SECRETO,
        PREGUNTAR_SECRETO,
        PREGUNTAR_CONTRASENA_NUEVO_SECRETO,
        PREGUNTAR_CONTRASENA_LEER_SECRETO,
        MOSTRAR_SECRETO,
        OPCIONES_VICTORIA,
        PANTALLA_PERDER,
        TEXTO_GRANDE
    }

    // Constantes

    companion object {
        private const val CLAVE_NUMERO_SECRETO = "numero_secreto"
        private const val CLAVE_ESCENA_ACTUAL = "escena_actual"
        private const val CLAVE_ESTADO_ACTUAL = "estado_actual"
        private const val CLAVE_TEXTO_GRANDE = "texto_grande"
        private const val CLAVE_RISA = "risa"
        private const val CLAVE_USUARIO_ES_NUEVO = "usuario_es_nuevo"
        private const val CLAVE_SECRETO_PENDIENTE = "secreto_pendiente"

        private const val VOLUMEN_MUSICA_FONDO = 0.35f
        private const val VOLUMEN_RISA = 1.0f
    }

    // Ciclo de vida del Fragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configurarAccionTecladoEntrada()
        inicializarEfectosSonido()
        iniciarMusicaFondo()

        if (savedInstanceState == null) {
            iniciarFlujo()
        } else {
            restaurarFlujo(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(CLAVE_NUMERO_SECRETO, numeroSecreto)
        outState.putInt(CLAVE_ESCENA_ACTUAL, escenaActual)
        outState.putString(CLAVE_ESTADO_ACTUAL, estadoActual.name)
        outState.putString(CLAVE_TEXTO_GRANDE, textoGrandeActual)
        outState.putBoolean(CLAVE_RISA, risaReproducida)
        outState.putBoolean(CLAVE_USUARIO_ES_NUEVO, usuarioEsNuevo)
        outState.putString(CLAVE_SECRETO_PENDIENTE, secretoPendiente)
    }

    override fun onResume() {
        super.onResume()
        reanudarMusicaFondo()
    }

    override fun onPause() {
        pausarMusicaFondo()
        super.onPause()
    }

    override fun onDestroyView() {
        trabajoSecuencia?.cancel()
        liberarReproductoresAudio()
        Glide.with(this).clear(binding.ivEscena)
        _binding = null
        super.onDestroyView()
    }

    // Restauración del flujo

    private fun restaurarFlujo(savedInstanceState: Bundle) {
        numeroSecreto = savedInstanceState.getInt(CLAVE_NUMERO_SECRETO, 0)
        escenaActual = savedInstanceState.getInt(CLAVE_ESCENA_ACTUAL, R.drawable.lain_base)

        estadoActual = EstadoFlujo.valueOf(
            savedInstanceState.getString(CLAVE_ESTADO_ACTUAL, EstadoFlujo.INICIO.name)!!
        )

        textoGrandeActual = savedInstanceState.getString(CLAVE_TEXTO_GRANDE, "")
        risaReproducida = savedInstanceState.getBoolean(CLAVE_RISA, false)
        usuarioEsNuevo = savedInstanceState.getBoolean(CLAVE_USUARIO_ES_NUEVO, false)
        secretoPendiente = savedInstanceState.getString(CLAVE_SECRETO_PENDIENTE, "")

        mostrarEscena(escenaActual)

        when (estadoActual) {
            EstadoFlujo.INICIO -> iniciarFlujo()
            EstadoFlujo.PREGUNTAR_NOMBRE -> preguntarNombre()
            EstadoFlujo.PRESENTACION -> mostrarPresentacion()
            EstadoFlujo.PREGUNTAR_IDENTIDAD -> preguntarIdentidad()
            EstadoFlujo.PREGUNTA_FINAL -> preguntarPreguntaFinal()
            EstadoFlujo.MENU_USUARIO_EXISTENTE -> mostrarMenuUsuarioExistente()
            EstadoFlujo.PREGUNTAR_NUMERO -> preguntarNumero()
            EstadoFlujo.NOTIFICACION_INCORRECTA -> mostrarNotificacionIncorrectaYEscape()
            EstadoFlujo.PREGUNTAR_GUARDAR_SECRETO -> preguntarGuardarSecreto()
            EstadoFlujo.PREGUNTAR_SECRETO -> preguntarSecreto()
            EstadoFlujo.PREGUNTAR_CONTRASENA_NUEVO_SECRETO -> preguntarContrasenaNuevoSecreto()
            EstadoFlujo.PREGUNTAR_CONTRASENA_LEER_SECRETO -> preguntarContrasenaParaLeerSecreto()
            EstadoFlujo.MOSTRAR_SECRETO -> mostrarSecreto(textoGrandeActual)
            EstadoFlujo.OPCIONES_VICTORIA -> mostrarOpcionesVictoria()
            EstadoFlujo.PANTALLA_PERDER -> mostrarPantallaGameOver()
            EstadoFlujo.TEXTO_GRANDE -> mostrarTextoGrande(textoGrandeActual)
        }
    }

    // Flujo principal

    private fun iniciarFlujo() {
        trabajoSecuencia?.cancel()

        ocultarPanel()
        ocultarTextoGrande()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            mostrarEscena(R.drawable.lain_entrada)
            delay(2200)

            mostrarEscena(R.drawable.lain_base)
            delay(600)

            mostrarLinea(
                texto = "Hola..."
            ) {
                preguntarNombre()
            }
        }
    }

    private fun preguntarNombre() {
        estadoActual = EstadoFlujo.PREGUNTAR_NOMBRE

        mostrarEntrada(
            texto = "¿Cuál es tu nombre?",
            pista = "Nombre",
            tipoEntrada = InputType.TYPE_CLASS_TEXT,
            textoBoton = "Responder"
        ) { valor ->
            val nombreLimpio = valor.trim().lowercase()
                .replaceFirstChar { it.uppercase() }

            if (nombreLimpio.isBlank()) {
                mostrarLinea(
                    texto = "Escribe un nombre"
                ) {
                    preguntarNombre()
                }
                return@mostrarEntrada
            }

            nombreJugador = nombreLimpio
            verificarUsuario()
        }
    }

    private fun verificarUsuario() {
        val nombreMinusculas = nombreJugador.trim().lowercase()
        val esLain = nombreMinusculas == "lain"

        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = if (esLain) {
                null
            } else {
                viewModel.obtenerUsuario(nombreJugador)
            }

            usuarioActual = usuario

            if (esLain || usuario != null) {
                usuarioEsNuevo = false

                mostrarLinea(
                    texto = "Oh, hola $nombreJugador, parece que volviste..."
                ) {
                    mostrarMenuUsuarioExistente()
                }
            } else {
                usuarioEsNuevo = true
                usuarioActual = viewModel.registrarUsuario(nombreJugador)

                mostrarLinea(
                    texto = "Hola $nombreJugador"
                ) {
                    mostrarPresentacion()
                }
            }
        }
    }

    private fun mostrarMenuUsuarioExistente() {
        estadoActual = EstadoFlujo.MENU_USUARIO_EXISTENTE

        mostrarOpciones(
            texto = "¿Qué quieres hacer?",
            opciones = listOf(
                Opcion("Jugar") {
                    usuarioEsNuevo = false
                    iniciarRetoNumero()
                },
                Opcion("Leer secreto") {
                    iniciarLoginSecreto()
                }
            )
        )
    }

    private fun mostrarPresentacion() {
        estadoActual = EstadoFlujo.PRESENTACION

        mostrarLinea(
            texto = "Mi nombre es Lain"
        ) {
            preguntarIdentidad()
        }
    }

    private fun preguntarIdentidad() {
        estadoActual = EstadoFlujo.PREGUNTAR_IDENTIDAD

        mostrarOpciones(
            texto = "¿Sabes quién eres tu?",
            opciones = listOf(
                Opcion(nombreJugador) {
                    mostrarLinea(
                        texto = "No, tú eres Lain yo soy $nombreJugador"
                    ) {
                        iniciarSecuenciaReaparicion()
                    }
                },
                Opcion("Lain") {
                    mostrarLinea(
                        texto = "Así es"
                    ) {
                        iniciarSecuenciaReaparicion()
                    }
                }
            )
        )
    }

    private fun iniciarSecuenciaReaparicion() {
        trabajoSecuencia?.cancel()

        ocultarPanel()
        ocultarTextoGrande()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            mostrarEscena(R.drawable.lain_salida)
            delay(1800)

            mostrarEscena(R.drawable.lain_entrada)
            delay(1800)

            mostrarEscena(R.drawable.lain_base)
            delay(700)

            mostrarLinea(
                texto = "Hola Lain, soy $nombreJugador"
            ) {
                preguntarPreguntaFinal()
            }
        }
    }

    private fun preguntarPreguntaFinal() {
        estadoActual = EstadoFlujo.PREGUNTA_FINAL

        mostrarOpciones(
            texto = "¿Quieres volver a ser $nombreJugador?",
            opciones = listOf(
                Opcion("Sí") {
                    iniciarRetoNumero()
                },
                Opcion("No") {
                    mostrarLinea(
                        texto = "Tampoco me gustaría ser $nombreJugador..."
                    ) {
                        mostrarNotificacionIncorrectaYEscape()
                    }
                }
            )
        )
    }

    // Reto del número secreto

    private fun iniciarRetoNumero() {
        risaReproducida = false
        numeroSecreto = Random.nextInt(1, 6)
        preguntarNumero()
    }

    private fun preguntarNumero() {
        estadoActual = EstadoFlujo.PREGUNTAR_NUMERO

        mostrarOpciones(
            texto = "¿Adivina qué número estoy pensando?",
            opciones = (1..5).map { numero ->
                Opcion(numero.toString()) {
                    if (numero == numeroSecreto) {
                        mostrarLinea(
                            texto = "Felicidades, supongo que me quedaré aquí..."
                        ) {
                            if (usuarioEsNuevo) {
                                preguntarGuardarSecreto()
                            } else {
                                mostrarFinalDesaparicion()
                            }
                        }
                    } else {
                        mostrarNotificacionIncorrectaYEscape()
                    }
                }
            }
        )
    }

    private fun reintentarRetoNumero() {
        trabajoSecuencia?.cancel()

        ocultarPanel()
        ocultarTextoGrande()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            mostrarEscena(R.drawable.estatica_salida)
            delay(1600)

            mostrarEscena(R.drawable.lain_entrada)
            delay(1800)

            mostrarEscena(R.drawable.lain_base)
            delay(700)

            iniciarRetoNumero()
        }
    }

    // Flujo de victoria

    private fun preguntarGuardarSecreto() {
        estadoActual = EstadoFlujo.PREGUNTAR_GUARDAR_SECRETO

        mostrarEscena(R.drawable.lain_base)

        mostrarOpciones(
            texto = "¿Quieres guardar algún secreto?",
            opciones = listOf(
                Opcion("Sí") {
                    preguntarSecreto()
                },
                Opcion("No") {
                    mostrarOpcionesVictoria()
                }
            )
        )
    }

    private fun mostrarFinalDesaparicion() {
        trabajoSecuencia?.cancel()

        ocultarPanel()
        ocultarTextoGrande()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            mostrarEscena(R.drawable.lain_base)
            delay(600)

            mostrarOpcionesVictoria()
        }
    }

    private fun mostrarOpcionesVictoria() {
        estadoActual = EstadoFlujo.OPCIONES_VICTORIA

        ocultarTextoGrande()
        mostrarEscena(R.drawable.lain_base)

        mostrarBotonesInferiores(
            opciones = listOf(
                Opcion("Salir") {
                    requireActivity().finishAffinity()
                },
                Opcion("Otra vez") {
                    reintentarRetoNumero()
                }
            )
        )
    }

    // Flujo de derrota

    private fun mostrarNotificacionIncorrectaYEscape() {
        estadoActual = EstadoFlujo.NOTIFICACION_INCORRECTA

        reproducirRisa()

        mostrarLinea(
            texto = "Jajaja, parece que te quedarás un buen rato aquí Lain..."
        ) {
            mostrarFinalEscape()
        }
    }

    private fun mostrarFinalEscape() {
        trabajoSecuencia?.cancel()

        ocultarPanel()
        ocultarTextoGrande()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            mostrarEscena(R.drawable.lain_salida)
            delay(1600)

            mostrarEscena(R.drawable.estatica_entrada)
            delay(1600)

            mostrarEscena(R.drawable.estatica_base)
            delay(700)

            mostrarPantallaGameOver()
        }
    }

    private fun mostrarPantallaGameOver() {
        estadoActual = EstadoFlujo.PANTALLA_PERDER
        textoGrandeActual = "Perdiste"

        binding.tvTextoGrande.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
        binding.tvTextoGrande.isVisible = true
        binding.tvTextoGrande.text = "Perdiste"

        mostrarBotonesInferiores(
            opciones = listOf(
                Opcion("Nuevo intento") {
                    reintentarRetoNumero()
                }
            )
        )
    }

    // Guardar secreto

    private fun preguntarSecreto() {
        estadoActual = EstadoFlujo.PREGUNTAR_SECRETO

        mostrarEntrada(
            texto = "Escribe tu secreto",
            pista = "Máximo 8 palabras",
            tipoEntrada = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
            textoBoton = "Guardar"
        ) { valor ->
            val secretoLimpio = valor.trim().replace(Regex("\\s+"), " ")

            if (secretoLimpio.isBlank()) {
                mostrarLinea(
                    texto = "No puedo guardar un secreto vacío."
                ) {
                    preguntarSecreto()
                }
                return@mostrarEntrada
            }

            if (contarPalabras(secretoLimpio) > 8) {
                mostrarLinea(
                    texto = "Solo puedo guardar secretos de máximo 8 palabras."
                ) {
                    preguntarSecreto()
                }
                return@mostrarEntrada
            }

            secretoPendiente = secretoLimpio
            preguntarContrasenaNuevoSecreto()
        }
    }

    private fun preguntarContrasenaNuevoSecreto() {
        estadoActual = EstadoFlujo.PREGUNTAR_CONTRASENA_NUEVO_SECRETO

        mostrarEntrada(
            texto = "Ahora escribe una contraseña",
            pista = "Contraseña",
            tipoEntrada = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            textoBoton = "Guardar"
        ) { valor ->
            val contrasena = valor.trim()

            if (contrasena.isBlank()) {
                mostrarLinea(
                    texto = "La contraseña no puede estar vacía."
                ) {
                    preguntarContrasenaNuevoSecreto()
                }
                return@mostrarEntrada
            }

            viewLifecycleOwner.lifecycleScope.launch {
                usuarioActual = viewModel.guardarSecreto(
                    nombre = nombreJugador,
                    secreto = secretoPendiente,
                    contrasena = contrasena
                )

                secretoPendiente = ""

                mostrarLinea(
                    texto = "Lo recordaré..."
                ) {
                    mostrarOpcionesVictoria()
                }
            }
        }
    }

    // Leer secreto

    private fun iniciarLoginSecreto() {
        trabajoSecuencia?.cancel()

        ocultarPanel()
        ocultarTextoGrande()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            mostrarEscena(R.drawable.navi_login)
            delay(900)

            preguntarContrasenaParaLeerSecreto()
        }
    }

    private fun preguntarContrasenaParaLeerSecreto() {
        estadoActual = EstadoFlujo.PREGUNTAR_CONTRASENA_LEER_SECRETO

        mostrarEntrada(
            texto = "Escribe la contraseña",
            pista = "Contraseña",
            tipoEntrada = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            textoBoton = "Entrar"
        ) { valor ->
            val contrasena = valor.trim()

            if (contrasena.isBlank()) {
                mostrarLinea(
                    texto = "No escribiste nada."
                ) {
                    preguntarContrasenaParaLeerSecreto()
                }
                return@mostrarEntrada
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val usuarioConSecreto = viewModel.validarContrasena(
                    nombre = nombreJugador,
                    contrasena = contrasena
                )

                if (usuarioConSecreto == null) {
                    mostrarLinea(
                        texto = "Contraseña incorrecta... o no hay secreto guardado."
                    ) {
                        mostrarMenuUsuarioExistente()
                    }
                } else {
                    mostrarSecuenciaSecreto(
                        secreto = usuarioConSecreto.secreto.orEmpty()
                    )
                }
            }
        }
    }

    private fun mostrarSecuenciaSecreto(secreto: String) {
        trabajoSecuencia?.cancel()

        ocultarPanel()
        ocultarTextoGrande()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            mostrarEscena(R.drawable.estatica_entrada)
            delay(1600)

            mostrarEscena(R.drawable.estatica_base)
            delay(700)

            mostrarSecreto(secreto)
        }
    }

    private fun mostrarSecreto(secreto: String) {
        estadoActual = EstadoFlujo.MOSTRAR_SECRETO
        textoGrandeActual = secreto

        binding.tvTextoGrande.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        binding.tvTextoGrande.isVisible = true
        binding.tvTextoGrande.text = secreto

        mostrarBotonesInferiores(
            opciones = listOf(
                Opcion("Salir") {
                    requireActivity().finishAffinity()
                },
                Opcion("Jugar") {
                    usuarioEsNuevo = false
                    reintentarRetoNumero()
                }
            )
        )
    }

    // Escenas e interfaz principal

    private fun mostrarEscena(escenaRes: Int) {
        escenaActual = escenaRes

        binding.ivEscena.visibility = View.VISIBLE
        binding.ivEscena.background = null

        Glide.with(this)
            .asGif()
            .load(escenaRes)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.ivEscena)
    }

    private fun mostrarLinea(
        texto: String,
        alContinuar: () -> Unit
    ) {
        trabajoSecuencia?.cancel()

        moverPanelAbajo()
        mostrarUltimaEscenaSiHaceFalta()
        ocultarTextoGrande()

        binding.llPanelDialogo.isVisible = true
        binding.tvMensaje.isVisible = true
        binding.tvMensaje.text = texto

        binding.etEntrada.isVisible = false
        binding.btnEnviar.isVisible = false
        binding.llContenedorOpciones.isVisible = false
        binding.llContenedorOpciones.removeAllViews()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            delay(retrasoAutomaticoMs)
            alContinuar()
        }
    }

    private fun mostrarEntrada(
        texto: String,
        pista: String,
        tipoEntrada: Int,
        textoBoton: String,
        alEnviar: (String) -> Unit
    ) {
        trabajoSecuencia?.cancel()

        moverPanelAbajo()
        mostrarUltimaEscenaSiHaceFalta()
        ocultarTextoGrande()

        binding.llPanelDialogo.isVisible = true
        binding.tvMensaje.isVisible = true
        binding.tvMensaje.text = texto

        binding.etEntrada.isVisible = false
        binding.btnEnviar.isVisible = false
        binding.llContenedorOpciones.isVisible = false
        binding.llContenedorOpciones.removeAllViews()

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            delay(retrasoAutomaticoMs)

            binding.tvMensaje.isVisible = false

            binding.etEntrada.isVisible = true
            binding.etEntrada.text?.clear()
            binding.etEntrada.hint = pista
            binding.etEntrada.inputType = tipoEntrada
            binding.etEntrada.requestFocus()

            binding.btnEnviar.isVisible = true
            binding.btnEnviar.text = textoBoton
            binding.btnEnviar.setOnClickListener {
                ocultarTeclado(binding.etEntrada)
                binding.etEntrada.clearFocus()
                alEnviar(binding.etEntrada.text.toString())
            }
        }
    }

    private fun mostrarOpciones(
        texto: String,
        opciones: List<Opcion>
    ) {
        trabajoSecuencia?.cancel()

        moverPanelAbajo()
        mostrarUltimaEscenaSiHaceFalta()
        ocultarTextoGrande()

        binding.llPanelDialogo.isVisible = true
        binding.tvMensaje.isVisible = true
        binding.tvMensaje.text = texto

        binding.etEntrada.isVisible = false
        binding.btnEnviar.isVisible = false
        binding.llContenedorOpciones.isVisible = false
        binding.llContenedorOpciones.removeAllViews()

        binding.llContenedorOpciones.orientation = LinearLayout.HORIZONTAL
        binding.llContenedorOpciones.gravity = Gravity.CENTER

        trabajoSecuencia = viewLifecycleOwner.lifecycleScope.launch {
            delay(retrasoAutomaticoMs)

            binding.tvMensaje.isVisible = false
            binding.llContenedorOpciones.isVisible = true

            opciones.forEach { opcion ->
                val boton = crearBotonOpcion(
                    texto = opcion.texto
                )

                boton.setOnClickListener {
                    opcion.accion()
                }

                binding.llContenedorOpciones.addView(boton)
            }
        }
    }

    private fun mostrarBotonesInferiores(
        opciones: List<Opcion>
    ) {
        trabajoSecuencia?.cancel()

        moverPanelAbajo()

        binding.llPanelDialogo.isVisible = true

        binding.tvMensaje.isVisible = false
        binding.tvMensaje.text = ""

        binding.etEntrada.isVisible = false
        binding.btnEnviar.isVisible = false

        binding.llContenedorOpciones.isVisible = true
        binding.llContenedorOpciones.removeAllViews()
        binding.llContenedorOpciones.orientation = LinearLayout.HORIZONTAL
        binding.llContenedorOpciones.gravity = Gravity.CENTER

        opciones.forEach { opcion ->
            val boton = crearBotonOpcion(
                texto = opcion.texto
            )

            boton.setOnClickListener {
                opcion.accion()
            }

            binding.llContenedorOpciones.addView(boton)
        }
    }

    private fun mostrarTextoGrande(texto: String) {
        estadoActual = EstadoFlujo.TEXTO_GRANDE
        textoGrandeActual = texto

        binding.llPanelDialogo.isVisible = false
        binding.tvTextoGrande.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
        binding.tvTextoGrande.isVisible = true
        binding.tvTextoGrande.text = texto
    }

    private fun ocultarTextoGrande() {
        binding.tvTextoGrande.isVisible = false
    }

    private fun ocultarPanel() {
        binding.llPanelDialogo.isVisible = false
        binding.tvMensaje.isVisible = false
        binding.etEntrada.isVisible = false
        binding.btnEnviar.isVisible = false
        binding.llContenedorOpciones.isVisible = false
        binding.llContenedorOpciones.removeAllViews()
    }

    private fun mostrarUltimaEscenaSiHaceFalta() {
        if (!binding.ivEscena.isVisible) {
            mostrarEscena(escenaActual)
        }
    }

    private fun moverPanelAbajo() {
        val parametros = binding.llPanelDialogo.layoutParams as FrameLayout.LayoutParams
        parametros.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        parametros.bottomMargin = convertirADp(28)
        binding.llPanelDialogo.layoutParams = parametros
    }

    // Componentes de interfaz

    private fun crearBotonOpcion(
        texto: String
    ): MaterialButton {
        val boton = MaterialButton(requireContext())

        boton.text = texto
        boton.setTextColor(Color.WHITE)
        boton.textSize = 14f
        boton.gravity = Gravity.CENTER

        boton.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
        boton.strokeColor = ColorStateList.valueOf(Color.WHITE)
        boton.strokeWidth = convertirADp(1)
        boton.cornerRadius = convertirADp(16)

        boton.minHeight = convertirADp(48)
        boton.minimumHeight = convertirADp(48)
        boton.minWidth = 0
        boton.minimumWidth = 0

        boton.setPadding(
            convertirADp(8),
            0,
            convertirADp(8),
            0
        )

        val parametros = LinearLayout.LayoutParams(
            0,
            convertirADp(48),
            1f
        )

        parametros.setMargins(
            convertirADp(4),
            0,
            convertirADp(4),
            0
        )

        boton.layoutParams = parametros

        return boton
    }

    // Teclado

    private fun configurarAccionTecladoEntrada() {
        binding.etEntrada.setOnEditorActionListener { vista, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ocultarTeclado(vista)
                binding.etEntrada.clearFocus()
                binding.btnEnviar.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun ocultarTeclado(vista: View) {
        val administradorEntrada = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        administradorEntrada.hideSoftInputFromWindow(
            vista.windowToken,
            0
        )
    }

    // Audio: música de fondo

    private fun iniciarMusicaFondo() {
        if (reproductorMusicaFondo != null) return

        reproductorMusicaFondo = MediaPlayer.create(requireContext(), R.raw.musica)?.apply {
            isLooping = true
            setVolume(VOLUMEN_MUSICA_FONDO, VOLUMEN_MUSICA_FONDO)

            setOnErrorListener { reproductor, _, _ ->
                reproductor.release()
                reproductorMusicaFondo = null
                true
            }

            start()
        }
    }

    private fun reanudarMusicaFondo() {
        val reproductor = reproductorMusicaFondo

        if (reproductor == null) {
            iniciarMusicaFondo()
            return
        }

        try {
            if (!reproductor.isPlaying) {
                reproductor.start()
            }
        } catch (_: IllegalStateException) {
            reproductorMusicaFondo?.release()
            reproductorMusicaFondo = null
            iniciarMusicaFondo()
        }
    }

    private fun pausarMusicaFondo() {
        try {
            reproductorMusicaFondo?.let { reproductor ->
                if (reproductor.isPlaying) {
                    reproductor.pause()
                }
            }
        } catch (_: IllegalStateException) {
            reproductorMusicaFondo?.release()
            reproductorMusicaFondo = null
        }
    }
    
    // Audio: efectos de sonido

    private fun inicializarEfectosSonido() {
        if (reproductorEfectos != null) return

        val atributosAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        reproductorEfectos = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(atributosAudio)
            .build()

        reproductorEfectos?.setOnLoadCompleteListener { _, idMuestra, estado ->
            if (estado == 0 && idMuestra == idSonidoRisa) {
                risaCargada = true
            }
        }

        idSonidoRisa = reproductorEfectos?.load(requireContext(), R.raw.risa, 1) ?: 0
    }

    private fun reproducirRisa() {
        if (risaReproducida) return

        risaReproducida = true

        val reproductor = reproductorEfectos ?: return

        if (!risaCargada) return

        reproductor.play(
            idSonidoRisa,
            VOLUMEN_RISA,
            VOLUMEN_RISA,
            1,
            0,
            1f
        )
    }

    private fun liberarReproductoresAudio() {
        reproductorMusicaFondo?.release()
        reproductorMusicaFondo = null

        reproductorEfectos?.release()
        reproductorEfectos = null

        idSonidoRisa = 0
        risaCargada = false
    }

    // Utilidades

    private fun convertirADp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    private fun contarPalabras(texto: String): Int {
        return texto
            .trim()
            .split(Regex("\\s+"))
            .filter { palabra -> palabra.isNotBlank() }
            .size
    }
}