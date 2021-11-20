package com.enigma.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings;
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.enigma.weatherapp.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import android.os.Looper
import com.google.android.gms.location.LocationResult
import android.location.LocationManager
import android.content.Intent
import android.widget.Toast
import android.location.Geocoder
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import com.enigma.weatherapp.model.Status
import com.enigma.weatherapp.model.WeatherResponse
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val locationPermissionRequestCode = 100
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        startWeatherObserver()
        citySearchEditText.doOnTextChanged { text, start, before, count ->
            if (count > 2)
                mainViewModel.getCurrentLocationWeather(text.toString())
        }
        citySearchEditText.setOnEditorActionListener(object :
            TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow((currentFocus ?: View(baseContext)).windowToken, 0)
                    citySearchEditText.clearFocus()
                    return true;
                }
                return false;
            }
        })
    }

    private fun startWeatherObserver() {
        mainViewModel.weatherResponseLiveData.observe(this, {
            when (it) {
                is Status.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is Status.Success -> {
                    progressBar.visibility = View.GONE
                    showData(it.data)
                }
                is Status.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, it.e, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun showData(data: WeatherResponse) {
        Picasso.get()
            .load("https://openweathermap.org/img/wn/${data.weather[0].icon}.png")
            .resize(80, 80)
            .centerCrop()
            .into(condIcon)
        cityNameTextView.text = String.format(getString(R.string.city_name), data.name)
        tempTextView.text = String.format(getString(R.string.temp), data.main.temp.toString())
        pressLabTextView.text =
            String.format(getString(R.string.pressure_symbole), data.main.pressure.toString())
        feelLikeTextView.text =
            String.format(getString(R.string.temp_feel_like), data.main.feelsLike.toString())
        humLabTextView.text =
            String.format(getString(R.string.humidity), data.main.humidity.toString())
        windSpeed.text = String.format(getString(R.string.wind_speed), data.wind.speed.toString())
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        if (isLocationPermissionGranted()) {
            if (isLocationEnabled()) {
                val getLocation = fusedLocationProviderClient.lastLocation
                getLocation.addOnSuccessListener {
                    if (it != null) {
                        currentLocation = it
                        getCurrentCityName()
                    } else {
                        requestNewLocationData()
                    }
                }
            } else {
                Toast.makeText(this, "Please turn on your location...", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                currentLocation = locationResult.lastLocation
                getCurrentCityName()
            }
        }
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5
            fastestInterval = 0
            numUpdates = 1
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()!!
        )

    }


    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                locationPermissionRequestCode
            )
            false
        } else {
            true
        }
    }

    fun getCurrentCityName() {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses =
            currentLocation?.latitude?.let { latitude ->
                currentLocation?.longitude?.let { longitude ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude, 1
                    )
                }
            }
        citySearchEditText.setText(addresses?.get(0)?.adminArea)
        addresses?.get(0)?.adminArea?.let { mainViewModel.getCurrentLocationWeather(it) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            locationPermissionRequestCode -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLocation()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        fetchLocation()
    }
}