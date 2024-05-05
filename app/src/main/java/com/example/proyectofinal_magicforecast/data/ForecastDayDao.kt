package com.example.proyectofinal_magicforecast.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ForecastDayDao {
    @Query("SELECT * FROM forecastDAY")
    fun getAllForecast(): List<ForecastDayBD>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(forecast: ForecastDayBD)
}