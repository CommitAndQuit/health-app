package io.biotrace.app.data.local.store

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local persistent store for demographic parameters required by clinical phenotypic models.
 */
@Singleton
class UserProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getChronologicalAge(): Int {
        val stored = prefs.getInt(KEY_CHRONOLOGICAL_AGE, DEFAULT_AGE)
        return stored.coerceIn(MIN_AGE, MAX_AGE)
    }

    fun setChronologicalAge(age: Int) {
        val coerced = age.coerceIn(MIN_AGE, MAX_AGE)
        prefs.edit().putInt(KEY_CHRONOLOGICAL_AGE, coerced).apply()
    }

    fun getBiologicalSex(): String {
        return prefs.getString(KEY_BIOLOGICAL_SEX, DEFAULT_SEX) ?: DEFAULT_SEX
    }

    fun setBiologicalSex(sex: String) {
        prefs.edit().putString(KEY_BIOLOGICAL_SEX, sex).apply()
    }

    companion object {
        private const val PREFS_NAME = "user_profile_prefs"
        private const val KEY_CHRONOLOGICAL_AGE = "chronological_age"
        private const val KEY_BIOLOGICAL_SEX = "biological_sex"

        const val DEFAULT_AGE = 35
        const val MIN_AGE = 18
        const val MAX_AGE = 120
        const val DEFAULT_SEX = "UNSPECIFIED"
    }
}
