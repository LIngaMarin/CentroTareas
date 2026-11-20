package com.ingamarin.centrotareas.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

/** Cargas simuladas con delay (datos ficticios) y el calculo de primos. */
class TareasRepositoryLocal : TareasRepository {

    override suspend fun cargarPerfil(): String {
        delay(2000)
        return "Ana Torres (ficticia)"
    }

    override suspend fun cargarPedidos(simularError: Boolean): List<String> {
        delay(3000)
        if (simularError) throw IOException("Sin conexión")
        return listOf("Pedido 101", "Pedido 102", "Pedido 103")
    }

    /**
     * Calculo pesado (usa mucho procesador): va a Dispatchers.Default.
     * ensureActive() en cada vuelta permite cancelarlo a mitad del calculo.
     */
    override suspend fun contarPrimos(hasta: Int, onProgreso: (Float) -> Unit): Int =
        withContext(Dispatchers.Default) {
            var cantidad = 0
            val total = (hasta - 1).coerceAtLeast(1)
            // Avisa solo cuando cambia el porcentaje: como maximo 100 avisos, no millones
            val paso = (total / 100).coerceAtLeast(1)
            for (n in 2..hasta) {
                ensureActive()
                if (esPrimo(n)) cantidad++
                if ((n - 1) % paso == 0) onProgreso((n - 1).toFloat() / total)
            }
            onProgreso(1f)
            cantidad
        }

    /** true si ningun d entre 2 y la raiz de n divide a n. d * d <= n en Long evita desbordes. */
    private fun esPrimo(n: Int): Boolean {
        var d = 2
        while (d.toLong() * d <= n) {
            if (n % d == 0) return false
            d++
        }
        return true
    }
}
