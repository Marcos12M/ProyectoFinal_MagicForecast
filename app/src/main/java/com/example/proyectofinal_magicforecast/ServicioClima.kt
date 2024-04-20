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

    @GET("forecast")
    fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("cnt") count: Int,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Call<ForecastResponse>

    @GET("weather")
    fun getCurrentWeatherCity(
        @Query("q") city: String,
        @Query("units") units: String,
        @Query("appid") apiKey: String
    ):Call<WeatherResponse>
}