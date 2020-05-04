package com.example.locationnotifier

import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var marker: Marker
    private var circle: Circle? = null
    private var REQUEST_PERMISSION_LOCATION = 1
    private var REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)

    lateinit var geofencingClient: GeofencingClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //The marker is set to Bay Farm, San Fransisco, California
        val bayFarm = LatLng(37.7749, -122.4194)
        marker= mMap.addMarker(MarkerOptions()
            .position(bayFarm)
            .title("San Fransisco, California")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_image)))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(bayFarm))
        enableMyLocation()

        but_submit.setOnClickListener {
            if(validate()){
                but_submit.isEnabled = false

                if(circle != null){
                    circle?.isVisible = false
                }

                val latLong = edit_lat_long.text.toString().split(",")
                val lat= latLong[0].toDouble()
                val long = latLong[1].toDouble()
                val location = LatLng(lat, long)

                setMarker(location)
                drawCircle(location, edit_radius.text.toString().toDouble())

                val address: String = getAddress(location)
                text_address.text = address
                showNotification()

                but_submit.isEnabled = true
            }
        }
    }
    /*
    A Boolean function to check the validation of both the Latitude, Longitude and Radius
     */
    private fun validate() = (validateLatLong() && validateRadius())
    /*
    A Boolean function to check the format validation of Latitude and Longitude
     */
    private fun validateLatLong() : Boolean{
        val regex = Regex("[-]?\\d[.\\d]*,[-]?\\d+[.\\d]*")
        if(edit_lat_long.text.toString() matches regex){
            val latLong = edit_lat_long.text.toString().split(",")
            val latitude = latLong[0].trim()
            val longitude = latLong[1].trim()
            val lat= latitude.toDouble()
            val long = longitude.toDouble()
            if(!validateRangeLatLong(lat, long)){
                edit_lat_long.error= "Enter a proper Latitude(-90,90) and Longitude(-180,180)"
                edit_lat_long.requestFocus()
                return false
            }
            else{
                return true
            }
        }
        else{
            edit_lat_long.error= "Enter a proper Latitude and Longitude ex: 37.77, -122.41 "
            edit_lat_long.requestFocus()
            return false
        }
    }
    /*
    A Boolean function to check if the radius is empty or not
     */
    private fun validateRadius() : Boolean{
        if(edit_radius.text.toString().isBlank()){
            edit_radius.error = "Enter a proper radius"
            edit_radius.requestFocus()
            return false
        }
        else{
            return true
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
    private fun setMarker(location : LatLng){
        marker.position = location
        marker.title = "Marker in ${location.latitude} , ${location.longitude}"

        mMap.moveCamera(CameraUpdateFactory.newLatLng(location))
    }
    /*
    Function to draw a circle on the location specified of the radius mentioned
     */
    private fun drawCircle(location : LatLng, radius : Double){
        val stroke = 6f
        val zoom = 14.0f
        val circleOptions = CircleOptions()
            .center(location)
            .radius(radius) // In meters
            .strokeWidth(stroke)
            .strokeColor(Color.YELLOW)
            .fillColor(Color.argb(128, 255, 0, 0))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoom))

        circle= mMap.addCircle(circleOptions)
        circle?.isVisible = true
    }
    /*
    Function that takes a location of LatLang and returns an Address of String
     */
    private fun getAddress(location: LatLng) : String{
        var address = ""
        val geocoder = Geocoder(this, Locale.getDefault())
        try{
            val addressList : List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if(addressList != null){
                val returnedAddress: Address = addressList[0]
                val city: String = returnedAddress.locality
                val state: String = returnedAddress.adminArea
                val knownName: String = returnedAddress.featureName

                address = "$knownName, $city, $state "
            }
            else{
                address = "No Address!"
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            address = "Can't get this Address"
        }
        return address
    }

    private fun showNotification(){
        val builder = NotificationCompat.Builder(this, "personal_notification")
        builder.setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Demo Notification")
            .setContentText("You've entered in the location specified")
            .priority = NotificationCompat.PRIORITY_DEFAULT

        val notificationManagerCompat = NotificationManagerCompat.from(this)
        notificationManagerCompat.notify(1, builder.build())

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation(){
        if(allPermissionsGranted()){
            mMap.isMyLocationEnabled = true
        }
        else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSION_LOCATION)
        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(this,
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.activityMapsMain,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofenceForClue()
            }
        }
    }
}
