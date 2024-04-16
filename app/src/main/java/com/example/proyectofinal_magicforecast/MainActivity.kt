package com.example.proyectofinal_magicforecast

import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.widget.Toast
import androidx.core.app.ActivityCompat
import retrofit2.Call
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        val compareButton: Button = findViewById(R.id.BotonAbrirCompararCiudades)
        compareButton.setOnClickListener {
            val intent = Intent(this, Pantalla_Comparacion::class.java)
            startActivity(intent)
        }

    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Explicar por qué necesitas el permiso antes de solicitarlo formalmente
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Se necesita acceso a la ubicación para mostrar el clima local.", Toast.LENGTH_LONG).show()
            }
            // Solicitar el permiso
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permiso ya concedido
            fetchLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                fetchLocation()
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Usar la ubicación aquí
                    val latitude = location.latitude
                    val longitude = location.longitude
                    fetchWeather(latitude, longitude)
                } else {
                    Toast.makeText(this, "Ubicación no disponible.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        ClienteAPI.instance.getCurrentWeather(lat, lon, "metric", "0f2d487b9ae4f71cc6f8eacc1f5cad8c")
            .enqueue(object : retrofit2.Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            val temp = it.main.temp
                            val tempString = "%.0f".format(temp)
                            val description = it.weather[0].description
                            val pressure = it.main.pressure
                            val windSpeed = it.wind.speed
                            val humidity = it.main.humidity

                            // Actualizar la interfaz de usuario con los nuevos datos
                            findViewById<TextView>(R.id.textGrados).text = "$tempString°C"
                            findViewById<TextView>(R.id.textClima).text = "Descripción: $description"
                            findViewById<TextView>(R.id.textPressure).text = "Presión: $pressure hPa"
                            findViewById<TextView>(R.id.textWind).text = "Velocidad del viento: $windSpeed m/s"
                            findViewById<TextView>(R.id.textHumedad).text = "Humedad: $humidity %"
                        }
                    } else {
                        Toast.makeText(applicationContext, "Error al obtener clima", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }
}