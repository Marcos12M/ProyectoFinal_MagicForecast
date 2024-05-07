package com.example.proyectofinal_magicforecast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forecastDAY")
data class ForecastDayBD (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val temp: Double,
    val windSpeed: Double,
    val humidity: Int,
    val posicion: Int,
    val cityName: String
)

