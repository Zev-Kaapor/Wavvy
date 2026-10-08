package com.wavvy.app.core.innertube

// JSON
import org.json.JSONArray
import org.json.JSONObject

// Follows a path of names and positions through the answer, empty when any step is missing
fun JSONObject.at(vararg path: Any): Any? {
    var current: Any? = this
    for (step in path) {
        current = when {
            current is JSONObject && step is String -> current.opt(step)
            current is JSONArray && step is Int -> current.opt(step)
            else -> return null
        }
    }

    return current.takeUnless { it == JSONObject.NULL }
}

// Object at the end of the path
fun JSONObject.objectAt(vararg path: Any): JSONObject? = at(*path) as? JSONObject

// List at the end of the path
fun JSONObject.arrayAt(vararg path: Any): JSONArray? = at(*path) as? JSONArray

// Text at the end of the path, empty when it is blank
fun JSONObject.stringAt(vararg path: Any): String? = (at(*path) as? String)?.takeIf { it.isNotBlank() }

// Every object kept under this name, wherever it is in the answer, so a change in the way the answer is wrapped does not hide it
fun JSONObject.findObjects(name: String): List<JSONObject> {
    val found = mutableListOf<JSONObject>()
    fun walk(value: Any?) {
        when (value) {
            is JSONObject -> {
                value.optJSONObject(name)?.let { found += it }
                value.keys().forEach { walk(value.opt(it)) }
            }
            is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i))
        }
    }
    walk(this)
    return found
}

// Objects of a list, the entries that are not objects are left out
fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
