package com.example.proyectofinal_magicforecast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherBD(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val temp: Double,
    val description: String,
    val pressure: Int,
    val windSpeed: Double,
    val humidity: Int,
    val country: String,
    val icon: String
)