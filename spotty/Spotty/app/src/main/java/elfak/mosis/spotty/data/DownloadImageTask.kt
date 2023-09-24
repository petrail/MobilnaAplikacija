package elfak.mosis.spotty.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.ImageView
import java.net.URL

@Suppress("DEPRECATION")
class DownloadImageTask(bm:ImageView): AsyncTask<String, Void, Bitmap>() {
    private var bmImage = bm

    override fun doInBackground(vararg p0: String?): Bitmap? {
        var bMap:Bitmap? = null
        try {
            var inStream = URL(p0[0]).openStream();
            bMap = BitmapFactory.decodeStream(inStream);
        } catch (e:Exception) {
            e.printStackTrace();
        }
        return bMap;
    }

    override fun onPostExecute(result:Bitmap?) {
        if(result!=null)
            bmImage.setImageBitmap(result);
    }

}