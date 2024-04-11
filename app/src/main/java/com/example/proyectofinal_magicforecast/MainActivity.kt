package com.example.proyectofinal_magicforecast

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val compareButton: Button = findViewById(R.id.BotonAbrirCompararCiudades)
        compareButton.setOnClickListener {
            val intent = Intent(this, Pantalla_Comparacion::class.java)
            startActivity(intent)
        }

    }
}