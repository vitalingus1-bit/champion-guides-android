package app.championguides.data.catalog

import app.championguides.domain.Catalog
import app.championguides.domain.CatalogSnapshot
import app.championguides.domain.Champion
import app.championguides.domain.GuideLink
import app.championguides.domain.Relic
import app.championguides.domain.RelicBuild
import app.championguides.domain.StarPower
import org.json.JSONArray
import org.json.JSONObject

object CatalogJson {
    fun parse(text: String): Catalog {
        val root = JSONObject(text)
        require(root.get("schemaVersion") == 1) { "Unsupported catalogue schema" }
        val relics = root.getJSONArray("relics").mapItems { index ->
            getJSONObject(index).let { Relic(it.required("id"), it.required("name"), it.optional("description")) }
        }
        val champions = root.getJSONArray("champions").mapItems { index ->
            val entry = getJSONObject(index)
            Champion(
                id = entry.required("id"),
                name = entry.required("name"),
                region = entry.optional("region"),
                aliases = entry.getJSONArray("aliases").mapItems { getString(it) },
                plan = entry.required("plan"),
                builds = entry.getJSONArray("builds").mapItems { parseBuild(getJSONObject(it)) },
                originalName = entry.optional("originalName"),
                mulligan = entry.optional("mulligan"),
                caution = entry.optional("caution"),
                portrait = entry.optional("portrait"),
                starPowers = entry.optJSONArray("starPowers")?.mapItems {
                    val power = getJSONObject(it)
                    StarPower(power.getInt("star"), power.required("name"), power.required("description"))
                }.orEmpty(),
                referenceGuides = entry.optJSONArray("referenceGuides")?.mapItems {
                    val link = getJSONObject(it)
                    GuideLink(link.required("title"), link.required("url"))
                }.orEmpty(),
            )
        }
        val snapshot = root.optJSONObject("snapshot")?.let {
            CatalogSnapshot(it.required("patch"), it.required("checkedDate"))
        }
        return Catalog(champions, relics, snapshot)
    }

    private fun parseBuild(entry: JSONObject) = RelicBuild(
        id = entry.required("id"),
        title = entry.required("title"),
        stars = entry.optionalInt("stars"),
        level = entry.optionalInt("level"),
        slots = entry.getJSONArray("slots").mapItems { index ->
            getJSONArray(index).mapItems { getString(it) }
        },
        maxStars = entry.optionalInt("maxStars"),
        strategy = entry.optional("strategy"),
        notes = entry.optional("notes"),
        source = entry.optional("source"),
        requirementsSource = entry.optional("requirementsSource"),
    )

    private fun JSONObject.optional(key: String): String? = if (isNull(key)) null else required(key)

    private fun JSONObject.required(key: String): String =
        (get(key) as? String)?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Expected a non-empty string for $key")

    private fun JSONObject.optionalInt(key: String): Int? =
        if (isNull(key)) null else get(key) as? Int
            ?: throw IllegalArgumentException("Expected an integer for $key")

    private fun <T> JSONArray.mapItems(transform: JSONArray.(Int) -> T): List<T> =
        List(length()) { transform(it) }
}
