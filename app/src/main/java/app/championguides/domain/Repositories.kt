package app.championguides.domain

import kotlinx.coroutines.flow.Flow

fun interface CatalogRepository {
    suspend fun load(): Catalog?
}

interface FavoriteRepository {
    fun observeIds(): Flow<Set<String>>
    suspend fun setFavorite(championId: String, favorite: Boolean): Boolean
}
