package com.example.recycleme.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OneMapApi {

// returned format:
// {
//    "SrchResults": [
//    { // overview of results returned
//        "FeatCount": 718,
//        "Theme_Name": "E-waste Recycling",
//        "Category": "Environment",
//        "Owner": "NATIONAL ENVIRONMENT AGENCY",
//        "DateTime": "2022-06-02T13:10:31+00:00",
//        "Published_Date": "2014-05-16T00:00:00+00:00",
//        "Formatted_DateTime": "02/06/2022",
//        "Formatted_Published_Date": "16/05/2014"
//    },
//    {  // what we will be using
//        "NAME": "NEA Producer Responsibility Scheme - ALBA E-waste Recycling Programme",
//        "DESCRIPTION": "Bin collection; E-waste accepted: ICT equipment, Batteries and Lamps only",
//        "ADDRESSBUILDINGNAME": "ALEXANDRA RETAIL CENTRE (ARC)",
//        "ADDRESSPOSTALCODE": "119963",
//        "ADDRESSSTREETNAME": "460 ALEXANDRA ROAD, ALEXANDRA RETAIL CENTRE, LEVEL 1, BESIDE WATSON UNIT NEAR NURSING ROOM",
//        "HYPERLINK": "https://alba-ewaste.sg; https://go.gov.sg/e-waste",
//        "Type": "Point",
//        "LatLng": "1.27434294717938,103.801500800092",
//        "ICON_NAME": "ewaste.jpg"
//    },
//    ...
    // method to get ALL bins
    @GET("/api/public/themesvc/retrieveTheme?queryName=ewaste")
    fun getAllEWasteBinLocations(@Header("Authorization") token: String): Call<HashMap<String, ArrayList<HashMap<String, String>>>>

    // method to get bins within specified area
    @GET("/api/public/themesvc/retrieveTheme")
    fun getAllEWasteBinLocationsExtent(@Header("Authorization") token: String, @Query ("queryName") theme: String, @Query("extents") extents: String): Call<HashMap<String, ArrayList<HashMap<String, String>>>>

    // method to refresh auth token
    @POST("/api/auth/post/getToken")
    fun refreshToken(@Body user: HashMap<String, String>): Call<HashMap<String, String>>
}