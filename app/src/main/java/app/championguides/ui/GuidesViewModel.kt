package app.championguides.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.championguides.domain.Catalog
import app.championguides.domain.CatalogRepository
import app.championguides.domain.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface CatalogState {
    data object Loading : CatalogState
    data object Failed : CatalogState
    data class Ready(val catalog: Catalog) : CatalogState
}

class GuidesViewModel(
    catalogRepository: CatalogRepository,
    private val favorites: FavoriteRepository,
) : ViewModel() {
    private val mutableCatalog = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalog = mutableCatalog.asStateFlow()
    private val mutableFavoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds = mutableFavoriteIds.asStateFlow()
    private val mutableProfileError = MutableStateFlow(false)
    val profileError = mutableProfileError.asStateFlow()

    init {
        viewModelScope.launch {
            mutableCatalog.value = catalogRepository.load()?.let { CatalogState.Ready(it) }
                ?: CatalogState.Failed
        }
        viewModelScope.launch {
            favorites.observeIds()
                .catch { mutableProfileError.value = true }
                .collect { mutableFavoriteIds.value = it }
        }
    }

    fun setFavorite(championId: String, favorite: Boolean) {
        viewModelScope.launch {
            if (!favorites.setFavorite(championId, favorite)) {
                mutableProfileError.value = true
            }
        }
    }

    fun dismissProfileError() {
        mutableProfileError.value = false
    }
}
