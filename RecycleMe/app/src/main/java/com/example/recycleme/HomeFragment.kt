package com.example.recycleme
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.File


class HomeFragment : Fragment() {
    private var titles = ArrayList<String>()
    private lateinit var myAdapter : ArrayAdapter<String>
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var newsLv: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(com.example.recycleme.R.layout.fragment_home, container, false)
        loadingProgressBar = view.findViewById<ProgressBar>(R.id.progressBar3)
        newsLv = view.findViewById<ListView>(com.example.recycleme.R.id.newsLv)
        loadingProgressBar.visibility = View.VISIBLE
        newsLv.visibility = View.GONE
        scrapeWebsite(view)
        return view
    }

    private fun scrapeWebsite(view: View) {

        val fileName = "scraped_data.txt"
        val file = File(requireActivity().filesDir, fileName)
        performScrapingAndSaveToFile(view, file)
    }

    private fun performScrapingAndSaveToFile(view: View, file: File) {
        Thread {
            try {
                val url = "https://www.channelnewsasia.com/topic/recycling"
                val doc: Document = Jsoup.connect(url).get()
                val headings = doc.select(".list-object__heading a").slice(0..10)

                val scrapedData = StringBuilder()

                for (heading in headings) {
                    val indivUrl = "https://www.channelnewsasia.com" + heading.attr("href")
                    val indivDoc: Document = Jsoup.connect(indivUrl).get()
                    val title = indivDoc.select(".h1--page-title").text()
                    val img = indivDoc.select(".layout__region section figure picture img").attr("src")
                    if (title.isNotBlank()) {
                        titles.add(title)
                        val paragraphs = indivDoc.select(".content-wrapper .text .text-long p")
                        val body = paragraphs.joinToString("||||") { it.text() }
                        scrapedData.append("$title\t\t\t\t$body\t\t\t\t$img\n")
                    }
                }

                file.writeText(scrapedData.toString())

                activity?.runOnUiThread {
                    processDataAndUpdateUI(scrapedData.toString(), view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun processDataAndUpdateUI(data: String, view: View) {
        titles.clear()
        data.split("\n").forEach {
            val parts = it.split("\t\t\t\t")
            if (parts.size >= 2) {
                titles.add(parts[0])
            }
        }

        myAdapter = NewsAdapter(requireContext(), titles)
        newsLv.adapter = myAdapter
        loadingProgressBar.visibility = View.GONE
        newsLv.visibility = View.VISIBLE

        newsLv.setOnItemClickListener { _, _, index, _ ->
            val selectedItemText = titles[index]
            val newsFragment = NewsFragment.newInstance(selectedItemText)
            loadFragment(newsFragment)
        }
    }


    private  fun loadFragment(fragment: Fragment){
        val transaction = requireFragmentManager().beginTransaction()
        transaction.replace(com.example.recycleme.R.id.container,fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

}