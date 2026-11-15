package app.championguides.data.catalog

import android.content.res.AssetManager
import app.championguides.domain.Catalog
import app.championguides.domain.CatalogRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

class AssetCatalogRepository(private val assets: AssetManager) : CatalogRepository {
    override suspend fun load(): Catalog? = withContext(Dispatchers.IO) {
        try {
            assets.open("catalog.json").bufferedReader(Charsets.UTF_8)
                .use { CatalogJson.parse(it.readText()) }
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
