package com.example.proyectofinal_magicforecast

import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import retrofit2.Call
import retrofit2.Response
import com.bumptech.glide.Glide
import com.example.proyectofinal_magicforecast.data.AppDatabase
import com.example.proyectofinal_magicforecast.data.WeatherDao
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: AppDatabase
    private lateinit var weatherDao: WeatherDao

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)
        weatherDao = db.weatherDao()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        val compareButton: Button = findViewById(R.id.BotonAbrirCompararCiudades)
        compareButton.setOnClickListener {
            val intent = Intent(this, Pantalla_Comparacion::class.java)
            startActivity(intent)
        }

    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Explicar por qué necesitas el permiso antes de solicitarlo formalmente
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(
                    this,
                    "Se necesita acceso a la ubicación para mostrar el clima local.",
                    Toast.LENGTH_LONG
                ).show()
            }
            // Solicitar el permiso
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permiso ya concedido
            fetchLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    fetchWeather(latitude, longitude)
                    fetchForecast(latitude, longitude)
                } else {
                    Toast.makeText(this, "Ubicación no disponible.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        ClienteAPI.instance.getCurrentWeather(
            lat,
            lon,
            "metric",
            "0f2d487b9ae4f71cc6f8eacc1f5cad8c"
        )
            .enqueue(object : retrofit2.Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            val temp = it.main.temp
                            val tempString = "%.0f".format(temp)
                            val description = it.weather[0].description
                            val pressure = it.main.pressure
                            val windSpeed = it.wind.speed
                            val humidity = it.main.humidity
                            val country = it.name

                            val iconFileName = "c${it.weather[0].icon}"

                            val iconResourceId =
                                resources.getIdentifier(iconFileName, "drawable", packageName)

                            findViewById<ImageView>(R.id.imgClima).setImageResource(iconResourceId)

                            findViewById<TextView>(R.id.textCiudad).text = "$country"
                            findViewById<TextView>(R.id.textGrados).text = "$tempString°C"

                            val descripcion = when (description) {
                                "clear sky" -> "Cielo despejado"
                                "few clouds" -> "Hay pocas nubes"
                                "scattered clouds" -> "Hay nubes dispersas"
                                "broken clouds" -> "Esta nublado"
                                "shower rain" -> "Hay un aguacero"
                                "rain" -> "Esta lloviendo"
                                "thunderstorm" -> "Hay tormenta eléctrica"
                                "snow" -> "Clima con nieve"
                                "mist" -> "Clima con niebla"
                                else -> "???"
                            }

                            findViewById<TextView>(R.id.textClima).text = "$descripcion"
                            findViewById<TextView>(R.id.textPressure).text =
                                "Presión: $pressure hPa"
                            findViewById<TextView>(R.id.textWind).text = "Viento: $windSpeed m/s"
                            findViewById<TextView>(R.id.textHumidity).text = "Humedad: $humidity %"
                        }
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error al obtener clima",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchForecast(lat: Double, lon: Double) {
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
                            val filteredList = list.filterIndexed { index, _ -> index % 8 == 0 }

                            val linearLayout =
                                findViewById<LinearLayout>(R.id.linearLayout_forecast)
                            linearLayout.removeAllViews()
                            filteredList.forEach { forecast ->
                                val dateTimeString = forecast.dt_txt
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                val dateTime = LocalDateTime.parse(dateTimeString, formatter)
                                val formattedDate =
                                    dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                val iconFileName = "c${forecast.weather[0].icon}"
                                val temp = "${forecast.main.temp}°"
                                val wind = "${forecast.wind.speed} m/s"
                                val humidity = "${forecast.main.humidity}%"

                                val forecastLayout = LinearLayout(this@MainActivity)
                                val marginInPixels =
                                    (10 * resources.displayMetrics.density).toInt()
                                val layoutParams = LinearLayout.LayoutParams(
                                    300,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                layoutParams.setMargins(
                                    20,
                                    0,
                                    20,
                                    marginInPixels
                                )
                                forecastLayout.layoutParams = layoutParams
                                forecastLayout.orientation = LinearLayout.VERTICAL

                                val dateTextView = TextView(this@MainActivity)
                                dateTextView.text = formattedDate
                                dateTextView.setTextColor(Color.BLACK)
                                dateTextView.gravity = Gravity.CENTER
                                dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                dateTextView.setTypeface(null, Typeface.BOLD)
                                forecastLayout.addView(dateTextView)

                                val iconImageView = ImageView(this@MainActivity)
                                val iconResourceId =
                                    resources.getIdentifier(iconFileName, "drawable", packageName)

                                iconImageView.setImageResource(iconResourceId)
                                val iconParams = LinearLayout.LayoutParams(
                                    290,
                                    290
                                )
                                iconParams.gravity = Gravity.CENTER
                                iconImageView.layoutParams = iconParams
                                forecastLayout.addView(iconImageView)

                                val tempTextView = TextView(this@MainActivity)
                                tempTextView.text = temp
                                tempTextView.setTextColor(Color.BLACK)
                                tempTextView.gravity = Gravity.CENTER
                                tempTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                tempTextView.setTypeface(null, Typeface.BOLD)
                                forecastLayout.addView(tempTextView)

                                val windTextView = TextView(this@MainActivity)
                                windTextView.text = wind
                                windTextView.setTextColor(Color.BLACK)
                                windTextView.gravity = Gravity.CENTER
                                windTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                windTextView.setTypeface(null, Typeface.BOLD)
                                forecastLayout.addView(windTextView)

                                val humidityTextView = TextView(this@MainActivity)
                                humidityTextView.text = humidity
                                humidityTextView.setTextColor(Color.BLACK)
                                humidityTextView.gravity = Gravity.CENTER
                                humidityTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                humidityTextView.setTypeface(null, Typeface.BOLD)
                                forecastLayout.addView(humidityTextView)

                                linearLayout.addView(forecastLayout)
                            }
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
}