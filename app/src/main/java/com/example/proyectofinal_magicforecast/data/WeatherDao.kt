package com.example.proyectofinal_magicforecast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
@Dao
interface WeatherDao {

    @Insert
    fun insert(weather: WeatherBD)

    @Query("SELECT * FROM weather WHERE date = :date")
    fun getWeatherByDate(date: String): List<WeatherBD>
}