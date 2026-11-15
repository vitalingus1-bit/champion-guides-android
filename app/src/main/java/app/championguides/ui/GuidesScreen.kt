package app.championguides.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.championguides.R
import app.championguides.domain.BuildComparison
import app.championguides.domain.Catalog
import app.championguides.domain.Champion
import app.championguides.domain.ComparedBuild
import app.championguides.domain.RelicBuild

private val comparisonSelectionSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it },
)

@Composable
fun GuidesScreen(viewModel: GuidesViewModel) {
    val state by viewModel.catalog.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val profileError by viewModel.profileError.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val profileErrorText = stringResource(R.string.favorite_error)

    LaunchedEffect(profileError) {
        if (profileError) {
            snackbar.showSnackbar(profileErrorText, duration = SnackbarDuration.Short)
            viewModel.dismissProfileError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        val body = Modifier.fillMaxSize().padding(insets)
        when (val value = state) {
            CatalogState.Loading -> Box(body, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            CatalogState.Failed -> Box(body.padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.catalog_error))
            }
            is CatalogState.Ready -> Catalogue(
                catalog = value.catalog,
                favorites = favorites,
                onFavoriteChange = viewModel::setFavorite,
                modifier = body,
            )
        }
    }
}

@Composable
private fun Catalogue(
    catalog: Catalog,
    favorites: Set<String>,
    onFavoriteChange: (String, Boolean) -> Unit,
    modifier: Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var descending by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var comparisonIds by rememberSaveable(stateSaver = comparisonSelectionSaver) {
        mutableStateOf(emptyList<String>())
    }
    var showComparison by rememberSaveable { mutableStateOf(false) }
    val visible = remember(catalog, query, favorites, favoritesOnly, descending) {
        catalog.search(query, favorites.takeIf { favoritesOnly }, descending)
    }
    val selected = catalog.champions.find { it.id == selectedId }
    val comparison = remember(catalog, comparisonIds) {
        comparisonIds.fold(BuildComparison()) { current, buildId ->
            val champion = catalog.champions.find { entry -> entry.builds.any { it.id == buildId } }
            val build = champion?.builds?.find { it.id == buildId }
            if (champion != null && build != null) current.toggle(champion, build) else current
        }
    }

    Column(modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(
            catalog.snapshot?.let { stringResource(R.string.snapshot_notice, it.patch, it.checkedDate) }
                ?: stringResource(R.string.fixture_notice),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
        )
        if (comparisonIds.isNotEmpty()) {
            FilledTonalButton(
                onClick = { showComparison = true },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.compare, comparisonIds.size))
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 750.dp
            BackHandler(enabled = selected != null && !isWide) { selectedId = null }

            val list: @Composable (Modifier) -> Unit = { pane ->
                ChampionList(
                    champions = visible,
                    query = query,
                    onQueryChange = { query = it },
                    favoritesOnly = favoritesOnly,
                    onToggleFavorites = { favoritesOnly = !favoritesOnly },
                    descending = descending,
                    onToggleOrder = { descending = !descending },
                    selectedId = selectedId,
                    onSelect = { selectedId = it },
                    modifier = pane,
                )
            }
            val detail: @Composable (Champion, Modifier) -> Unit = { champion, pane ->
                ChampionDetail(
                    champion = champion,
                    catalog = catalog,
                    isFavorite = champion.id in favorites,
                    onFavorite = { onFavoriteChange(champion.id, champion.id !in favorites) },
                    onClose = if (isWide) null else ({ selectedId = null }),
                    comparedIds = comparisonIds,
                    onCompare = { build ->
                        comparisonIds = comparison.toggle(champion, build).entries.map { it.build.id }
                    },
                    modifier = pane,
                )
            }

            if (isWide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    list(Modifier.weight(0.9f))
                    if (selected == null) {
                        Text(
                            stringResource(R.string.select_champion),
                            Modifier.weight(1.1f).padding(top = 16.dp),
                        )
                    } else {
                        detail(selected, Modifier.weight(1.1f))
                    }
                }
            } else if (selected != null) {
                detail(selected, Modifier.fillMaxSize())
            } else {
                list(Modifier.fillMaxSize())
            }
        }
    }

    if (showComparison && comparison.entries.isNotEmpty()) {
        ComparisonDialog(
            entries = comparison.entries,
            catalog = catalog,
            onRemove = { buildId ->
                comparisonIds = comparison.remove(buildId).entries.map { it.build.id }
                if (comparisonIds.isEmpty()) showComparison = false
            },
            onClose = { showComparison = false },
        )
    }
}

@Composable
private fun ChampionList(
    champions: List<Champion>,
    query: String,
    onQueryChange: (String) -> Unit,
    favoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    descending: Boolean,
    onToggleOrder: () -> Unit,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.search)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = favoritesOnly,
                onClick = onToggleFavorites,
                label = { Text(stringResource(R.string.favorites)) },
            )
            TextButton(onClick = onToggleOrder) {
                Text(stringResource(if (descending) R.string.za else R.string.az))
            }
        }
        if (champions.isEmpty()) {
            Text(stringResource(R.string.empty_results))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(champions, key = Champion::id) { champion ->
                    Card(
                        onClick = { onSelect(champion.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (champion.id == selectedId) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                        ),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(champion.name, style = MaterialTheme.typography.titleMedium)
                            (champion.region ?: champion.originalName)?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChampionDetail(
    champion: Champion,
    catalog: Catalog,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onClose: (() -> Unit)?,
    comparedIds: List<String>,
    onCompare: (RelicBuild) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            if (onClose != null) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
            }
            Text(champion.name, style = MaterialTheme.typography.headlineSmall)
            (champion.region ?: champion.originalName)?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary)
            }
            TextButton(onClick = onFavorite) {
                Text(stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite))
            }
        }
        champion.portrait?.let { path -> item(key = "portrait:${champion.id}") { ChampionPortrait(path) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GuideParagraph(stringResource(R.string.overview), champion.plan)
                champion.mulligan?.let { GuideParagraph(stringResource(R.string.mulligan), it) }
                champion.caution?.let { GuideParagraph(stringResource(R.string.caution), it) }
            }
        }
        item {
            Text(stringResource(R.string.builds), style = MaterialTheme.typography.titleLarge)
            if (comparedIds.size == BuildComparison.MAX_BUILDS) {
                Text(stringResource(R.string.comparison_full), style = MaterialTheme.typography.bodySmall)
            }
        }
        items(champion.builds, key = RelicBuild::id) { build ->
            val inComparison = build.id in comparedIds
            BuildCard(build, catalog) {
                TextButton(
                    onClick = { onCompare(build) },
                    enabled = inComparison || comparedIds.size < BuildComparison.MAX_BUILDS,
                ) {
                    Text(stringResource(if (inComparison) R.string.remove_comparison else R.string.add_comparison))
                }
            }
        }
        if (champion.starPowers.isNotEmpty()) {
            item(key = "powers:${champion.id}") {
                ExpandableSection(stringResource(R.string.star_powers)) {
                    champion.starPowers.forEach { power ->
                        GuideParagraph("${power.star}★ · ${power.name}", power.description)
                    }
                }
            }
        }
        if (champion.referenceGuides.isNotEmpty()) {
            item(key = "references:${champion.id}") {
                ExpandableSection(
                    stringResource(R.string.reference_guides, champion.referenceGuides.size),
                ) {
                    champion.referenceGuides.forEach { SourceLink(it.title, it.url) }
                }
            }
        }
    }
}

@Composable
private fun GuideParagraph(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description)
    }
}

@Composable
private fun ExpandableSection(label: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(
                stringResource(if (expanded) R.string.hide_section else R.string.show_section, label),
            )
        }
        if (expanded) content()
    }
}

@Composable
private fun SourceLink(title: String, url: String) {
    val handler = LocalUriHandler.current
    TextButton(onClick = { handler.openUri(url) }) { Text(title) }
}

@Composable
private fun BuildCard(
    build: RelicBuild,
    catalog: Catalog,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    var inspectedSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(build.title, style = MaterialTheme.typography.titleMedium)
            Text(buildRequirementsLabel(build), color = MaterialTheme.colorScheme.secondary)
            build.slots.forEachIndexed { index, slot ->
                val relics = slot.map(catalog.relicsById::getValue)
                val names = relics.joinToString(" / ") { it.name }
                val label = stringResource(R.string.relic_options, index + 1, names)
                if (relics.any { it.description != null }) {
                    TextButton(onClick = { inspectedSlot = index }) { Text(label) }
                } else {
                    Text(label)
                }
            }
            if (build.strategy != null || build.notes != null || build.source != null) {
                ExpandableSection(stringResource(R.string.strategy_and_sources)) {
                    build.notes?.let { Text(it) }
                    build.strategy?.let { Text(it) }
                    build.source?.let {
                        SourceLink(stringResource(R.string.build_source), it)
                    }
                    build.requirementsSource?.let {
                        SourceLink(stringResource(R.string.requirements_source), it)
                    }
                }
            }
            action()
        }
    }
    inspectedSlot?.let { index ->
        val relics = build.slots[index].map(catalog.relicsById::getValue)
        AlertDialog(
            onDismissRequest = { inspectedSlot = null },
            title = { Text(stringResource(R.string.relic_slot_title, index + 1)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    relics.forEach { relic ->
                        GuideParagraph(relic.name, relic.description.orEmpty())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedSlot = null }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun buildRequirementsLabel(build: RelicBuild): String {
    val min = build.stars
    val max = build.maxStars
    val level = build.level
    if (min == null && max == null && level == null) {
        return stringResource(R.string.unknown_requirements)
    }
    val stars = when {
        min != null && max != null && min != max -> stringResource(R.string.stars_range, min, max)
        min != null -> stringResource(R.string.stars_exact, min)
        max != null -> stringResource(R.string.stars_through, max)
        else -> stringResource(R.string.stars_unknown)
    }
    val levelLabel = if (level == null) stringResource(R.string.level_unknown)
        else stringResource(R.string.level_exact, level)
    return stringResource(R.string.build_requirements, stars, levelLabel)
}

@Composable
private fun ComparisonDialog(
    entries: List<ComparedBuild>,
    catalog: Catalog,
    onRemove: (String) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            BoxWithConstraints(Modifier.padding(16.dp)) {
                val maxBodyHeight = maxHeight * 0.78f
                val wide = maxWidth >= 720.dp
                LazyColumn(
                    Modifier.heightIn(max = maxBodyHeight),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            stringResource(R.string.comparison_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    if (wide) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                entries.forEach { entry ->
                                    ComparisonCard(entry, catalog, onRemove, Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(entries, key = { it.build.id }) { entry ->
                            ComparisonCard(entry, catalog, onRemove)
                        }
                    }
                    item {
                        TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    entry: ComparedBuild,
    catalog: Catalog,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(entry.championName, style = MaterialTheme.typography.labelLarge)
        BuildCard(entry.build, catalog) {
            TextButton(onClick = { onRemove(entry.build.id) }) {
                Text(stringResource(R.string.remove_comparison))
            }
        }
    }
}
