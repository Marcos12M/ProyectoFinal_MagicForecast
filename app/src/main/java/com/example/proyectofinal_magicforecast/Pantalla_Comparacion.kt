package com.example.proyectofinal_magicforecast

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Pantalla_Comparacion : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_comparacion)

        val backButton: Button = findViewById(R.id.botonRegresar)
        backButton.setOnClickListener {
            finish() // Cierra la Activity actual y regresa a la anterior en la pila de Activities
        }

    }
}