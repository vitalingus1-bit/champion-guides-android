package app.championguides.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChampionPortrait(path: String, modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val portrait by produceState<Bitmap?>(null, assets, path) {
        value = withContext(Dispatchers.IO) {
            try {
                assets.open(path).use {
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeStream(it, null, options)
                }
            } catch (_: IOException) {
                null
            }
        }
    }

    portrait?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxWidth().aspectRatio(2f).clip(MaterialTheme.shapes.large),
        )
    }
}
