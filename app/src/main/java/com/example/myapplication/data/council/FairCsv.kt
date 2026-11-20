package com.example.myapplication.data.council

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.Condition
import com.example.myapplication.ui.addItem.ListingValidator
import org.apache.commons.csv.CSVFormat
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.util.Locale

data class FairRow(
    val ref: String, val title: String, val description: String,
    val category: Category, val condition: Condition
)

object FairCsv {
    private val headers = listOf("ref", "title", "description", "category", "condition")
    private val reference = Regex("[A-Z0-9][A-Z0-9_-]{0,39}")
    private const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_ROWS = 500

    fun read(input: InputStream): List<FairRow> {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(bytes.size() + count <= MAX_BYTES) { "Choose a CSV file up to 2 MB." }
            bytes.write(buffer, 0, count)
        }
        val csv = try {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString().removePrefix("\uFEFF")
        } catch (_: CharacterCodingException) {
            throw IllegalArgumentException("Save the file as CSV UTF-8 in Excel, then choose it again.")
        }
        val separator = if (';' in csv.lineSequence().first()) ';' else ','
        val format = CSVFormat.DEFAULT.builder().setDelimiter(separator).setIgnoreEmptyLines(false).build()
        val rows = mutableListOf<FairRow>()
        val seen = mutableSetOf<String>()
        format.parse(csv.reader()).use { parser ->
            try {
                val records = parser.iterator()
                require(records.hasNext() && records.next().map { it.trim().lowercase(Locale.ROOT) } == headers) {
                    "Use the template header: ${headers.joinToString(";")}."
                }
                for (record in records) {
                    val where = "Row ${record.recordNumber}"
                    if (record.all { it.isBlank() }) continue
                    require(rows.size < MAX_ROWS) { "Choose up to $MAX_ROWS listings per file." }
                    require(record.size() == headers.size) { "$where needs five columns. Quote text containing a separator." }
                    val (rawRef, title, description, categoryName, conditionName) = record.map { it.trim() }
                    val ref = rawRef.uppercase(Locale.ROOT)
                    require(reference.matches(ref)) { "$where needs a reference of 1–40 letters A–Z, digits, dashes or underscores, starting with a letter or digit." }
                    require(seen.add(ref)) { "$where repeats reference $ref. Give each item its own reference." }
                    val category = Category.entries.find { it.name.equals(categoryName, true) }
                    val condition = Condition.entries.find { it.name.equals(conditionName, true) || it.label.equals(conditionName, true) }
                    val errors = ListingValidator.validate(title, description, category, condition)
                    require(errors.isEmpty()) { "$where ($ref): ${errors.values.joinToString(". ")}." }
                    rows += FairRow(ref, title, description, requireNotNull(category), requireNotNull(condition))
                }
            } catch (e: java.io.UncheckedIOException) {
                throw IllegalArgumentException("Invalid CSV quoting near line ${parser.currentLineNumber}.", e)
            }
        }
        require(rows.isNotEmpty()) { "Fill in at least one listing below the header." }
        return rows
    }
}
