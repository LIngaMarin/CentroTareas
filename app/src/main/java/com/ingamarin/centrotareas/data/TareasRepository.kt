package com.ingamarin.centrotareas.data

/** Contrato de las tareas que tardan. El ViewModel solo conoce esta interface. */
interface TareasRepository {
    /** onProgreso recibe el avance de 0f a 1f. Es una funcion de Kotlin: el Repository no conoce la UI. */
    suspend fun contarPrimos(hasta: Int, onProgreso: (Float) -> Unit = {}): Int
    suspend fun cargarPerfil(): String
    suspend fun cargarPedidos(simularError: Boolean): List<String>
}
