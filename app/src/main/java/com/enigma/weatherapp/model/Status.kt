package com.enigma.weatherapp.model

sealed class Status<out T> {
    data class Success<out T>(val data: T) : Status<T>()

    data class Error(var e: String) : Status<Nothing>()

    object Loading : Status<Nothing>()

}