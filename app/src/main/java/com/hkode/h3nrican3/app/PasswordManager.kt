package com.hkode.h3nrican3.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedPassword(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis()
)

class PasswordManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hkode_saved_passwords", Context.MODE_PRIVATE)
    private val KEY_PASSWORDS = "passwords_list"

    fun getSavedPasswords(): List<SavedPassword> {
        val jsonString = prefs.getString(KEY_PASSWORDS, null) ?: return emptyList()
        val list = mutableListOf<SavedPassword>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedPassword(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        label = obj.optString("label", "Saved Key"),
                        password = obj.optString("password", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.createdAt }
    }

    fun savePassword(label: String, password: String): Boolean {
        if (password.isBlank()) return false
        val currentList = getSavedPasswords().toMutableList()
        
        // If password already exists, update label
        val existingIndex = currentList.indexOfFirst { it.password == password }
        if (existingIndex != -1) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(label = if (label.isNotBlank()) label else existing.label)
        } else {
            val finalLabel = if (label.isNotBlank()) label else "Secret Key ${currentList.size + 1}"
            currentList.add(0, SavedPassword(label = finalLabel, password = password))
        }
        return persistList(currentList)
    }

    fun deletePassword(id: String): Boolean {
        val currentList = getSavedPasswords().filter { it.id != id }
        return persistList(currentList)
    }

    fun clearAll(): Boolean {
        return prefs.edit().remove(KEY_PASSWORDS).commit()
    }

    private fun persistList(list: List<SavedPassword>): Boolean {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("password", item.password)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        return prefs.edit().putString(KEY_PASSWORDS, array.toString()).commit()
    }
}
