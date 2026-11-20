package com.ingamarin.centrotareas.model

/** Estados del calculo de primos. */
sealed interface PrimosUi {
    data object Inicial : PrimosUi
    data object Calculando : PrimosUi
    data class Listo(val cantidad: Int, val milisegundos: Long) : PrimosUi
    data object Cancelado : PrimosUi
}

/** Un intento de carga exitoso, para la tabla de comparacion (Mejora IA 2). */
data class IntentoCarga(val modo: String, val milisegundos: Long)

/** Estados de las dos cargas (perfil y pedidos). */
sealed interface CargaUi {
    data object Inicial : CargaUi
    data object Cargando : CargaUi
    data class Exito(
        val modo: String,
        val perfil: String,
        val pedidos: Int,
        val milisegundos: Long
    ) : CargaUi
    data class Error(val mensaje: String) : CargaUi
}
