package com.example.proyectofinal_magicforecast

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
interface ServicioClima {
    @GET("weather")
    fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String,
        @Query("appid") apiKey: String
    ): Call<WeatherResponse>
}