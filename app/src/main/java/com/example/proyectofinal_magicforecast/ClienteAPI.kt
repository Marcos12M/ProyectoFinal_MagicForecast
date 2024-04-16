package com.example.proyectofinal_magicforecast

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
object ClienteAPI {

    private val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private var retrofit: Retrofit? = null

    val instance: ServicioClima by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ServicioClima::class.java)
    }
}