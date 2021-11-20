package com.enigma.weatherapp.repo

import com.enigma.weatherapp.remote.ApiService


class WeatherRepository(private val apiService: ApiService) {
    suspend fun getCityWeatherByName(cityName: String) =
        apiService.getWeatherByCityName(cityName = cityName)
}