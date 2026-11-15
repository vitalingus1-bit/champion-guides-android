package app.championguides.data.profile

import android.database.SQLException
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import app.championguides.domain.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "favorites")
data class Favorite(@PrimaryKey val championId: String)

@Dao
interface FavoriteDao {
    @Query("SELECT championId FROM favorites")
    fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE championId = :championId")
    suspend fun remove(championId: String)
}

@Database(entities = [Favorite::class], version = 1, exportSchema = true)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao
}

class RoomFavoriteRepository(private val dao: FavoriteDao) : FavoriteRepository {
    override fun observeIds(): Flow<Set<String>> = dao.observeIds().map { it.toSet() }

    override suspend fun setFavorite(championId: String, favorite: Boolean): Boolean {
        return try {
            if (favorite) dao.insert(Favorite(championId)) else dao.remove(championId)
            true
        } catch (_: SQLException) {
            false
        }
    }
}
