package com.ingamarin.centrotareas.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ingamarin.centrotareas.data.TareasRepositoryLocal

/** Crea el TareasViewModel con su Repository. */
object TareasViewModelFactory {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { TareasViewModel(TareasRepositoryLocal()) }
    }
}
