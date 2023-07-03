package wiktoria.debowska_151874

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths


class MainActivity : AppCompatActivity() {
    var UserName="costam"
    var numberOfGames="0"
    var numberOfDlc="0"
    var data="28-"
    private val KEY_USERENTRY = "KEY_USERENTRY"
    private val KEY_USERENTRY1 = "KEY_USERENTRY1"
    private val KEY_USERENTRY2 = "KEY_USERENTRY2"
    var PREFS_KEY = "prefs"
    private val KEY_USERENTRY3 = "KEY_USERENTRY3"
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

        UserName = sharedPreferences.getString(KEY_USERENTRY, UserName).toString()
        numberOfGames = sharedPreferences.getString(KEY_USERENTRY1,numberOfGames).toString()
        numberOfDlc = sharedPreferences.getString(KEY_USERENTRY2,numberOfDlc).toString()
        data = sharedPreferences.getString(KEY_USERENTRY3,data).toString()

        wczytanie()
        val user1: TextView = findViewById(R.id.user)
        if(UserName=="costam" || UserName==""|| user1.getText().toString() == "Cześć, costam!")
        { val i= Intent(this,EkranKonfuguracyjny::class.java)
            startActivity(i)
        }

    }
    fun wczytanie(){
        val user: TextView=findViewById(R.id.user)
        val message1 = "Cześć, " + UserName + "!"
        user.setText(message1)

        val games: TextView = findViewById(R.id.games)
        val message2 = "Liczba gier: " + numberOfGames
        games.setText(message2)

        val dlc: TextView = findViewById(R.id.dlc)
        val message3 = "Liczba dodatków: " + numberOfDlc
        dlc.setText(message3)

        val data1: TextView = findViewById(R.id.data)
        val message4 = "Data synchronizacji: " + data
        data1.setText(message4)
    }
    fun gry(v: View){
        val i= Intent(this,EkranGier::class.java)
        startActivity(i)
    }
    fun dodatki(v: View){
        val i=Intent(this,EkranDodatkow::class.java)
        startActivity(i)
    }
    fun synchronizacja(v: View){
        val i=Intent(this,EkranSynchronizacji::class.java)
        i.putExtra("username",UserName)
        i.putExtra("liczbaGier",numberOfGames.toString())
        i.putExtra("liczbaDodatkow",numberOfDlc.toString())
        i.putExtra("data",data)
        startActivity(i)
    }
    fun logout(v: View){
        val i=Intent(this,EkranKonfuguracyjny::class.java)
        UserName="costam"
        numberOfDlc=""
        numberOfGames=""
        data=""
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setMessage("Na pewno chesz usunąć dane?")
            .setCancelable(false)
            .setPositiveButton("Tak") { dialog, id ->
                var path = Paths.get("$filesDir/data.txt")
                try {
                    val result = Files.deleteIfExists(path)
                    if (result) {
                        println("Usunięto dane")
                    } else {
                        println("Usuwanie zakończyło się niepowodzeniem.")
                    }
                } catch (e: IOException) {
                    println("Usuwanie zakończyło się niepowodzeniem.")
                    e.printStackTrace()
                }
                val dbHandler = DataBaseMain(this, null, null,  1)
                dbHandler.clear()
                val dbStat = DBHandlerStat(this, null,null, 1)
                dbStat.clear()
                finish()
                startActivity(i)
            }
            .setNegativeButton("Nie") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }
}