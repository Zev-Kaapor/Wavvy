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

// Objects of a list, the entries that are not objects are left out
fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
