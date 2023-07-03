package wiktoria.debowska_151874

import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.measureTimeMillis

class EkranSynchronizacji : AppCompatActivity() {
    var liczbaGier=0
    var liczbaDodatkow=0
    var today=""
    var data=""
    var UserName=""
    var saveFinished: Boolean = false
    var downloadFinished: Boolean = false
    var expired: Boolean = false
    private val KEY_DATA = "KEY_DATA"
    public final lateinit var progressDialog:AlertDialog

    private inner class DataDownloader: AsyncTask<String, Int, String>(){
        var u: String = ""
        var name: String = ""
        var main: Boolean = false

        fun setData(adress: String, name: String, m:Boolean){
            this.u = adress
            this.name = name
            this.main = m
        }
        override fun onPreExecute() {
            super.onPreExecute()
            downloadFinished = false
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun doInBackground(vararg p0: String?): String {

            try {
                val url = URL(u)
                val connection = url.openConnection()
                connection.connect()
                val istream = connection.getInputStream()
                val content = istream.bufferedReader().use { it.readText() }
                val lenghtOfFile = content.length
                val isStream = url.openStream()
                val testDirectory = File("$filesDir/XML")
                if(!testDirectory.exists()){
                    testDirectory.mkdir()
                }
                val fos = FileOutputStream("$testDirectory/$name.xml")
                val data = ByteArray(1024)
                var count = 0
                var total:Long = 0
                var progress = 0
                count = isStream.read(data)
                while(count != -1){
                    total += count.toLong()
                    val progress_tmp = total.toInt()*100/lenghtOfFile
                    if(progress_tmp%10 == 0 && progress != progress_tmp){
                        progress = progress_tmp
                        publishProgress(progress)
                    }
                    fos.write(data, 0, count)
                    count = isStream.read(data)
                }
                isStream.close()
                fos.close()
            }catch (e: MalformedURLException){
                return "Zły URL"
            }catch (e: FileNotFoundException){
                return "Brak pilku"
            }catch (e: IOException){
                return "wyjątek IO"
            }
            while(!saveRetry(name, main)){
                if(!expired){
                    sleepyHead()
                }
                else{
                    runOnUiThread(Runnable() {
                        run() {
                            Toast.makeText(this@EkranSynchronizacji, "Error", Toast.LENGTH_LONG)
                        }
                    })
                    sleepyHead()
                    finish()
                }
            }
            downloadFinished = true
            return "success"
        }

    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_DATA,data)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ekran_synchronizacji)
        data=getIntent().getStringExtra("data")+""
        if(data==""){
            if (savedInstanceState != null) {
                data = savedInstanceState.getString(KEY_DATA,data).toString()
            }
        }
        val data1: TextView = findViewById(R.id.synchro)
        data1.setText(data)
    }
    fun porownanie_dat(a: String, b: String): Long {
        var spt = a.split('-')
        val a_year = (spt[2].toLong())
        val a_month = (spt[1].toLong())
        val a_day = (spt[0].toLong())
        spt = b.split('-')
        val b_year = (spt[2].toLong())
        val b_month = (spt[1].toLong())
        val b_day = (spt[0].toLong())
        val year_diff = b_year - a_year
        val month_diff = b_month - a_month
        val day_diff = b_day - a_day
        val wynik = year_diff * 365 + month_diff * 30 + day_diff
        return wynik
    }
    fun SynchronizujClick(v: View){
        saveFinished=false
        downloadFinished=false
        val data = getIntent().getStringExtra("data")+""
        UserName = getIntent().getStringExtra("username").toString()
        liczbaGier = Integer.parseInt(getIntent().getStringExtra("liczbaGier"))
        liczbaDodatkow = Integer.parseInt(getIntent().getStringExtra("liczbaDodatkow"))
        val c: Date = Calendar.getInstance().getTime()
        val df = android.icu.text.SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        today= df.format(c)
        if(porownanie_dat(today,data) == 0L){
            val message = AlertDialog.Builder(this@EkranSynchronizacji)
            message.setMessage("Dane są aktualne.\nNa pewno chcesz je zaktualizować?")                .setCancelable(false)
                .setPositiveButton("Tak") { dialog, id ->
                    dialog.dismiss()
                    synchro(UserName)
                }
                .setNegativeButton("Nie") { dialog, id ->
                    dialog.dismiss()
                    saveFinished=true
                    downloadFinished=true
                    today=data
                }
            val alert = message.create()
            alert.show()
        }
        else {
            synchro(UserName)
        }
    }

    fun synchro(user: String){
        val progressView: View = layoutInflater.inflate(R.layout.progress_popup, null)
        val dialogBuilder = AlertDialog.Builder(this@EkranSynchronizacji)
        dialogBuilder.setView(progressView)
        progressDialog = dialogBuilder.create()
        progressDialog.show()

        if(!downloadFinished) {
            val q =
                "https://boardgamegeek.com/xmlapi2/collection?username=$user&stats=1&own=1&excludesubtype=boardgameexpansion"
            pobierz(q, user + "_collection", true)
            while (!saveFinished) {
            }
            saveFinished = false

            getAddOn(user)

            while (!saveFinished) {
            }
            koniec()
        }
        val i=Intent(this,MainActivity::class.java)
        if(saveFinished) {
            i.putExtra("data", today)
            i.putExtra("username", UserName)
            i.putExtra("liczbaGier", liczbaGier.toString())
            i.putExtra("liczbaDodatkow", liczbaDodatkow.toString())
            startActivity(i)
        }
    }
fun getAddOn(user: String){
    val q = "https://boardgamegeek.com/xmlapi2/collection?username=$user&stats=1&own=1&subtype=boardgameexpansion"
    pobierz(q, "user_addons", false)
    while(!downloadFinished){}
}
fun sleepyHead() = runBlocking<Unit> {
    val time = measureTimeMillis {
        val one = async { delay(4000L) }
        one.await()
    }
}
    private fun saveRetry(f: String, main: Boolean): Boolean {
        val testDirectory = File("$filesDir/XML")
        val filename = "$testDirectory/$f.xml"
        val file = File(filename)
        var xmlDoc: Document
        var cor: Boolean = false
        var c = 0
        while (!cor) {
            if (c == 15) {
                expired = true
                return false
            }
            cor = true
            try {
                xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            } catch (e: Exception) {
                e.printStackTrace()
                cor = false
                c++
            }
        }

        xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        xmlDoc.documentElement.normalize()
        val items: NodeList = xmlDoc.getElementsByTagName("item")
        if (items.length == 0) {
            Files.deleteIfExists(Paths.get(filename))
            return false
        } else {
            saveDataMain(items, !main)
            Files.deleteIfExists(Paths.get(filename))
            return true
        }
    }
    fun pobierz(q: String, filename: String, main: Boolean) {
        downloadFinished = false
        val cd = DataDownloader()
        cd.setData(q, filename, main)
        cd.execute()
    }

    fun koniec() {
        val baza = DataBaseMain(this, null, null, 1)
        liczbaGier = baza.countGames()
        liczbaDodatkow = baza.countAddons()
        run { Toast.makeText(this, "Koniec", Toast.LENGTH_LONG) }
    }
    private fun saveDataMain(items: NodeList, small: Boolean) {
        val dbHandler = DataBaseMain(this, null, null,  1)
        val dbStat = DBHandlerStat(this, null, null, 1)
        if(!small)
        {dbHandler.clear()}
        for(i in 0..items.length-1){
            val itemNode: Node = items.item(i)
            if(itemNode.nodeType == Node.ELEMENT_NODE) {
                val elem = itemNode as Element
                val children = elem.childNodes
                var id: String? = null
                var title: String? = null
                var org_title: String? = null
                var year_pub: String? = null
                var rank_pos: String? = null
                var pic: String? = null
                var photo: String? = null
                var error: String? = null
                var tmp: String? = null
                val tags = itemNode.attributes
                for(j in 0..tags.length-1){
                    val node = tags.item(j)
                    when (node.nodeName){
                        "objectid" -> {id = node.nodeValue}
                    }
                }
                for(j in 0..children.length-1) {
                    val node = children.item(j)
                    if (node is Element) {
                        when (node.nodeName) {
                            "name" -> {
                                title = node.textContent
                            }
                            "yearpublished" -> {
                                year_pub = node.textContent
                            }
                            "thumbnail" -> {
                                pic = node.textContent
                            }
                            "image" -> {
                                photo = node.textContent
                            }
                            "error" -> {
                                error = node.textContent
                            }
                            "stats" -> {
                                val n = node.childNodes
                                for (j1 in 0..n.length - 1) {
                                    val node = n.item(j1)
                                    if (node is Element) {
                                        when (node.nodeName) {
                                            "rating" -> {
                                                val n = node.childNodes
                                                for (j2 in 0..n.length - 1) {
                                                    val node = n.item(j2)
                                                    if (node is Element) {
                                                        when (node.nodeName) {
                                                            "ranks" -> {
                                                                val n = node.childNodes
                                                                for (j3 in 0..n.length - 1) {
                                                                    val node = n.item(j3)
                                                                    if (node is Element) {
                                                                        val tags = node.attributes
                                                                        for (j4 in 0..tags.length - 1) {
                                                                            val node = tags.item(j4)
                                                                            when (node.nodeName) {
                                                                                "id" -> {
                                                                                    tmp =
                                                                                        node.nodeValue
                                                                                }
                                                                                "value" -> {
                                                                                    rank_pos =
                                                                                        node.nodeValue
                                                                                }
                                                                            }
                                                                            if (tmp == "1" && rank_pos != null) {
                                                                                break
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                var _expansion: Int = 0
                if(small){
                    _expansion = 1
                }
                if(rank_pos == "Not Ranked" || rank_pos == null){
                    rank_pos = "0"
                }
                var _id: Int = Integer.parseInt(id)
                var _title: String? = title
                var _org_title: String? = title
                var _year_pub: Int = 0
                if(year_pub == null){
                    _year_pub = 1900
                }
                else{
                    _year_pub = Integer.parseInt(year_pub)
                }
                var _rank_pos: Int = Integer.parseInt(rank_pos)
                var _pic: String? = pic
                var _photo: String? = photo
                var _error: String? = error

                val product = DataBase(_id, _title, _org_title, _year_pub,_rank_pos, _pic,_photo,_error, _expansion)
                dbHandler.addRecord(product)
                if(_rank_pos!= 0){

                    val s = Stat(_id, _rank_pos, today)
                    dbStat.addStat(s)
                }
            }
        }
        saveFinished = true
    }
}