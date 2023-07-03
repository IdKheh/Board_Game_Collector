package wiktoria.debowska_151874

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.*
import java.lang.Exception
import java.lang.Runnable
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.measureTimeMillis

class EkranKonfuguracyjny : AppCompatActivity() {
    var liczbaGier=0
    var liczbaDodatkow=0
    var formattedDate=""
    var saveFinished: Boolean = false
    var downloadFinished: Boolean = false
    var expired: Boolean = false
    var login: Boolean = false
    public final lateinit var progressDialog:AlertDialog
    lateinit var sharedPreferences: SharedPreferences
    private val KEY_USERENTRY = "KEY_USERENTRY"
    private val KEY_USERENTRY1 = "KEY_USERENTRY1"
    private val KEY_USERENTRY2 = "KEY_USERENTRY2"
    var PREFS_KEY = "prefs"
    private val KEY_USERENTRY3 = "KEY_USERENTRY3"

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
                            Toast.makeText(this@EkranKonfuguracyjny, "Error", Toast.LENGTH_LONG)
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ekran_konfuguracyjny)
        sharedPreferences = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
    }
    fun zaloguj(v: View){
        val user: EditText = findViewById(R.id.userName)
        if(user.getText().toString() != ""){
            val userName= user.getText().toString()
            val progressView: View = layoutInflater.inflate(R.layout.progress_popup, null)
            val dialogBuilder = AlertDialog.Builder(this@EkranKonfuguracyjny)
            dialogBuilder.setView(progressView)
            progressDialog = dialogBuilder.create()
            progressDialog.show()
            synchro(userName)

            val c: Date = Calendar.getInstance().getTime()
            val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val formattedDate: String = df.format(c)

            val i= Intent(this,MainActivity::class.java)
            val editor: SharedPreferences.Editor = sharedPreferences.edit()

            editor.putString(KEY_USERENTRY, userName)
            editor.putString(KEY_USERENTRY1, liczbaGier.toString())
            editor.putString(KEY_USERENTRY2, liczbaDodatkow.toString())
            editor.putString(KEY_USERENTRY3, formattedDate)

            editor.apply()
            startActivity(i)
        }
    }
    fun synchro(user: String){
        var q = "https://boardgamegeek.com/xmlapi2/collection?username=$user&stats=1&own=1&excludesubtype=boardgameexpansion"
        pobierz(q, user+"_collection", true)
        while(!saveFinished){}
        saveFinished=false

        q = "https://boardgamegeek.com/xmlapi2/collection?username=$user&stats=1&own=1&subtype=boardgameexpansion"
        pobierz(q, "user_addons", false)
        while(!downloadFinished){}

        while(!saveFinished){}
        val baza = DataBaseMain(this, null, null,  1)
        liczbaGier = baza.countGames()
        liczbaDodatkow = baza.countAddons()
        run{Toast.makeText(this, "Koniec", Toast.LENGTH_LONG)}
    }
    fun pobierz(q: String, filename: String, main:Boolean){
        downloadFinished = false
        val cd = DataDownloader()
        cd.setData(q, filename, main)
        cd.execute()
    }

    fun sleepyHead() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async { delay(4000L) }
            one.await()
        }
    }
    private fun saveRetry(f: String, main: Boolean):Boolean {
        val testDirectory = File("$filesDir/XML")
        val filename =  "$testDirectory/$f.xml"
        val file = File(filename)
        var xmlDoc: Document
        var cor: Boolean = false
        var c = 0
        while(!cor){
            if(c == 15)
            {
                expired = true
                return false
            }
            cor = true
            try{
                xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            }catch (e: Exception){
                e.printStackTrace()
                cor = false
                c++
            }
        }

        xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        xmlDoc.documentElement.normalize()
        val items: NodeList = xmlDoc.getElementsByTagName("item")
        if(items.length == 0) {
            Files.deleteIfExists(Paths.get(filename))
            return false
        }else{
            saveDataMain(items, !main)
            Files.deleteIfExists(Paths.get(filename))
            return true
        }
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
                var expansion: Int = 0
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

                val product = DataBase(_id, _title, _org_title, _year_pub,_rank_pos, _pic, _photo,_error,_expansion)
                dbHandler.addRecord(product)
                if(_rank_pos!= 0){

                    val s = Stat(_id, _rank_pos, formattedDate)
                    dbStat.addStat(s)
                }
            }
        }
        saveFinished = true
    }
}