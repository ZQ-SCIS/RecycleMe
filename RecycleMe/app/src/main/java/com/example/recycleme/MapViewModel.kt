package com.example.recycleme

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.recycleme.api.OneMapApi
import com.example.recycleme.api.RetrofitHelper
import com.google.android.gms.maps.GoogleMap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.util.Scanner
import kotlin.math.*

class MapViewModel : ViewModel() {
    val locations = MutableLiveData<ArrayList<HashMap<String, String>>>()
    var allLocations = ArrayList<HashMap<String, String>>()
    var token = HashMap<String, String>()
    val creds = HashMap<String, String>()
    var lat: Double = 0.0
    var lng: Double = 0.0

    fun init(context: Context, latitude: Double, longitude: Double){
        lat = latitude
        lng = longitude
        checkToken(context)
        readFromJSON(context, "one_map_bins")
    }


    private fun getNearbyBinLocations(context: Context, token: String){
        val extents = getExtentsFromCoordinates(lat, lng)
        val theme = "ewaste"
        val api = RetrofitHelper.getInstance().create(OneMapApi::class.java)
        api.getAllEWasteBinLocationsExtent(token, theme, extents).enqueue(object : Callback<HashMap<String, ArrayList<HashMap<String, String>>>> {
            override fun onResponse(
                call: Call<HashMap<String, ArrayList<HashMap<String, String>>>>,
                response: Response<HashMap<String, ArrayList<HashMap<String, String>>>>
            ) {
                println("response.body(): " + response.body())
                if(response.body() != null){
                    var locationsResp = response.body()!!.get("SrchResults") as ArrayList<HashMap<String, String>>
                    // Check if there is at least one element before removing
                    // Remove the first element which is the overview of results, not location data
                    if (locationsResp.isNotEmpty()) {
                        locationsResp.removeAt(0)
                    }
                    locations.postValue(locationsResp)
                }

            }

            override fun onFailure(
                call: Call<HashMap<String, ArrayList<HashMap<String, String>>>>,
                t: Throwable
            ) {
                println("error fetching api, reading from json")
                println(t.message)
                throw t
            }

        })

    }

    // helper method to read from json file for all bin locations
    private fun readFromJSON(context: Context, fileName: String) {
        val locationsArr = ArrayList<HashMap<String, String>>()

        try {
            // Open the JSON file from res/raw
            val resourceId = context.resources.getIdentifier(fileName, "raw", context.packageName)
            val inputStream: InputStream = context.resources.openRawResource(resourceId)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Read the JSON content
            val jsonContent = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                jsonContent.append(line)
            }

            // Parse JSON using Gson
            val gson = Gson()
            val type = object : TypeToken<Map<String, List<HashMap<String, String>>>>() {}.type
            val jsonData: Map<String, List<HashMap<String, String>>> = gson.fromJson(jsonContent.toString(), type)

            // Get the list of objects under "SrchResults"
            val srchResults = jsonData["SrchResults"]

            // Check if "SrchResults" is present and not empty
            if (srchResults != null && srchResults.isNotEmpty()) {
                // Filter objects based on keys
                for (result in srchResults) {
                    val filteredObject = result.filterKeys { key ->
                        setOf("NAME", "DESCRIPTION", "ADDRESSBUILDINGNAME", "ADDRESSPOSTALCODE", "ADDRESSSTREETNAME", "HYPERLINK", "Type", "LatLng", "ICON_NAME").contains(key)
                    }

                    // Check if the filtered object has all the required keys
                    if (filteredObject.size == 9) {
                        locationsArr.add(filteredObject as HashMap<String, String>)
                    }
                }
            }

            // Close the stream
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check if there is at least one element before removing
        // Remove the first element which is the overview of results, not location data
        if (locationsArr.isNotEmpty()) {
            locationsArr.removeAt(0)
        }
        allLocations = locationsArr
    }

    // get file from device directory that contains token details
    private fun checkToken(context: Context) {
        val credentialsFileName = "oneMapCredentials.txt"
        val tokenFileName = "oneMapToken.txt"

        // Check if the file exists
        val credsFile = File(context.filesDir, credentialsFileName)
        val tokenFile = File(context.filesDir, tokenFileName)

        // create a credentials file if it doesnt exist
        if (!credsFile.exists()) {
            // If the file doesn't exist, create it
            credsFile.createNewFile()
            val outStream = credsFile.writer()
            outStream.write("email\tjiayi.fok.2020@scis.smu.edu.sg\n")
            outStream.write("password\tChickenr1cewithegg!")
            creds["email"] = "jiayi.fok.2020@scis.smu.edu.sg"
            creds["password"] = "Chickenr1cewithegg!"
            outStream.close()
            getNewToken(context)
        }
        // else read from it and assign the credentials
        else{
            val scanner = Scanner(credsFile)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine().split("\t")
                creds[line[0]] = line[1]
            }
            val scanner2 = Scanner(tokenFile)
            while (scanner2.hasNextLine()) {
                val line = scanner2.nextLine().split("\t")
                token[line[0]] = line[1]
            }
            // get new token if token has expired
            if(token["expiry_timestamp"]!!.toLong() * 1000 < System.currentTimeMillis()){
                getNewToken(context)
            }

            scanner.close()
            getNearbyBinLocations(context, token["access_token"].toString())
        }

    }

    // helper function to get new token from One Map API
    private fun getNewToken(context: Context) {
        // check if there is a token file available
        val tokenFileName = "oneMapToken.txt"
        val tokenFile = File(context.filesDir, tokenFileName)
        if (!tokenFile.exists()) {
            tokenFile.createNewFile()
        }
        // call api for new token and create token file
        val api = RetrofitHelper.getInstance().create(OneMapApi::class.java)
        api.refreshToken(creds).enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>>,
                response: Response<HashMap<String, String>>
            ) {
                if(response.body() != null){
                    token["access_token"] = response.body()!!["access_token"].toString()
                    token["expiry_timestamp"] = response.body()!!["expiry_timestamp"].toString()

                    val outStream = tokenFile.writer()
                    outStream.write("access_token\t${token["access_token"]}\n")
                    outStream.write("expiry_timestamp\t${token["expiry_timestamp"]}")
                    outStream.close()
                    getNearbyBinLocations(context, token["access_token"].toString())

                }
            }
            override fun onFailure(
                call: Call<HashMap<String, String>>,
                t: Throwable
            ) {
                println(t.message)
                println("Error getting new token")
            }

        })

    }

    private fun getExtentsFromCoordinates(latitude: Double, longitude: Double): String{

        var minLat = latitude-0.005
        var minLng = longitude-0.005
        var maxLat = latitude+0.005
        var maxLng = longitude+0.005

        return "$minLat,$minLng,$maxLat,$maxLng"
    }
}