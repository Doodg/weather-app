package com.enigma.weatherapp.repo

import com.enigma.weatherapp.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepoModule {
    @Singleton
    @Provides
    fun providesRepository(apiService: ApiService) = WeatherRepository(apiService)
}