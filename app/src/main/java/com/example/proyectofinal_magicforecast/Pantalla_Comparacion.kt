package com.example.proyectofinal_magicforecast

import android.graphics.Typeface
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
                    updateMap(cityName, googleMap1, 1)
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
                    updateMap(cityName, googleMap2, 2)
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
    private fun updateMap(cityName: String, map: GoogleMap?, cual: Int) {
        map?.let { googleMap ->
            // Usa Geocoder para obtener la ubicación de la ciudad.
            val geocoder = Geocoder( this)
            try {
                val addressList = geocoder.getFromLocationName(cityName, 1)
                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    fetchForecastDay(address.latitude,address.longitude, cual)
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

    private fun fetchForecastDay(lat: Double, lon: Double , cual: Int) {
        ClienteAPI.instance.getForecast(lat, lon, 40, "0f2d487b9ae4f71cc6f8eacc1f5cad8c", "metric")
            .enqueue(object : retrofit2.Callback<ForecastResponse> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<ForecastResponse>,
                    response: Response<ForecastResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            val list = it.list
                            val filteredList = list.take(8)
                            var linearLayout: LinearLayout? = null
                            val chart: LineChart

                            if (cual == 1) {
                                linearLayout = findViewById<LinearLayout>(R.id.linearLayout_forecastDAY1)
                                chart = findViewById<LineChart>(R.id.chartForecast1)
                                findViewById<HorizontalScrollView>(R.id.forecast_day1).visibility = View.VISIBLE
                            }else{
                                linearLayout = findViewById<LinearLayout>(R.id.linearLayout_forecastDAY2)
                                chart = findViewById<LineChart>(R.id.chartForecast2)
                                findViewById<HorizontalScrollView>(R.id.forecast_day2).visibility = View.VISIBLE
                            }
                            linearLayout.removeAllViews()

                            val xvalue =  ArrayList<String>()
                            var count = 0f
                            val lineentry = ArrayList<Entry>()

                            filteredList.forEach { forecast ->
                                val dateTimeString = forecast.dt_txt
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                val dateTime = LocalDateTime.parse(dateTimeString, formatter)
                                val formattedTime =
                                    dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                                val temp = forecast.main.temp.toFloat()
                                val wind = "${forecast.wind.speed} m/s"
                                val humidity = "${forecast.main.humidity}%"
                                xvalue.add("$formattedTime\n$wind\n$humidity")
                                lineentry.add(Entry(count, temp))
                                count++
                            }

                            val linedataset = LineDataSet(lineentry, "Temperatura en °")
                            linedataset.setDrawIcons(true)
                            linedataset.setDrawValues(true)
                            linedataset.color = resources.getColor(R.color.color2)

                            val xAxis = chart.xAxis
                            xAxis.valueFormatter = IndexAxisValueFormatter(xvalue)
                            xAxis.textSize = 18f
                            xAxis.typeface = Typeface.DEFAULT_BOLD
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            chart.setXAxisRenderer(
                                CustomXAxisRenderer(
                                    chart.viewPortHandler,
                                    chart.xAxis,
                                    chart.getTransformer(YAxis.AxisDependency.LEFT)
                                )
                            )

                            val data= LineData(linedataset)

                            data.setValueTextSize(18f)
                            data.setValueTypeface(Typeface.DEFAULT_BOLD)

                            chart.data = data

                            val legend = chart.legend
                            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP

                            chart.axisLeft.setDrawLabels(false)
                            chart.axisRight.setDrawLabels(false)
                            chart.animateXY(2000,2000)
                            chart.extraLeftOffset = 40f
                            chart.extraRightOffset = 45f
                            chart.extraBottomOffset = 60f
                            chart.description = null

                            linearLayout.addView(chart)
                        }
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error al obtener clima",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                    println(t.message)
                }
            })

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