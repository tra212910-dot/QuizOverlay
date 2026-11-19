package com.example.quizoverlay

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Q(val text: String, val options: List<String>, val answer: Int)

object QuizStore {
    private const val KEY = "questions"
    private fun prefs(c: Context) =
        c.getSharedPreferences("quiz", Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<Q> {
        val list = mutableListOf<Q>()
        val arr = JSONArray(prefs(c).getString(KEY, "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val opts = o.getJSONArray("options")
            list.add(Q(
                o.getString("text"),
                List(opts.length()) { opts.getString(it) },
                o.getInt("answer")
            ))
        }
        return list
    }

    fun save(c: Context, list: List<Q>) {
        val arr = JSONArray()
        list.forEach { q ->
            arr.put(JSONObject()
                .put("text", q.text)
                .put("options", JSONArray(q.options))
                .put("answer", q.answer))
        }
        prefs(c).edit().putString(KEY, arr.toString()).apply()
    }
}
