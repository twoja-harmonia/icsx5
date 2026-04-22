/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package eu.dobreterapie.calendar.db.dao

import androidx.room.*
import eu.dobreterapie.calendar.db.entity.Credential

@Dao
interface CredentialsDao {

    @Query("SELECT * FROM credentials WHERE subscriptionId=:subscriptionId")
    fun getBySubscriptionId(subscriptionId: Long): Credential?

    @Insert
    fun create(credential: Credential)

    @Upsert
    fun upsert(credential: Credential)

    @Query("DELETE FROM credentials WHERE subscriptionId=:subscriptionId")
    fun removeBySubscriptionId(subscriptionId: Long)

    @Update
    fun update(credential: Credential)

}