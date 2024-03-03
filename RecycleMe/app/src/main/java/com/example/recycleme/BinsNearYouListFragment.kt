package com.example.recycleme

import android.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recycleme.databinding.FragmentBinsNearYouListBinding
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken

class BinsNearYouListFragment : Fragment() {
    private lateinit var binding: FragmentBinsNearYouListBinding
    private lateinit var mapViewModel: MapViewModel
    private var locations: ArrayList<HashMap<String, String>> = ArrayList()
    private var data: ArrayList<HashMap<String, String>> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_bins_near_you_list, container, false)

        // get locations from map fragment
        mapViewModel = ViewModelProvider(requireActivity()).get(MapViewModel::class.java)
        mapViewModel.locations.observe(viewLifecycleOwner) { updatedLocations ->
//            val view = requireView()

            // getting the recyclerview by its id
            val recyclerview = view.findViewById<RecyclerView>(R.id.locationListRV)

            // Update UI with the new location markers whenever location is updated
            updateList(updatedLocations)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        mapViewModel.init(requireContext())
    }

    // adapter for recyclerview
    class CustomAdapter(private val mList: ArrayList<HashMap<String, String>>,
    private val parentFragment: Fragment)
    : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        // create new views
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // inflates the card_view_design view
            // that is used to hold list item
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.location_list_item, parent, false)

            return ViewHolder(view)
        }

        // binds the list items to a view
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val oneLocation = mList[position]
            if(oneLocation.containsKey("LatLng")) {

                // sets the image to the imageview from our itemHolder class
                holder.imageView.setImageResource(R.drawable.directions)
                holder.imageView.setOnClickListener(){
                // Handle the click event here
                // For example, you can perform some action or start another activity
                    // get lat lng from tag
                    var latLng = oneLocation["LatLng"]?.split(",")
                    // call the method in map fragment to move the focus to the clicked location
                    val mapFragment = parentFragment as MapFragment
                    mapFragment.focusOnMarker(latLng!![0].toDouble(), latLng!![1].toDouble())
                }

                // sets the text to the textview from our itemHolder class
                holder.textView.text = if (oneLocation["ADDRESSSTREETNAME"]!!.length > 30) oneLocation["ADDRESSSTREETNAME"]!!.substring(0,31)+"..." else oneLocation["ADDRESSSTREETNAME"]
                holder.textView.tag = oneLocation["LatLng"]

                holder.itemView.setOnClickListener {
                    val inflater = parentFragment?.layoutInflater
                    val dialogView = inflater?.inflate(R.layout.pop, null)

                    val alertDialogBuilder = AlertDialog.Builder(parentFragment?.context)
                    alertDialogBuilder.setView(dialogView)
                    val alertDialog = alertDialogBuilder.create()

                    val latLng = oneLocation["LatLng"]
                    val description = oneLocation["DESCRIPTION"]
                    val buildingName = oneLocation["ADDRESSBUILDINGNAME"]
                    val address = oneLocation["ADDRESSSTREETNAME"]

                    val buildingNameView = dialogView?.findViewById<TextView>(R.id.buildingName)
                    buildingNameView?.text = "$buildingName"

                    val descriptionView = dialogView?.findViewById<TextView>(R.id.description2)
                    descriptionView?.text = "$description"

                    val addressView = dialogView?.findViewById<TextView>(R.id.address)
                    addressView?.text = "$address"

                    val closeButton = dialogView?.findViewById<ImageView>(R.id.closeButton)
                    closeButton?.setOnClickListener {
                        alertDialog?.dismiss()
                    }
                    alertDialog.show()

                }
            }
        }

        // return the number of the items in the list
        override fun getItemCount(): Int {
            return mList.size
        }

        // Holds the views for adding it to image and text
        class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageview)
            val textView: TextView = itemView.findViewById(R.id.locationTV)
        }
    }

    fun updateList(locationList: ArrayList<HashMap<String, String>>) {
        val view = requireView()

        // getting the recyclerview by its id
        val recyclerview = view.findViewById<RecyclerView>(R.id.locationListRV)

        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(context)

        // This will pass the ArrayList to our Adapter
        val adapter = CustomAdapter(locationList, requireParentFragment())

        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter
    }

    fun scrollToPosition(position: Int) {
        val view = requireView()

        // get the reference to the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.locationListRV)
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager?

        // scroll to the specified position with smooth animation
        layoutManager?.scrollToPosition(position)
    }
}