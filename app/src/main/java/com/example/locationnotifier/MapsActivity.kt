package com.example.locationnotifier

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*

/*
MapsActivity to initialize a Google map and enable Goeofencing
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var marker: Marker
    private lateinit var locationSharedPref: SharedPreferences
    private var circle: Circle? = null
    private val REQUEST_PERMISSION_LOCATION = 1
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        geofencingClient = LocationServices.getGeofencingClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationSharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        showProgressBar()
        mMap = googleMap
        //The marker is set to Bay Farm, San Fransisco, California as per the figma designs
        val bayFarm = LatLng(37.7749, -122.4194)
        marker = mMap.addMarker(
            MarkerOptions()
                .position(bayFarm)
                .title("San Fransisco, California")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_image))
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLng(bayFarm))
        enableMyLocation()
        hideProgressBar()

        if (locationSharedPref.getBoolean(LOCATION_AVAILABLE, false)) {
            showProgressBar()
            val lat = locationSharedPref.getFloat(LATITUDE, 37.7749f).toDouble()
            val long = locationSharedPref.getFloat(LONGITUDE, -122.4194f).toDouble()
            val radius = locationSharedPref.getFloat(RADIUS_OF_GEOFENCE, 500f).toDouble()
            val location = LatLng(lat, long)

            setMarker(location)
            drawCircle(location, radius)
            val address: String = getAddress(location)
            text_address.text = address
            edit_lat_long.setText("$lat,$long")
            edit_radius.setText("$radius")
            hideProgressBar()
        }

        but_submit.setOnClickListener {
            if (validate()) {
                showProgressBar()
                if (circle != null) {
                    circle?.isVisible = false
                }
                val latLong = edit_lat_long.text.toString().split(",")
                val lat = latLong[0].toDouble()
                val long = latLong[1].toDouble()
                val location = LatLng(lat, long)
                val radius = edit_radius.text.toString().toDouble()
                setMarker(location)
                drawCircle(location, radius)

                val address: String = getAddress(location)
                text_address.text = address
                hideProgressBar()

                addGeofences(location, radius.toFloat())
                val editor = locationSharedPref.edit()
                editor.putFloat(LATITUDE, lat.toFloat())
                editor.putFloat(LONGITUDE, long.toFloat())
                editor.putFloat(RADIUS_OF_GEOFENCE, radius.toFloat())
                editor.putBoolean(LOCATION_AVAILABLE, true)
                editor.apply()
            }
        }
    }

    /*
    Function to add geofences in the map to a location of LatLang
     */
    private fun addGeofences(location: LatLng, radius: Float) {
        val geofenceingRequest = getGeofencingRequest(location, radius)
        geofencingClient.addGeofences(geofenceingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(
                    this@MapsActivity,
                    "The Geofence has been successfully added!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            addOnFailureListener {
                Toast.makeText(
                    this@MapsActivity,
                    "The Geofence failed to be added!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /*
    A function that returns a GeoFencingRequest after building it
     */
    private fun getGeofencingRequest(latlang: LatLng, radius: Float): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf(buildGeofence(latlang, radius)))
        }.build()
    }

    /*
    A function that returns a Geofence
     */
    private fun buildGeofence(latlang: LatLng, radius: Float): Geofence {
        return Geofence.Builder()
            .setRequestId("location")
            .setCircularRegion(
                latlang.latitude,
                latlang.longitude,
                radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    /*
    A Boolean function to check the validation of both the Latitude, Longitude and Radius
     */
    private fun validate() = (validateLatLong() && validateRadius())

    /*
    A Boolean function to check the format validation of Latitude and Longitude
     */
    private fun validateLatLong(): Boolean {
        val regex = Regex("[-]?\\d[.\\d]*,[-]?\\d+[.\\d]*")
        if (edit_lat_long.text.toString() matches regex) {
            val latLong = edit_lat_long.text.toString().split(",")
            val latitude = latLong[0].trim()
            val longitude = latLong[1].trim()
            val lat = latitude.toDouble()
            val long = longitude.toDouble()
            if (!validateRangeLatLong(lat, long)) {
                edit_lat_long.error = "Enter a proper Latitude(-90,90) and Longitude(-180,180)"
                edit_lat_long.requestFocus()
                return false
            } else {
                return true
            }
        } else {
            edit_lat_long.error = "Enter a proper Latitude and Longitude ex: 37.77, -122.41 "
            edit_lat_long.requestFocus()
            return false
        }
    }

    /*
    A Boolean function to check if the radius is empty or not
     */
    private fun validateRadius(): Boolean {
        return if (edit_radius.text.toString().isBlank()) {
            edit_radius.error = "Enter a proper radius"
            edit_radius.requestFocus()
            false
        } else {
            true
        }
    }

    /*
    A Boolean function to check the range of Latitude and Longitude. This function is used in validateLatLong
     */
    private fun validateRangeLatLong(lat: Double, lng: Double): Boolean {
        if (lat < -90 || lat > 90) {
            return false
        } else if (lng < -180 || lng > 180) {
            return false
        }
        return true
    }

    /*
    Function to set the marker on the location specified
     */
    private fun setMarker(location: LatLng) {
        marker.position = location
        marker.title = "Marker in ${location.latitude} , ${location.longitude}"

        mMap.moveCamera(CameraUpdateFactory.newLatLng(location))
    }

    /*
    Function to draw a circle on the location specified of the radius mentioned
     */
    private fun drawCircle(location: LatLng, radius: Double) {
        val stroke = 6f
        val zoom = 14.0f
        val circleOptions = CircleOptions()
            .center(location)
            .radius(radius) // In meters
            .strokeWidth(stroke)
            .strokeColor(Color.YELLOW)
            .fillColor(Color.argb(128, 255, 0, 0))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoom))

        circle = mMap.addCircle(circleOptions)
        circle?.isVisible = true
    }

    /*
    Function that takes a location of LatLang and returns an Address of String
     */
    private fun getAddress(location: LatLng): String {
        var address = ""
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList: List<Address>? =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            address = if (addressList != null) {
                val returnedAddress: Address = addressList[0]
                val city: String = returnedAddress.locality
                val state: String = returnedAddress.adminArea
                val knownName: String = returnedAddress.featureName

                "$knownName, $city, $state "
            } else {
                "No Address!"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            address = "Can't get this Address"
        }
        return address
    }

    /*
    A boolean function to check if all the permissions required are granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*
    A function to enable the Location of the device
     */
    private fun enableMyLocation() {
        if (allPermissionsGranted()) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_PERMISSION_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(
                        this,
                        R.string.location_permission_not_granted,
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri: Uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                return
            }
        }
    }

    private fun showProgressBar() {
        but_submit.isEnabled = false
        circular_progress.isVisible = true
    }

    private fun hideProgressBar() {
        but_submit.isEnabled = true
        circular_progress.isVisible = false
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT = "Geofence_Event"
        internal lateinit var notificationManager: NotificationManager
        private const val SHARED_PREF_NAME = "location_shared_pref"
        private const val LATITUDE = "latitude"
        private const val LONGITUDE = "longitude"
        private const val RADIUS_OF_GEOFENCE = "radius_of_geofence"
        private const val LOCATION_AVAILABLE = "is_location_available"
    }
}