package com.enigma.weatherapp.remote

import com.enigma.weatherapp.BuildConfig
import com.enigma.weatherapp.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("weather/")
    suspend fun getWeatherByCityName(
        @Query("q") cityName: String,
        @Query("appid") appid: String = BuildConfig.SECRET_KEY
    ): WeatherResponse
}