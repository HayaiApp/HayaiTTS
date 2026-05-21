package dev.ahmedmohamed.hayaitts.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.ahmedmohamed.hayaitts.data.db.entities.AppRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRouteDao {

    @Query("SELECT * FROM app_routes ORDER BY callerPackage, locale")
    fun all(): Flow<List<AppRouteEntity>>

    @Query("SELECT * FROM app_routes WHERE callerPackage = :callerPackage AND locale = :locale LIMIT 1")
    suspend fun lookup(callerPackage: String, locale: String): AppRouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AppRouteEntity)

    @Delete
    suspend fun delete(entry: AppRouteEntity)
}
