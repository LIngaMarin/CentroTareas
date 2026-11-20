package com.ingamarin.centrotareas.data

/** Contrato de las tareas que tardan. El ViewModel solo conoce esta interface. */
interface TareasRepository {
    suspend fun contarPrimos(hasta: Int): Int
    suspend fun cargarPerfil(): String
    suspend fun cargarPedidos(simularError: Boolean): List<String>
}
