package com.example.proyectofinal_magicforecast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ForecastDayDao {
    @Query("SELECT * FROM forecastDAY WHERE posicion = :posicion")
    fun getAllForecast(posicion: Int): List<ForecastDayBD>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(forecast: ForecastDayBD)
}