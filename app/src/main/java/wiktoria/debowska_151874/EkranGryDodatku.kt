package wiktoria.debowska_151874

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.net.URI
import java.net.URL
import java.util.concurrent.Executors


class EkranGryDodatku : AppCompatActivity() {
    var Id: Int = 0
    var buttonPressed:Boolean=false
    lateinit var picture:Bitmap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ekran_gry_dodatku)

        val extras = intent.extras
        Id = extras!!.getInt("Id")
        val dbm = DataBaseMain(this, null, null, 1)
        val rec = dbm.findRecord(Id)
        setCaptions(rec!!)

        val button6:Button = findViewById(R.id.button6)
        var mGetContent=registerForActivityResult(ActivityResultContracts.GetContent()){
            result->if(result!=null){
                dodaj(result)
            }
        }
        button6.setOnClickListener { mGetContent.launch("image/*") }
    }
    fun dodaj(zdjecie: Uri){
        val table:LinearLayout= findViewById(R.id.zdjecia)
        val image= ImageView (this)
        image.setImageURI(zdjecie)
        image.setPadding(10, 0, 10, 0)

        image.setOnClickListener {
            if(buttonPressed){
                table.removeView(image)
                buttonPressed=false
            }
        }
        if(!buttonPressed){
        table.addView(image)
        }
    }
    fun usun_zdjecie(v:View){
        buttonPressed=true
    }
    fun bigPhoto(v:View){
        val builder = Dialog(this)
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE)
        builder.window!!.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        builder.setOnDismissListener {}
        val imageView = ImageView(this)
        imageView.setImageBitmap(picture)
        builder.addContentView(
            imageView, RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        builder.show()
    }
    fun stringCutter(src: String):String{
        var res: String = ""
        var lim = 10
        for(i in 0..src.length-1){
            if(src[i] != ' '){
                res += src[i]
            }
            else{
                if(i >= lim){
                    res+= '\n'
                    lim+=i
                }
                else{
                    res+=' '
                }
            }
        }
        return res
    }

    private fun setCaptions(rec: DataBase) {
        val statYear: TextView = findViewById(R.id.rok)
        statYear.text = "Rok: "+ rec.year_pub.toString()
        val statID: TextView = findViewById(R.id.id)
        statID.text = "Id: "+rec.id.toString()
        val statName: TextView = findViewById(R.id.tytul)
        statName.text = stringCutter(rec.title!!)

        val statRanga: TextView = findViewById(R.id.ranga)
        statRanga.text = "Pozycja: "+rec.rank_pos.toString()


        val im: ImageView = findViewById(R.id.photo)
        val executor = Executors.newSingleThreadExecutor()
        val executor1 = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        val handler1 = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        var image1: Bitmap? = null
        executor.execute {
            val imageURL = rec.pic
            try {
                val `in` = URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                    im.setImageBitmap(image)
                }
            } catch (e: Exception) {

                im.setImageBitmap(image)

                e.printStackTrace()
            }
        }
        executor1.execute {
            val imageURL = rec.photo
            try {
                val `in` = URL(imageURL).openStream()
                image1 = BitmapFactory.decodeStream(`in`)
                handler1.post {
                    picture= (image1 as Bitmap?)!!
                }
            } catch (e: Exception) {
                picture= (image1 as Bitmap?)!!
                e.printStackTrace()
            }
        }
    }
}