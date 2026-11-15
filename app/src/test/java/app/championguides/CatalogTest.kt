package app.championguides

import app.championguides.data.catalog.CatalogJson
import app.championguides.domain.BuildComparison
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test

class CatalogTest {
    private val fixture = File("src/main/assets/catalog.json").readText(Charsets.UTF_8)
    private val catalog = CatalogJson.parse(fixture)

    @Test
    fun findsMultipleTokensAcrossAliasesAndRegion() {
        val matches = catalog.search("КУРЬЕР ridge", favorites = null, descending = false)
        assertEquals(listOf("courier"), matches.map { it.id })
    }

    @Test
    fun filtersFavoritesAndSortsDescending() {
        val matches = catalog.search("", setOf("courier", "warden"), descending = true)
        assertEquals(listOf("warden", "courier"), matches.map { it.id })
    }

    @Test
    fun unknownRequirementsStayUnknown() {
        val build = catalog.champions.first().builds[1]
        assertNull(build.stars)
        assertNull(build.level)
    }

    @Test
    fun rejectsBrokenRelicReference() {
        val broken = fixture.replace("[\"brass-lens\", \"trail-compass\"]", "[\"missing-relic\"]")
        assertThrows(IllegalArgumentException::class.java) { CatalogJson.parse(broken) }
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        val newer = fixture.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        assertThrows(IllegalArgumentException::class.java) { CatalogJson.parse(newer) }
    }

    @Test
    fun rejectsNonIntegerSchemaVersion() {
        for (version in listOf("\"1\"", "1.5")) {
            val invalid = fixture.replace("\"schemaVersion\": 1", "\"schemaVersion\": $version")
            assertThrows(IllegalArgumentException::class.java) { CatalogJson.parse(invalid) }
        }
    }

    @Test
    fun comparisonHoldsAtMostThreeAndAllowsRemoval() {
        val courier = catalog.champions[0]
        val keeper = catalog.champions[1]
        val warden = catalog.champions[2]
        val full = BuildComparison()
            .toggle(courier, courier.builds[0])
            .toggle(courier, courier.builds[1])
            .toggle(keeper, keeper.builds.single())
        assertSame(full, full.toggle(warden, warden.builds.single()))
        val next = full.toggle(courier, courier.builds[0])
            .toggle(warden, warden.builds.single())
        assertEquals(3, next.entries.size)
        assertEquals(warden.builds.single(), next.entries.last().build)
    }

    @Test
    fun loadsExportedCatalogWhenRequested() {
        val path = System.getProperty("catalog.assetsDir")
        assumeNotNull(path)
        val directory = File(checkNotNull(path))
        val local = CatalogJson.parse(directory.resolve("catalog.json").readText())
        assertNotNull(local.snapshot)
        assertTrue(local.champions.all {
            val portrait = it.portrait
            portrait != null && directory.resolve(portrait).isFile
        })
        assertTrue(local.relicsById.values.all { !it.description.isNullOrBlank() })

        val soraka = local.search("Сорака", favorites = null, descending = false).single()
        assertEquals("soraka", soraka.id)
        val rally = soraka.builds.first { it.title == "Rapid Rally Engine" }
        assertNull(rally.stars)
        assertNull(rally.level)
        assertEquals("Небесный посох", local.relicsById.getValue(rally.slots.first().first()).name)
        val ahri = local.champions.first { it.id == "ahri" }
        val beforeSix = ahri.builds.first { it.title == "The Gatebreaker Loop (Pre-C6)" }
        assertEquals(3, beforeSix.stars)
        assertEquals(5, beforeSix.maxStars)
        assertEquals(25, beforeSix.level)
        assertEquals(
            "spirit-blossom-ahri",
            local.search("Ари цветение духа", favorites = null, descending = false).single().id,
        )
    }
}
