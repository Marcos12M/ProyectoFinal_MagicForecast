package com.example.proyectofinal_magicforecast

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.proyectofinal_magicforecast.data.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: AppDatabase
    private lateinit var weatherDao: WeatherDao
    private lateinit var searchAdapter: CursorAdapter

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

        val searchView = findViewById<SearchView>(R.id.search_view)
        searchAdapter = SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, arrayOf("city"), intArrayOf(android.R.id.text1), 0)
        searchView.suggestionsAdapter = searchAdapter
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchForCity(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchAdapter.getItem(position) as Cursor
                val index = cursor.getColumnIndex("city")
                val city = cursor.getString(index)
                searchForCity(city)
                searchView.setQuery(city, false)
                return true
            }
        })

        // Carga el historial de búsqueda cuando se abre el SearchView
        searchView.setOnSearchClickListener {
            loadSearchHistory()
        }

    }

    private fun loadSearchHistory() {
        val history = getSearchHistory()
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, "city"))

        history.forEachIndexed { index, city ->
            cursor.addRow(arrayOf(index, city))
        }

        searchAdapter.changeCursor(cursor)
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
                    fetchForecastDay(latitude, longitude)

                } else {
                    Toast.makeText(this, "Ubicación no disponible.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun searchForCity(city: String) {
        ClienteAPI.instance.getCurrentWeatherCity(city, "metric", "0f2d487b9ae4f71cc6f8eacc1f5cad8c")
            .enqueue(object : retrofit2.Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val weatherData = response.body()
                        saveSearchToHistory(city)
                        updateUI(weatherData)
                    } else {
                        Toast.makeText(applicationContext, "Error al obtener clima a", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(weatherData: WeatherResponse?) {
        weatherData?.let { weather ->
            fetchWeather(weather.coord.lat, weather.coord.lon)
            fetchForecast(weather.coord.lat, weather.coord.lon)
            fetchForecastDay(weather.coord.lat, weather.coord.lon)
        } ?: run {
            Toast.makeText(this, "No se pudo actualizar la UI con los datos del clima.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSearchToHistory(city: String) {
        val sharedPreferences = getSharedPreferences("weatherApp", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val searches = getSearchHistory().toMutableSet() // Obtiene el historial actual y lo convierte en un MutableSet para evitar duplicados

        searches.add(city) // Añade la nueva búsqueda al conjunto
        editor.putStringSet("searchHistory", searches)
        editor.apply()
    }

    private fun getSearchHistory(): Set<String> {
        val sharedPreferences = getSharedPreferences("weatherApp", MODE_PRIVATE)
        return sharedPreferences.getStringSet("searchHistory", setOf()) ?: setOf()
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

                            val weather = WeatherBD(
                                temp = temp,
                                description = description,
                                pressure = pressure,
                                windSpeed = windSpeed,
                                humidity = humidity,
                                country = country,
                                icon = iconFileName
                            )

                            // Insertar en la base de datos utilizando coroutines
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = AppDatabase.getDatabase(applicationContext).weatherDao()
                                dao.insert(weather)

                                // Consultar y verificar si los datos se guardaron correctamente
                                val savedWeather = dao.getWeather()
                                if (savedWeather != null) {
                                    Log.d("Weather", "Datos guardados correctamente: Temperature: ${savedWeather.temp}, Description: ${savedWeather.description}, Pressure: ${savedWeather.pressure}, Wind Speed: ${savedWeather.windSpeed}, Humidity: ${savedWeather.humidity}, Country: ${savedWeather.country}, Icon: ${savedWeather.icon}")
                                } else {
                                    Log.e("Weather", "Error: No se encontraron datos guardados")
                                }
                            }

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
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setTitle("Datos cargados")
                    builder.setMessage("Se han cargado los datos desde la base de datos")
                    builder.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    val dialog = builder.create()
                    dialog.show()

                    // Intenta obtener el último clima guardado de la base de datos
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getDatabase(applicationContext).weatherDao()
                        val savedWeather = dao.getWeather()
                        if (savedWeather != null) {

                            // Si se encuentra un clima guardado, actualiza la interfaz de usuario con esos datos
                            withContext(Dispatchers.Main) {

                                val iconResourceId =
                                    resources.getIdentifier(savedWeather.icon, "drawable", packageName)

                                findViewById<ImageView>(R.id.imgClima).setImageResource(iconResourceId)

                                findViewById<TextView>(R.id.textCiudad).text = "${savedWeather.country}"
                                findViewById<TextView>(R.id.textGrados).text = "${"%.0f".format(savedWeather.temp)}°C"

                                val descripcion = when (savedWeather.description) {
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
                                    "Presión: ${savedWeather.pressure} hPa"
                                findViewById<TextView>(R.id.textWind).text = "Viento: ${savedWeather.windSpeed} m/s"
                                findViewById<TextView>(R.id.textHumidity).text = "Humedad: ${savedWeather.humidity} %"
                            }
                        } else {
                            // Si no se encuentra ningún clima guardado, muestra un mensaje de error
                            withContext(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "No se encontraron datos guardados", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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

                        val dao = AppDatabase.getDatabase(applicationContext).forecastDao()


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

                            val forecastEntry = ForecastBD(
                                date = formattedDate,
                                temp = forecast.main.temp,
                                windSpeed = forecast.wind.speed,
                                humidity = forecast.main.humidity,
                                icon = iconFileName
                            )


                            // Insertar en la base de datos utilizando coroutines
                            CoroutineScope(Dispatchers.IO).launch {
                                dao.insert(forecastEntry)

                                // Registro de un mensaje de log después de insertar en la base de datos
                                Log.d("Forecast", "Datos del pronóstico insertados en la base de datos: Date: $formattedDate, Temperature: $temp, Wind Speed: $wind, Humidity: $humidity, Icon: $iconFileName")
                            }

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

                    val linearLayout =
                        findViewById<LinearLayout>(R.id.linearLayout_forecast)
                    linearLayout.removeAllViews()

                    // Intenta obtener los últimos 5 datos del pronóstico del clima de la base de datos
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = AppDatabase.getDatabase(applicationContext).forecastDao()
                        val savedForecastList = dao.getAllForecast()

                        val lastFiveForecast = if (savedForecastList.size >= 5) {
                            savedForecastList.subList(savedForecastList.size - 5, savedForecastList.size)
                        } else {
                            savedForecastList
                        }

                        if (lastFiveForecast.isNotEmpty()) {
                            // Si se encuentran datos del pronóstico del clima en la base de datos, crea y muestra las vistas
                            withContext(Dispatchers.Main) {
                                lastFiveForecast.forEach { forecast ->
                                    val formattedDate = forecast.date
                                    val iconFileName = forecast.icon
                                    val temp = "${forecast.temp}°"
                                    val wind = "${forecast.windSpeed} m/s"
                                    val humidity = "${forecast.humidity}%"

                                    val forecastLayout = LinearLayout(this@MainActivity)
                                    val marginInPixels = (10 * resources.displayMetrics.density).toInt()
                                    val layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    layoutParams.setMargins(20, 0, 20, marginInPixels)
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
                            // Si no se encuentran datos del pronóstico del clima en la base de datos, muestra un mensaje de error
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    applicationContext,
                                    "No se encontraron datos de pronóstico del clima en la base de datos",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

            })
    }

    private fun fetchForecastDay(lat: Double, lon: Double) {
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

                            val linearLayout =
                                findViewById<LinearLayout>(R.id.linearLayout_forecastDAY)
                            val chart =
                                findViewById<LineChart>(R.id.chartForecast)
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

    private fun deleteDatabase() {
        val context = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            context.deleteDatabase("weather_database")
        }
    }

}