package com.example.germeteo


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.germeteo.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import org.json.JSONObject

private val LOCATION_PERMISSION_REQUEST_CODE = 1001
const val API_KEY = "2200e00da5d445679a1161620241402"

class MainActivity : AppCompatActivity() {
    private lateinit var fLocationClient: FusedLocationProviderClient
    private lateinit var city: String
    private lateinit var bindingClass: ActivityMainBinding
    private lateinit var latit: String
    private lateinit var longit: String
    private lateinit var cityFromRequest: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
        bindingClass = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)


        bindingClass.button.setOnClickListener { //слушатель нажатий на кнопку
            city = bindingClass.editTextText.text.toString()
            getResult(city)
        }

        bindingClass.swipeLay.setOnRefreshListener { //слушатель свайпа для обновления данных

            runOnUiThread {

                getResult("$latit, $longit")
                bindingClass.swipeLay.isRefreshing = false
            }
        }

        bindingClass.geobutton.setOnClickListener {
            checkLocationPermission() //проверка прав на доступ к геолокаици
            checkLocation() //проверка включена ли геолокация и диалогое меню
        }
    }


    private fun checkLocationPermission() { //для проверки наличия разрешения
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) { //для проверки наличия разрешения
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //если отклонил доступ
            }
        }
    }

    private fun getResult(name: String) {
        val url = "http://api.weatherapi.com/v1/current.json?key=$API_KEY&q=$name&aqi=no"
        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(Request.Method.GET,
            url,
            { result ->
                val utf8Result = String(result.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                parseWeatherData(utf8Result)
            },
            {
                Log.d("MyLog", "Volley error $it")
            }
        )
        queue.add(stringRequest)
    }




    private fun parseWeatherData(result: String) = with(bindingClass) {
        val mainObject = JSONObject(result)
        val item = WeatherObj(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("temp_c"),
            mainObject.getJSONObject("current").getString("humidity"),
            mainObject.getJSONObject("current").getString("wind_kph"),
            mainObject.getJSONObject("current").getString("feelslike_c"),
            mainObject.getJSONObject("location").getString("lat"),
            mainObject.getJSONObject("location").getString("lon")
        )
        val tempo: String = item.temperature + "°C"
        textViewTemp.text = tempo  //temp
        textViewTemp.visibility = View.VISIBLE
        textViewCity.text = item.city //city
        textViewCity.visibility = View.VISIBLE
        val humo = item.humidity + "%"
        textViewHum.text = humo //humidity
        textViewHum.visibility = View.VISIBLE
        val feelslike: String = item.fellsLike + "°C"
        textViewFL.text = feelslike //feelsLike
        textViewFL.visibility = View.VISIBLE
        val windSpeed: String = String.format("%.1f",(item.windSpeed.toDouble() / 3.6)) + getString(R.string.km)
        textViewWP.text = windSpeed //windSpeed
        textViewWP.visibility = View.VISIBLE
        latit = item.latitude
        longit = item.longitude
        cityFromRequest = item.city

    }

    private fun isLocationEnabled(): Boolean { //проверка на включенность геолокации на телефоне
        val lm = this@MainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getLocation(): Unit = with(bindingClass) { //получение результата через геолокацию
        if (!isLocationEnabled()) {
            Toast.makeText(this@MainActivity, "Location disabled", Toast.LENGTH_SHORT).show()
            return
        }
        val ct = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, ct.token)
            .addOnCompleteListener {
                getResult("${it.result.latitude}, ${it.result.longitude}")
            }
    }

    private fun checkLP(): Boolean { //для проверки прав на геолокацию
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocation() { //проверка геолокации и вызов диалога
        if (isLocationEnabled()) {
            getLocation()
        } else if (checkLP() && !isLocationEnabled()) {
            DialogManager.locationSettingsDialog(
                this@MainActivity,
                object : DialogManager.Listener {
                    override fun onClick() {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                })
        } else {

        }
    }
}