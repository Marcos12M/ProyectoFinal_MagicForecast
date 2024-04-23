package com.example.proyectofinal_magicforecast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather ORDER BY id DESC LIMIT 1")
    fun getWeather(): WeatherBD?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(weather: WeatherBD)
}