package com.example.recycleme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ScannerResultsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScannerResultsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var data: String? = null
    private var maxPos: String? = null
    lateinit var btnScanAgain: Button
    lateinit var locateBinBtn: Button
    lateinit var RecycleLocationTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            data = it.getString("data")
            maxPos = it.getString("maxPos")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_scanner_results, container, false)

//        val data = arguments?.getString("data")
//        val maxPos = arguments?.getString("maxPos")

        val tvResult = view.findViewById<TextView>(R.id.tvResult)
        val itemCategory = view.findViewById<TextView>(R.id.itemCategory)

        RecycleLocationTxt = view.findViewById<TextView>(R.id.RecycleLocationTxt)
        locateBinBtn = view.findViewById<Button>(R.id.RecycleLocationBtn)

        val recycleImg = view.findViewById<ImageView>(R.id.imageView)
        val disposal_stepsText = view.findViewById<TextView>(R.id.disposal_stepsText)
        val disposal_stepsImg = view.findViewById<ImageView>(R.id.disposal_stepsImg)

        println(data)
        println(maxPos)

        if (maxPos != null) {
            tvResult.text = "$maxPos".split(" - ")[1]
            itemCategory.text = "$maxPos".split(" - ")[0]

            if(tvResult.text == "Recyclable"){
                tvResult.setTextColor(resources.getColor(R.color.bluebin))

                if(itemCategory.text == "Ewaste") {
                    RecycleLocationTxt.visibility = View.GONE
                    disposal_stepsText.visibility = View.GONE
                    disposal_stepsImg.visibility = View.GONE

                    recycleImg.setImageResource(R.drawable.alba_ewaste_bin)

                    locateBinBtn.setOnClickListener {
                        // Add your click logic here
                        val fragmentManager = requireActivity().supportFragmentManager
                        val transaction = fragmentManager.beginTransaction()
                        transaction.replace(R.id.container, MapFragment())
                        transaction.addToBackStack(null) // If you want to add this transaction to the back stack
                        transaction.commit()
                    }
                }
                else {
                    if (itemCategory.text == "Paper") {
                        disposal_stepsImg.setImageResource(R.drawable.disposalsteps_paper)
                        disposal_stepsImg.layoutParams.width = 1250
                        disposal_stepsImg.layoutParams.height = 600
                    }
                    locateBinBtn.visibility = View.GONE
                    RecycleLocationTxt.text = Html.fromHtml("<font color='#0000FF'>Bloobin</font> can be found below each HDB blocks.")
                }
            } else {
                tvResult.setTextColor(resources.getColor(R.color.red))
                recycleImg.setImageResource(R.drawable.trash_bin)
                disposal_stepsText.visibility = View.GONE
                disposal_stepsImg.visibility = View.GONE

                locateBinBtn.visibility = View.GONE
                RecycleLocationTxt.text = Html.fromHtml("This item <font color='#D63D3D'>cannot be recycled</font>. Please dispose it at the nearest <font color='#D63D3D'>trash bin</font>.")
            }
        } else {
            tvResult.text = "Max Position not available"
            itemCategory.text = "NIL"
        }

        btnScanAgain = view.findViewById(R.id.btnScanAgain)

        btnScanAgain.setOnClickListener {
            // Add your click logic here
            val fragmentManager = requireActivity().supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.container, ScannerFragment())
            transaction.addToBackStack(null) // If you want to add this transaction to the back stack
            transaction.commit()
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ScannerResultsFragment.
         */
        fun newInstance(data: String, maxPos: String): ScannerResultsFragment {
            val fragment = ScannerResultsFragment()
            val args = Bundle()
            args.putString("data", data)
            args.putString("maxPos", maxPos)
            fragment.arguments = args
            return fragment
        }
    }
}