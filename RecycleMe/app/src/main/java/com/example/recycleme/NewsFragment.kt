package com.example.recycleme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import java.util.Scanner
class NewsFragment : Fragment() {
    private var news = HashMap<String, String>()
    private var images = HashMap<String, String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val data = arguments?.getString("data_key")
        val view = inflater.inflate(R.layout.fragment_news, container, false)
        val scanner = Scanner(requireActivity().openFileInput("scraped_data.txt"))
        readFile(scanner)
        val title = view.findViewById<TextView>(R.id.newsTitle)
        val body = view.findViewById<TextView>(R.id.body)
        val image = view.findViewById<ImageView>(R.id.image)
        val imageUrl = images[data]
        title.text = data
        body.text = news[data]
        if (!imageUrl!!.isNullOrBlank()){
            Picasso.get().load(imageUrl).into(image)
        }else{
            Picasso.get().load("https://www.econlib.org/wp-content/uploads/2018/05/recycling-2.jpg").into(image)
        }
        return view
    }

    private fun readFile(scanner: Scanner){
        while(scanner.hasNextLine()){
            val line = scanner.nextLine()
            var pieces = line.split("\t\t\t\t")
            news[pieces[0]] = pieces[1].replace("||||", "\n\n")
            images[pieces[0]] = pieces[2]
        }
    }

    companion object {
        fun newInstance(data: String): NewsFragment {
            val fragment = NewsFragment()
            val args = Bundle()
            args.putString("data_key", data)
            fragment.arguments = args
            return fragment
        }
    }
}