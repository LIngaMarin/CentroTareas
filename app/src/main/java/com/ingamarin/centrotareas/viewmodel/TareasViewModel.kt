package com.ingamarin.centrotareas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingamarin.centrotareas.data.TareasRepository
import com.ingamarin.centrotareas.model.CargaUi
import com.ingamarin.centrotareas.model.PrimosUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Estado de las tres tarjetas. Solo conoce la interface del Repository (no Context ni Composables). */
class TareasViewModel(private val repo: TareasRepository) : ViewModel() {

    companion object {
        /** Tiempo maximo de una carga cuando el interruptor esta activo. */
        const val LIMITE_MS = 2000L
    }

    // ----- Tarea con progreso (pasos 1 a 4)
    private val _mensaje = MutableStateFlow("Listo")
    val mensaje: StateFlow<String> = _mensaje.asStateFlow()

    // Avance de la tarea de 0f a 1f. Afuera solo se expone la version de lectura
    private val _progreso = MutableStateFlow(0f)
    val progreso: StateFlow<Float> = _progreso.asStateFlow()

    // La corrutina en marcha (para poder cancelarla) y si esta corriendo
    private var trabajo: Job? = null
    private val _corriendo = MutableStateFlow(false)
    val corriendo: StateFlow<Boolean> = _corriendo.asStateFlow()

    /**
     * La tarea larga en una corrutina: el trabajo pasa a un hilo de fondo (Dispatchers.Default)
     * y el hilo principal queda libre para dibujar y atender toques.
     * Avanza en 100 vueltas de 40 ms (unos 4 s) y publica el progreso.
     */
    fun tareaLarga() {
        if (_corriendo.value) return   // ya hay una en marcha: no se lanzan dos
        _corriendo.value = true
        trabajo = viewModelScope.launch {
            try {
                _progreso.value = 0f
                _mensaje.value = "Trabajando..."
                Log.d("HILO", "Antes: ${Thread.currentThread().name}")
                withContext(Dispatchers.Default) {
                    Log.d("HILO", "Dentro: ${Thread.currentThread().name}")
                    for (i in 1..100) {
                        delay(40)   // delay revisa la cancelacion
                        _progreso.value = i / 100f
                    }
                }
                Log.d("HILO", "Despues: ${Thread.currentThread().name}")
                _mensaje.value = "Terminó"
            } finally {
                _corriendo.value = false   // termine bien o cancelada
            }
        }
    }

    /** Detiene la tarea; la barra se queda donde estaba. */
    fun cancelar() {
        trabajo?.cancel()
        _mensaje.value = "Cancelado"
    }


    // ----- Contar primos (paso 5)
    private val _primos = MutableStateFlow<PrimosUi>(PrimosUi.Inicial)
    val primos: StateFlow<PrimosUi> = _primos.asStateFlow()

    // Hasta que numero se conto (para mostrarlo en el resultado)
    private val _hastaContado = MutableStateFlow(0)
    val hastaContado: StateFlow<Int> = _hastaContado.asStateFlow()

    private var trabajoPrimos: Job? = null

    /** Cuenta los primos hasta [hasta] en el Repository y mide el tiempo. */
    fun contar(hasta: Int) {
        if (_primos.value is PrimosUi.Calculando) return
        _primos.value = PrimosUi.Calculando
        _hastaContado.value = hasta
        trabajoPrimos = viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            try {
                val cantidad = repo.contarPrimos(hasta)
                _primos.value = PrimosUi.Listo(cantidad, System.currentTimeMillis() - inicio)
            } catch (e: CancellationException) {
                _primos.value = PrimosUi.Cancelado
                throw e   // se vuelve a lanzar: si no, la cancelacion se rompe
            }
        }
    }

    fun cancelarPrimos() {
        trabajoPrimos?.cancel()
    }

    // ----- Dos cargas (pasos 6 y 7)
    private val _carga = MutableStateFlow<CargaUi>(CargaUi.Inicial)
    val carga: StateFlow<CargaUi> = _carga.asStateFlow()

    // Interruptor "Simular error": hace que los pedidos fallen con IOException("Sin conexión")
    private val _simularError = MutableStateFlow(false)
    val simularError: StateFlow<Boolean> = _simularError.asStateFlow()

    fun cambiarSimularError(valor: Boolean) {
        _simularError.value = valor
    }

    // ---------- Mejora IA 1: tiempo maximo ----------
    // Interruptor "Tiempo maximo 2 s": si esta activo, la carga se corta con withTimeout
    private val _limitarTiempo = MutableStateFlow(false)
    val limitarTiempo: StateFlow<Boolean> = _limitarTiempo.asStateFlow()

    fun cambiarLimitarTiempo(valor: Boolean) {
        _limitarTiempo.value = valor
    }

    /** Perfil (2 s) y despues pedidos (3 s): 2 + 3 = unos 5 s. */
    fun cargarUnaPorUna() = cargar("Una por una") {
        val perfil = repo.cargarPerfil()
        val pedidos = repo.cargarPedidos(_simularError.value)
        Pair(perfil, pedidos)
    }

    /** Las dos al mismo tiempo con async: el maximo de 2 s y 3 s = unos 3 s. */
    fun cargarALaVez() = cargar("A la vez") {
        coroutineScope {
            val a = async { repo.cargarPerfil() }
            val b = async { repo.cargarPedidos(_simularError.value) }
            Pair(a.await(), b.await())
        }
    }

    /** Reintentar desde el estado de error: vuelve a hacer la carga A la vez. */
    fun reintentar() = cargarALaVez()

    /**
     * Comun a las dos cargas: estado Cargando, mide el tiempo y termina en Exito o Error.
     * La CancellationException se deja pasar (throw e) para no romper la cancelacion.
     */
    private fun cargar(modo: String, obtener: suspend () -> Pair<String, List<String>>) {
        if (_carga.value is CargaUi.Cargando) return
        _carga.value = CargaUi.Cargando
        viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            try {
                val (perfil, pedidos) =
                    if (_limitarTiempo.value) withTimeout(LIMITE_MS) { obtener() } else obtener()
                _carga.value = CargaUi.Exito(modo, perfil, pedidos.size, System.currentTimeMillis() - inicio)
            } catch (e: TimeoutCancellationException) {
                // Va ANTES que CancellationException: es una subclase suya
                _carga.value = CargaUi.Error("Tardó demasiado (más de ${LIMITE_MS / 1000} s)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _carga.value = CargaUi.Error("No se pudo cargar: ${e.message}")
            }
        }
    }

}
