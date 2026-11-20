package com.ingamarin.centrotareas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ingamarin.centrotareas.model.CargaUi
import com.ingamarin.centrotareas.model.IntentoCarga
import com.ingamarin.centrotareas.model.PrimosUi
import com.ingamarin.centrotareas.ui.theme.VerdeFondo
import com.ingamarin.centrotareas.ui.theme.VerdeTexto
import com.ingamarin.centrotareas.viewmodel.TareasViewModel

/** Pantalla principal: tres tarjetas. Solo muestra el estado del ViewModel y le avisa los eventos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTareas(vm: TareasViewModel) {
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val progreso by vm.progreso.collectAsStateWithLifecycle()
    val corriendo by vm.corriendo.collectAsStateWithLifecycle()
    val primos by vm.primos.collectAsStateWithLifecycle()
    val hastaContado by vm.hastaContado.collectAsStateWithLifecycle()
    val carga by vm.carga.collectAsStateWithLifecycle()
    val simularError by vm.simularError.collectAsStateWithLifecycle()
    val limitarTiempo by vm.limitarTiempo.collectAsStateWithLifecycle()
    val historial by vm.historial.collectAsStateWithLifecycle()
    val promedios by vm.promedios.collectAsStateWithLifecycle()
    // Por defecto 8 000 000: con 500 000 tardaba ~100 ms en este emulador; la guia pide entre 2 y 5 s
    var textoHasta by rememberSaveable { mutableStateOf("8000000") }

    // Prueba de vida: si deja de sumar, la pantalla esta congelada
    var toques by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CentroTareas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaTarea("Tarea con progreso") {
                Button(onClick = { toques++ }) { Text("Tócame: $toques") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.tareaLarga() }, enabled = !corriendo) { Text("Iniciar") }
                    OutlinedButton(onClick = { vm.cancelar() }, enabled = corriendo) { Text("Cancelar") }
                }
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${(progreso * 100).toInt()} %", style = MaterialTheme.typography.bodySmall)
                Text(mensaje, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TarjetaTarea("Contar números primos") {
                val calculando = primos is PrimosUi.Calculando
                OutlinedTextField(
                    value = textoHasta,
                    onValueChange = { textoHasta = it.filter(Char::isDigit) },
                    label = { Text("Hasta el número") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        // Se ignoran textos no numericos o menores que 2
                        onClick = { textoHasta.toIntOrNull()?.takeIf { it >= 2 }?.let { vm.contar(it) } },
                        enabled = !calculando
                    ) { Text("Contar") }
                    OutlinedButton(onClick = { vm.cancelarPrimos() }, enabled = calculando) { Text("Cancelar") }
                }
                when (val p = primos) {
                    PrimosUi.Inicial -> Unit
                    PrimosUi.Calculando -> Text("Calculando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    is PrimosUi.Listo -> RecuadroExito(
                        "Primos hasta $hastaContado: ${p.cantidad}",
                        "Tiempo: ${p.milisegundos} ms"
                    )
                    PrimosUi.Cancelado -> Text("Cancelado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TarjetaTarea("Dos cargas") {
                val cargando = carga is CargaUi.Cargando
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(checked = simularError, onCheckedChange = { vm.cambiarSimularError(it) })
                    Text("Simular error")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(checked = limitarTiempo, onCheckedChange = { vm.cambiarLimitarTiempo(it) })
                    Text("Tiempo máximo 2 s")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.cargarUnaPorUna() }, enabled = !cargando) { Text("Una por una") }
                    Button(onClick = { vm.cargarALaVez() }, enabled = !cargando) { Text("A la vez") }
                }
                when (val c = carga) {
                    CargaUi.Inicial -> Unit
                    CargaUi.Cargando -> CircularProgressIndicator()
                    is CargaUi.Exito -> RecuadroExito(
                        "Modo: ${c.modo}",
                        "Perfil: ${c.perfil}",
                        "Pedidos: ${c.pedidos}",
                        "Tiempo: ${c.milisegundos} ms"
                    )
                    is CargaUi.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                c.mensaje,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        OutlinedButton(onClick = { vm.reintentar() }) { Text("Reintentar") }
                    }
                }

                // Tabla de comparacion de tiempos (solo si ya hay intentos)
                if (historial.isNotEmpty()) {
                    TablaTiempos(
                        historial = historial,
                        promedios = promedios,
                        onLimpiar = { vm.limpiarHistorial() }
                    )
                }
            }
        }
    }
}

/** Tabla con los ultimos intentos, el promedio de cada modo y cuanto ahorra "A la vez". */
@Composable
fun TablaTiempos(
    historial: List<IntentoCarga>,
    promedios: Map<String, Long>,
    onLimpiar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HorizontalDivider()
        Text("Comparación de tiempos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth()) {
            Text("#", Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
            Text("Modo", Modifier.weight(2f), fontWeight = FontWeight.Bold)
            Text("Tiempo", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        historial.forEachIndexed { i, intento ->
            Row(Modifier.fillMaxWidth()) {
                Text("${i + 1}", Modifier.weight(0.5f))
                Text(intento.modo, Modifier.weight(2f))
                Text("${intento.milisegundos} ms", Modifier.weight(1.5f), textAlign = TextAlign.End)
            }
        }
        val unaPorUna = promedios["Una por una"]
        val aLaVez = promedios["A la vez"]
        if (unaPorUna != null && aLaVez != null) {
            RecuadroExito(
                "A la vez ahorra ${unaPorUna - aLaVez} ms",
                "Promedio una por una: $unaPorUna ms",
                "Promedio a la vez: $aLaVez ms"
            )
        }
        TextButton(onClick = onLimpiar, modifier = Modifier.align(Alignment.End)) { Text("Limpiar historial") }
    }
}

/** Recuadro verde con el resultado: primera linea en negrita y el resto debajo. */
@Composable
fun RecuadroExito(titulo: String, vararg lineas: String) {
    Surface(
        color = VerdeFondo,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titulo, color = VerdeTexto, fontWeight = FontWeight.Bold)
            lineas.forEach { Text(it, color = VerdeTexto, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/** Card con el título en azul y el contenido centrado. */
@Composable
fun TarjetaTarea(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                titulo,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            contenido()
        }
    }
}
