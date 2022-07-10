package com.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import kotlinx.coroutines.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {
    private val dispatcher = newFixedThreadPoolContext(2, "IO")
    private val factory = DocumentBuilderFactory.newInstance()

    private val feeds = listOf(
        "https://www.npr.org/rss/rss.php?id=1001",
        "http://rss.donga.com/total.xml",
        "http://rss.hankooki.com/sports/sp00_list.xml",
        "asdifo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalScope.launch(dispatcher) {
            asyncLoadNews()
        }
    }

    private fun asyncLoadNews() = GlobalScope.launch {
        val requests = mutableListOf<Deferred<List<String>>>()
        feeds.mapTo(requests) { asyncFetchHeadlines(it, dispatcher) }
        requests.forEach { it.join() }

        val headlines = requests
            .filter { !it.isCancelled }
            .flatMap { it.getCompleted() }

        val failed = requests.filter { it.isCancelled }.size
        val newsCount = findViewById<TextView>(R.id.newsCount)
        val warnings = findViewById<TextView>(R.id.warnings)
        val obtained = requests.size - failed

        launch(Dispatchers.Main) {
            newsCount.text = "Found ${headlines.size} News in ${obtained} feeds"

            if (failed > 0) {
                warnings.text = "Failed to fetch $failed feeds"
            }
        }
    }

    private fun asyncFetchHeadlines(feed: String, dispatcher: CoroutineDispatcher) = GlobalScope.async(dispatcher) {
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse(feed)
        val news = xml.getElementsByTagName("channel").item(0)

        (0 until news.childNodes.length)
            .asSequence()
            .map { news.childNodes.item(it) }
            .filter { Node.ELEMENT_NODE == it.nodeType }
            .map { it as Element }
            .filter { "item" == it.tagName }
            .map {
                it.getElementsByTagName("title").item(0).textContent
            }
            .toList()
    }
}