package com.example.proyectofinal_magicforecast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherBD(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val weather: String,
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double
)
