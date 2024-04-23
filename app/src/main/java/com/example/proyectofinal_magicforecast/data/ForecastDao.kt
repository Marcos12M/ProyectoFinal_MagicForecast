package com.example.proyectofinal_magicforecast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface ForecastDao {
    @Query("SELECT * FROM forecast")
    fun getAllForecast(): List<ForecastBD>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(forecast: ForecastBD)
}