package com.example.proyectofinal_magicforecast
data class ForecastResponse(
    val cod: String,
    val message: Double,
    val cnt: Int,
    val list: List<Forecast>,
    val city: City
)

data class City(
    val id: Int,
    val name: String,
    val coord: Coordinates,
    val country: String,
    val population: Int,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)

data class Forecast(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int,
    val pop: Double,
    val sys: Sys,
    val dt_txt: String
)

data class Clouds(
    val all: Int
)