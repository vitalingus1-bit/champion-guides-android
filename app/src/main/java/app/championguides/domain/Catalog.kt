package app.championguides.domain

import java.text.Normalizer
import java.util.Locale

data class Relic(val id: String, val name: String, val description: String? = null)

data class CatalogSnapshot(val patch: String, val checkedDate: String)

data class StarPower(val star: Int, val name: String, val description: String)

data class GuideLink(val title: String, val url: String)

data class RelicBuild(
    val id: String,
    val title: String,
    val stars: Int?,
    val level: Int?,
    val slots: List<List<String>>,
    val maxStars: Int? = null,
    val strategy: String? = null,
    val notes: String? = null,
    val source: String? = null,
    val requirementsSource: String? = null,
)

data class Champion(
    val id: String,
    val name: String,
    val region: String?,
    val aliases: List<String>,
    val plan: String,
    val builds: List<RelicBuild>,
    val originalName: String? = null,
    val mulligan: String? = null,
    val caution: String? = null,
    val portrait: String? = null,
    val starPowers: List<StarPower> = emptyList(),
    val referenceGuides: List<GuideLink> = emptyList(),
)

class Catalog(
    val champions: List<Champion>,
    relics: List<Relic>,
    val snapshot: CatalogSnapshot? = null,
) {
    val relicsById: Map<String, Relic> = relics.associateBy(Relic::id)

    init {
        val builds = champions.flatMap(Champion::builds)
        require(champions.isNotEmpty())
        require(champions.map(Champion::id).distinct().size == champions.size)
        require(relicsById.size == relics.size)
        require(builds.map(RelicBuild::id).distinct().size == builds.size)
        builds.forEach { build ->
            require(build.slots.size in 1..3)
            require(build.stars == null || build.stars >= 0)
            require(build.maxStars == null || build.maxStars >= (build.stars ?: 0))
            require(build.level == null || build.level >= 1)
            build.slots.forEach { slot ->
                require(slot.isNotEmpty() && slot.distinct().size == slot.size)
                require(slot.all(relicsById::containsKey))
            }
        }
    }

    fun search(query: String, favorites: Set<String>?, descending: Boolean): List<Champion> {
        val tokens = query.normalized().split(Regex("[^\\p{L}\\p{N}]+")).filter(String::isNotEmpty)
        val matches = champions.filter { champion ->
            val allowedByFavorites = favorites == null || champion.id in favorites
            val searchable = (champion.aliases + listOfNotNull(champion.name, champion.originalName, champion.region))
                .joinToString(" ").normalized()
            allowedByFavorites && tokens.all(searchable::contains)
        }
        val order = compareBy<Champion> { it.name.normalized() }.thenBy(Champion::id)
        return matches.sortedWith(if (descending) order.reversed() else order)
    }
}

private fun String.normalized(): String = Normalizer.normalize(
    lowercase(Locale.ROOT), Normalizer.Form.NFKD,
).replace(Regex("\\p{M}+"), "")

data class ComparedBuild(val championName: String, val build: RelicBuild)

class BuildComparison private constructor(val entries: List<ComparedBuild>) {
    constructor() : this(emptyList())

    fun toggle(champion: Champion, build: RelicBuild): BuildComparison {
        require(build in champion.builds)
        return when {
            entries.any { it.build.id == build.id } ->
                BuildComparison(entries.filterNot { it.build.id == build.id })
            entries.size == MAX_BUILDS -> this
            else -> BuildComparison(entries + ComparedBuild(champion.name, build))
        }
    }

    fun remove(buildId: String): BuildComparison =
        BuildComparison(entries.filterNot { it.build.id == buildId })

    companion object {
        const val MAX_BUILDS = 3
    }
}
