package com.example.recycleme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.substring
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.recycleme.api.OneMapApi
import com.example.recycleme.api.RetrofitHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale


class MapFragment : Fragment(), GoogleMap.OnCameraIdleListener {
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var latLngArr = ArrayList<String>()
    private lateinit var mapViewModel: MapViewModel

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionId = 2
    private lateinit var googleMap: GoogleMap
    private lateinit var supportMapFragment: SupportMapFragment
    private lateinit var binsNearYouListFragment: BinsNearYouListFragment
    private val markers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        // get list of all ewaste recycling bin locations
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getLocation()


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        mapViewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // get nearby bins
        mapViewModel.locations.observe(viewLifecycleOwner) { updatedLocations ->
            for (locationObj in mapViewModel.allLocations as ArrayList<HashMap<String, String>>) {
                var oneLocation = locationObj.toMap()
                for (oneNearbyLocationObj in updatedLocations as ArrayList<HashMap<String, String>>) {
                    var oneNearbyLocation = oneNearbyLocationObj.toMap()
                    var lat = oneNearbyLocation["LatLng"]!!.split(",")[0].toDouble()
                    var long = oneNearbyLocation["LatLng"]!!.split(",")[1].toDouble()
                    var buildingName = oneNearbyLocation["ADDRESSBUILDINGNAME"]
                    var bldgNameSubString = if (buildingName!!.length > 30) buildingName.substring(
                        0,
                        31
                    ) else buildingName
                    val markerOptions = MarkerOptions()
                        .position(LatLng(lat, long))
                        .title(bldgNameSubString)
                    // if location has alr been saved, dont add another marker
                    if (::googleMap.isInitialized && !latLngArr.contains(oneNearbyLocation["LatLng"])) {
                        latLngArr.add(oneNearbyLocation["LatLng"]!!)
                        // create marker for it
                        val marker = googleMap?.addMarker(markerOptions)
                        // save the nearby bins markers so that we can easily add them back
                        if (marker != null ){
                            markers.add(marker)
                        }
                        // Set the listener to check marker visibility when the camera is idle
                        googleMap.setOnCameraIdleListener(this)
                    }
                }
            }

        }

        val radioGroup = view.findViewById<RadioGroup>(R.id.filterRadioGroup)
        val nearbyRB = view.findViewById<RadioButton>(R.id.nearbyBinsRB)
        radioGroup.check(R.id.nearbyBinsRB)
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            // Handle radio button changes
            val selectedRadioButton: RadioButton = view.findViewById(checkedId)
            val selectedFilter: String = selectedRadioButton.text.toString()
            googleMap.clear()
            // add user's location marker
            val markerOptions = MarkerOptions()
                .position(LatLng(lat, lng))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .title("You are here")
            googleMap.addMarker(markerOptions)
            // if user selected all bins, loop thru all locations and re-add markers and listeners
            if(selectedFilter == "All bins"){
                for (locationObj in mapViewModel.allLocations) {
                    var oneLocation = locationObj.toMap()
                    var lat = oneLocation["LatLng"]!!.split(",")[0].toDouble()
                    var long = oneLocation["LatLng"]!!.split(",")[1].toDouble()
                    var buildingName = oneLocation["ADDRESSBUILDINGNAME"]
                    var bldgNameSubString = if (buildingName!!.length > 30) buildingName.substring(
                        0,
                        31
                    ) else buildingName
                    val markerOptions2 = MarkerOptions()
                        .position(LatLng(lat, long))
                        .title(bldgNameSubString)
                    googleMap.addMarker(markerOptions2)
                    googleMap.setOnCameraIdleListener(this)
                }

            }
            // user nearby bins is selected, loop thru marker objects and re-add markers and listeners
            else{
                for (marker in markers){
                    val markerOptions2 = MarkerOptions()
                        .position(marker.position)
                        .title(marker.title)
                    googleMap.addMarker(markerOptions2)
                    googleMap.setOnCameraIdleListener(this)

                }
            }
        }

        return view
    }

    // start of helper methods to get user's current location
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation() {
        var dblArr: DoubleArray? = null;
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val list: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        println("Latitude ${list?.get(0)?.latitude}" +
                                " Longitude: ${list?.get(0)?.longitude}")
                        if(list!![0]!!.countryCode != "US"){
                            // if not using emulator, get detected location
                            lat = list?.get(0)?.latitude!!
                            lng = list?.get(0)?.longitude!!
                        }
                        else{
                            // default to SMU SCIS's location if using emulator
                            lat = 1.2976094999999999
                            lng = 103.8492318

                        }
                        mapViewModel.init(requireContext(), lat, lng)
                        initialiseUserLocationOnMap()

                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }

    }
    // end of helper methods to get user's current location


    
    fun focusOnMarker(lat: Double, lng :Double){
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f)
        )
    }


    override fun onCameraIdle() {
        // Get the current zoom level
        val currentZoom = googleMap.cameraPosition.zoom

        // Get the width and height of the MapFragment's view
        val mapFragmentWidth = supportMapFragment.view?.width ?: 0
        val mapFragmentHeight = supportMapFragment.view?.height ?: 0

        // Iterate through markers and set visibility based on zoom level
        markers.forEach { marker ->
            marker.isVisible = marker.isVisibleAtZoom(currentZoom, mapFragmentWidth, mapFragmentHeight)
        }
    }

    // helper function to check if marker is visible depending on current zoom level
    private fun Marker.isVisibleAtZoom(zoomLevel: Float, mapFragmentWidth: Int, mapFragmentHeight: Int): Boolean {
        // Adjust this threshold value based on your preference
        val visibilityThreshold = 10f

        // Get the marker's position
        val markerPosition = position

        // Convert marker position to a Point on the screen
        val screenPosition = googleMap.projection.toScreenLocation(markerPosition)

        // Calculate the meters per pixel at the current zoom level
        val metersPerPixel = (156543.03392 * Math.cos(markerPosition.latitude * Math.PI / 180) / Math.pow(2.0, zoomLevel.toDouble()))

        // Calculate the visibility threshold in pixels
        val visibilityThresholdPx = visibilityThreshold / metersPerPixel

        // Check if the marker is within the visible region on the screen
        return screenPosition.x >= -visibilityThresholdPx &&
                screenPosition.x <= mapFragmentWidth + visibilityThresholdPx &&
                screenPosition.y >= -visibilityThresholdPx &&
                screenPosition.y <= mapFragmentHeight + visibilityThresholdPx
    }

    // map to add user's current location marker after getting location from device
    private fun initialiseUserLocationOnMap(){
        val binsNearYouListFragment = BinsNearYouListFragment()

        // Begin a FragmentTransaction
        val transaction = childFragmentManager.beginTransaction()

        // Add the fragment to the container (assuming you have a FrameLayout with the ID container in your activity layout)
        transaction.add(R.id.fragmentContainerView, binsNearYouListFragment, "binsNearYouListFragmentTag")

        // Commit the transaction
        transaction.commit()

        supportMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        supportMapFragment.getMapAsync {
                gmap -> googleMap = gmap
            if (::googleMap.isInitialized) {
                println("googleMap is initialised")

                // Add blue marker to show user's location
                val markerOptions = MarkerOptions()
                    .position(LatLng(lat, lng))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .title("You are here")

                googleMap.addMarker(markerOptions)

                // Zoom in to the user's current location marker
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f)
                )

            }

        }
    }

}