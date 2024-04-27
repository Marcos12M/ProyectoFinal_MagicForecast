package com.example.proyectofinal_magicforecast

import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

class Pantalla_Comparacion : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mapView1: MapView
    private lateinit var mapView2: MapView
    private var googleMap1: GoogleMap? = null
    private var googleMap2: GoogleMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_comparacion)

        val backButton: Button = findViewById(R.id.botonRegresar)
        backButton.setOnClickListener {
            finish() // Cierra la Activity actual y regresa a la anterior en la pila de Activities
        }

        mapView1 = findViewById<MapView>(R.id.mapView1).apply {
            onCreate(savedInstanceState)
            getMapAsync(this@Pantalla_Comparacion)
        }

        mapView2 = findViewById<MapView>(R.id.mapView2).apply {
            onCreate(savedInstanceState)
            getMapAsync(this@Pantalla_Comparacion)
        }

        // Configurar los SearchViews para manejar las búsquedas.
        val searchView1 = findViewById<SearchView>(R.id.search_viewComparar)
        val searchView2 = findViewById<SearchView>(R.id.search_viewComparar2)

        searchView1.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { cityName ->
                    updateMap(cityName, googleMap1)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        searchView2.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { cityName ->
                    updateMap(cityName, googleMap2)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    // Esta función se llama cuando el mapa está listo para usarse.
    override fun onMapReady(googleMap: GoogleMap) {
        if (googleMap1 == null) {
            googleMap1 = googleMap
        } else {
            googleMap2 = googleMap
        }
    }

    // Actualiza el mapa con la ubicación de la ciudad proporcionada.
    private fun updateMap(cityName: String, map: GoogleMap?) {
        map?.let { googleMap ->
            // Usa Geocoder para obtener la ubicación de la ciudad.
            val geocoder = Geocoder( this)
            try {
                val addressList = geocoder.getFromLocationName(cityName, 1)
                if (addressList != null && addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    googleMap.clear()
                    googleMap.addMarker(MarkerOptions().position(latLng).title(cityName))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                } else {
                    // Manejar el caso donde la dirección no es encontrada
                    Toast.makeText(this, "No se encontró la dirección.", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                // Manejar la excepción de IO
                Toast.makeText(this, "Error al buscar la ciudad: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Asegúrate de llamar a los métodos del ciclo de vida del mapa.
    override fun onStart() {
        super.onStart()
        mapView1.onStart()
        mapView2.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView1.onResume()
        mapView2.onResume()
    }

    override fun onPause() {
        mapView1.onPause()
        mapView2.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView1.onStop()
        mapView2.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mapView1.onDestroy()
        mapView2.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView1.onSaveInstanceState(outState)
        mapView2.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView1.onLowMemory()
        mapView2.onLowMemory()
    }
}