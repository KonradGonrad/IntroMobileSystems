package com.example.myapplication.data.council

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class FairStatus { NEW, CHANGED, UNCHANGED, RESERVED, DELETED }

data class FairMatch(val row: FairRow, val status: FairStatus, val id: String?, val previous: FairRow?)

class FairPreview(val matches: List<FairMatch>, internal val snapshot: JSONArray) {
    val added = matches.count { it.status == FairStatus.NEW }
    val changed = matches.count { it.status == FairStatus.CHANGED }
    fun skipped(overwrite: Boolean) = matches.size - added - if (overwrite) changed else 0

    companion object {
        fun decode(rows: List<FairRow>, snapshot: JSONArray): FairPreview {
            require(rows.size == snapshot.length()) { "Incomplete import preview." }
            val matches = rows.mapIndexed { index, row ->
                val item = snapshot.getJSONObject(index)
                require(row.ref == item.getString("ref")) { "Unexpected import reference." }
                val previous = item.optJSONObject("previous")?.let {
                    FairRow(row.ref, it.getString("title"), it.getString("description"),
                        Category.valueOf(it.getString("category")), Condition.valueOf(it.getString("condition")))
                }
                FairMatch(row, FairStatus.valueOf(item.getString("status").uppercase(Locale.ROOT)),
                    if (item.isNull("id")) null else item.getString("id"), previous)
            }
            return FairPreview(matches, snapshot)
        }
    }
}

internal fun List<FairRow>.toEntries(): JSONArray = JSONArray().apply {
    for (row in this@toEntries) put(JSONObject().put("ref", row.ref).put("title", row.title)
        .put("description", row.description).put("category", row.category.name).put("condition", row.condition.name))
}
