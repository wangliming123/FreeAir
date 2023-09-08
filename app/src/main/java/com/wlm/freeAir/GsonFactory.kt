package com.wlm.freeAir

import android.util.Log
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.util.concurrent.ConcurrentHashMap

object GsonFactory {

    const val DEFAULT_GSON = "DEFAULT_GSON"
    const val MAP_GSON = "MAP_GSON"

    private val gsonMap = ConcurrentHashMap<String, Gson>()

    @JvmOverloads
    fun getGson(key: String = DEFAULT_GSON): Gson {
        var gson = gsonMap[key]
        if (gson == null) {
            gson = createGson(key)
        }
        return gson
    }

    fun getExcludeGson(vararg excludeFieldName: String): Gson {
        return GsonBuilder().setExclusionStrategies(object : ExclusionStrategy {
            override fun shouldSkipField(f: FieldAttributes): Boolean {
                return excludeFieldName.contains(f.name)
            }

            override fun shouldSkipClass(clazz: Class<*>): Boolean {
                return false
            }

        }).create()
    }

    private fun createGson(key: String): Gson {
        return when (key) {
            MAP_GSON -> GsonBuilder().enableComplexMapKeySerialization().create()
            else -> Gson()
        }
    }

}


val Any.json: String
    get() {
        return GsonFactory.getGson(GsonFactory.MAP_GSON).toJson(this)
    }

fun Any.excludeJson(vararg excludeFieldName: String): String {
    return GsonFactory.getExcludeGson(*excludeFieldName).toJson(this)
}

val Any.exposeJson: String
    get() {
        return GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(this)
    }

fun <T> String.toObject(c: Class<T>): T {
    return GsonFactory.getGson(GsonFactory.MAP_GSON).fromJson(this, c)
}
fun <T> String.toObject(gson: Gson?, c: Class<T>): T {
    return (gson ?: GsonFactory.getGson(GsonFactory.MAP_GSON)).fromJson(this, c)
}

fun <T> String.toObjectList(c: Class<T>): List<T> {
    val res = mutableListOf<T>()
    try {
        val array = JsonParser.parseString(this).asJsonArray
        val gson = GsonFactory.getGson(GsonFactory.MAP_GSON)
        array.forEach {
            res.add(gson.fromJson(it, c))
        }
    } catch (e: Exception) {
        Log.e("Gson", "toObjectList error", e)
    }

    return res
}
