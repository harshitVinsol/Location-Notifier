package com.example.locationnotifier

import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
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
        val circleOptions = CircleOptions()
            .center(location)
            .radius(radius) // In meters
            .strokeWidth(6f)
            .strokeColor(Color.YELLOW)
            .fillColor(Color.argb(128, 255, 0, 0))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 14.0f))

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
}
