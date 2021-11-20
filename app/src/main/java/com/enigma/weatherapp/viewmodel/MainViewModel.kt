package com.enigma.weatherapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enigma.weatherapp.model.Status
import com.enigma.weatherapp.model.WeatherResponse
import com.enigma.weatherapp.repo.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val galleryRepository: WeatherRepository) :
    ViewModel() {
    val weatherResponseLiveData = MutableLiveData<Status<WeatherResponse>>()

    fun getCurrentLocationWeather(cityName: String) {
        weatherResponseLiveData.postValue(Status.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = galleryRepository.getCityWeatherByName(cityName)
                weatherResponseLiveData.postValue(Status.Success(response))
            } catch (e: Throwable) {
                if (e is HttpException)
                    weatherResponseLiveData.postValue(Status.Error(e.message()))
                else
                    weatherResponseLiveData.postValue(Status.Error(e.localizedMessage))
            }
        }
    }
}