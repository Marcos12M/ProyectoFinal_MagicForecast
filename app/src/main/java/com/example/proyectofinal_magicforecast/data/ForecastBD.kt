package com.example.proyectofinal_magicforecast.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forecast")
data class ForecastBD(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val temp: Double,
    val windSpeed: Double,
    val humidity: Int,
    val icon: String
)