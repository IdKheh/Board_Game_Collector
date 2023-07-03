package wiktoria.debowska_151874

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import java.util.concurrent.Executors

class EkranDodatkow : AppCompatActivity() {
    var addons: Boolean = true
    var desc = false
    var actO = Orders._ID
    fun Boolean.toInt() = if (this) 1 else 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ekran_dodatkow)

        val dbHandler = DataBaseMain(this, null, null,  1)
        val theList = dbHandler.getVals(addons.toInt(), actO, desc)
        populate(theList)
    }
    private fun populate(l: List<DataBase>) {
        val table: TableLayout = findViewById(R.id.tabela)
        for (i in 0..l.lastIndex) {
            val row = TableRow(this)
            val lp = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
            row.layoutParams = lp
            val textViewId = TextView(this)
            textViewId.text = l[i].id.toString()
            textViewId.setPadding(20, 15, 20, 15)
            val textViewName = TextView(this)
            textViewName.text = stringCutter(l[i].title!!)
            textViewName.setPadding(20, 15, 20, 15)
            val textViewYear = TextView(this)
            textViewYear.text = l[i].year_pub.toString()
            textViewYear.setPadding(20, 15, 20, 15)
            val im = ImageView(this)
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            var image: Bitmap? = null
            executor.execute {
                val imageURL = l[i].pic
                try {
                    val `in` = java.net.URL(imageURL).openStream()
                    image = BitmapFactory.decodeStream(`in`)
                    handler.post {
                        im.setImageBitmap(image)
                    }
                } catch (e: Exception) {

                    im.setImageBitmap(image)
                    e.printStackTrace()
                }
            }
            row.addView(textViewId)
            row.addView(textViewName)
            row.addView(textViewYear)
            row.addView(im)
            if(addons){
                val textViewRank = TextView(this)
                if(l[i].rank_pos == 0){
                    textViewRank.text = "N/A"
                    row.setOnClickListener() {
                        callStats(l[i].id)
                    }
                }
                else{
                    textViewRank.text = l[i].rank_pos.toString()
                    row.setOnClickListener() {
                        callStats(l[i].id)
                    }
                }
                textViewRank.setPadding(20, 15, 20, 15)
                textViewRank.gravity = Gravity.CENTER
                row.addView(textViewRank)
            }
            else{
                val rankCap: TextView = findViewById(R.id.ranga)
                rankCap.visibility = View.INVISIBLE
            }
            table.addView(row, i)
        }
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
    fun callStats(id: Int) {
        val i = Intent(this, EkranGryDodatku::class.java)
        val b = Bundle()
        b.putInt("Id", id)
        i.putExtras(b)
        startActivity(i)
    }
    fun sort(v: View) {
        val table: TableLayout = findViewById(R.id.tabela)
        val childCount = table.childCount
        if (childCount > 0) {
            table.removeViews(0, childCount)
        }
        val i = v.id

        when(i){
            R.id.id->{
                checkSet(Orders._ID)
            }
            R.id.tytul->{
                checkSet(Orders.TITLE)
            }
            R.id.rok->{
                checkSet(Orders.YEAR_PUB)
            }
            R.id.ranga->{
                if(addons){
                    checkSet(Orders.RANK_POS)
                }
            }
        }
        val dbHandler = DataBaseMain(this, null, null,  1)
        val theList = dbHandler.getVals(addons.toInt(), actO, desc)
        populate(theList)
    }
    fun checkSet(new: Orders){
        if(actO == new){
            desc = desc xor true
        }
        else{
            desc = false
            actO = new
        }
    }
}