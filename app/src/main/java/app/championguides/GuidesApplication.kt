package app.championguides

import android.app.Application
import androidx.room.Room
import app.championguides.data.profile.ProfileDatabase
import app.championguides.data.profile.RoomFavoriteRepository
import app.championguides.domain.FavoriteRepository

class GuidesApplication : Application() {
    val favorites: FavoriteRepository by lazy {
        val database = Room.databaseBuilder(this, ProfileDatabase::class.java, "profile.db").build()
        RoomFavoriteRepository(database.favorites())
    }
}
