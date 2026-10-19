package com.example.util

import com.example.data.VaultItem
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class CsvRowError(
    val rowNumber: Int,
    val field: String,
    val message: String
)

data class CsvParseResult(
    val validItems: List<VaultItem>,
    val duplicates: List<VaultItem>,
    val rowErrors: List<CsvRowError>,
    val totalRowsRead: Int,
    val detectedHeaders: List<String>
)

data class ImportSummary(
    val totalRows: Int,
    val insertedCount: Int,
    val duplicateCount: Int,
    val errorCount: Int,
    val errors: List<CsvRowError>
)

object CsvImporter {

    /**
     * Parses standard CSV line handling quoted fields, commas inside quotes,
     * and escaped quotes ("").
     */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    // Escaped quote
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    fun extractHeaders(inputStream: InputStream): List<String> {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val firstLine = reader.readLine() ?: return emptyList()
        return parseCsvLine(firstLine)
    }

    /**
     * Reads CSV from input stream, detects headers, maps columns,
     * validates data, checks duplicates against existing vault items,
     * and strictly associates parsed items with the target household.
     */
    fun parseSpreadsheet(
        inputStream: InputStream,
        targetHouseholdId: String,
        isDemoTarget: Boolean = false,
        existingItems: List<VaultItem> = emptyList(),
        columnMapping: Map<String, Int>? = null
    ): CsvParseResult {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val lines = reader.readLines()

        if (lines.isEmpty()) {
            return CsvParseResult(emptyList(), emptyList(), emptyList(), 0, emptyList())
        }

        // Header line
        val headerLine = lines.firstOrNull { it.isNotBlank() } ?: return CsvParseResult(emptyList(), emptyList(), emptyList(), 0, emptyList())
        val headers = parseCsvLine(headerLine).map { it.lowercase().trim() }

        // Find column indices (use columnMapping override if provided, else detect from headers)
        val titleIndex = columnMapping?.get("title") ?: findColumnIndex(headers, listOf("title", "account name", "asset", "name", "asset name", "item"))
        val categoryIndex = columnMapping?.get("category") ?: findColumnIndex(headers, listOf("category", "type", "asset type", "kind"))
        val institutionIndex = columnMapping?.get("institution") ?: findColumnIndex(headers, listOf("institution", "bank", "company", "provider", "dp", "insurer", "broker"))
        val numberIndex = columnMapping?.get("number") ?: findColumnIndex(headers, listOf("number", "account number", "folio", "policy number", "id", "identifier", "numberorid"))
        val ownerIndex = columnMapping?.get("owner") ?: findColumnIndex(headers, listOf("owner", "owner name", "holder", "account holder"))
        val nomineeIndex = columnMapping?.get("nominee") ?: findColumnIndex(headers, listOf("nominee", "nominee name", "beneficiary"))
        val relationIndex = columnMapping?.get("relation") ?: findColumnIndex(headers, listOf("relation", "relationship", "nominee relation"))
        val verifiedIndex = columnMapping?.get("verified") ?: findColumnIndex(headers, listOf("verified", "nominee verified", "status"))
        val locationIndex = columnMapping?.get("location") ?: findColumnIndex(headers, listOf("location", "physical location", "locker", "file", "folder"))
        val remarksIndex = columnMapping?.get("remarks") ?: findColumnIndex(headers, listOf("remarks", "notes", "description", "note"))
        val detailsIndex = columnMapping?.get("details") ?: findColumnIndex(headers, listOf("details", "detailsstring", "ifsc", "branch", "info", "extra"))

        val validItems = mutableListOf<VaultItem>()
        val duplicates = mutableListOf<VaultItem>()
        val rowErrors = mutableListOf<CsvRowError>()

        // Build duplicate check set from existing items in target household
        val existingIdentifierKeys = existingItems
            .filter { it.isDemo == isDemoTarget }
            .map { makeDuplicateKey(it.numberOrId, it.title, it.institution) }
            .toMutableSet()

        var rowNum = 1
        val dataLines = lines.drop(lines.indexOf(headerLine) + 1)

        for (line in dataLines) {
            rowNum++
            if (line.isBlank()) continue

            val tokens = parseCsvLine(line)
            if (tokens.all { it.isBlank() }) continue

            fun getVal(index: Int): String = if (index in tokens.indices) tokens[index].trim() else ""

            val title = if (titleIndex != -1) getVal(titleIndex) else ""
            val rawCategory = if (categoryIndex != -1) getVal(categoryIndex) else ""
            val institution = if (institutionIndex != -1) getVal(institutionIndex) else ""
            val number = if (numberIndex != -1) getVal(numberIndex) else ""
            val owner = if (ownerIndex != -1) getVal(ownerIndex) else ""
            val nominee = if (nomineeIndex != -1) getVal(nomineeIndex) else ""
            val relation = if (relationIndex != -1) getVal(relationIndex) else ""
            val rawVerified = if (verifiedIndex != -1) getVal(verifiedIndex) else ""
            val location = if (locationIndex != -1) getVal(locationIndex) else ""
            val remarks = if (remarksIndex != -1) getVal(remarksIndex) else ""
            val details = if (detailsIndex != -1) getVal(detailsIndex) else ""

            // Validation: at least one meaningful identifier or title must be present
            if (title.isBlank() && institution.isBlank() && number.isBlank()) {
                rowErrors.add(CsvRowError(rowNum, "Title/Number", "Row lacks account title, institution, or identifier."))
                continue
            }

            val finalTitle = title.ifBlank { "$institution Account".trim().ifBlank { "Imported Asset #$rowNum" } }
            val finalCategory = normalizeCategory(rawCategory, finalTitle, institution)
            val isVerified = rawVerified.equals("true", ignoreCase = true) ||
                    rawVerified.equals("yes", ignoreCase = true) ||
                    rawVerified.equals("verified", ignoreCase = true) ||
                    rawVerified.equals("1", ignoreCase = true)

            val item = VaultItem(
                category = finalCategory,
                title = finalTitle,
                ownerName = owner.ifBlank { "Primary Holder" },
                institution = institution.ifBlank { "Not Specified" },
                numberOrId = number,
                nomineeName = nominee,
                nomineeRelation = relation,
                nomineeVerified = isVerified,
                physicalLocation = location,
                remarks = remarks,
                detailsString = details,
                userId = targetHouseholdId,
                isDemo = isDemoTarget
            )

            // Duplicate detection
            val dupKey = makeDuplicateKey(number, finalTitle, institution)
            if (existingIdentifierKeys.contains(dupKey)) {
                duplicates.add(item)
            } else {
                existingIdentifierKeys.add(dupKey)
                validItems.add(item)
            }
        }

        return CsvParseResult(
            validItems = validItems,
            duplicates = duplicates,
            rowErrors = rowErrors,
            totalRowsRead = rowNum - 1,
            detectedHeaders = headers
        )
    }

    private fun makeDuplicateKey(numberOrId: String, title: String, institution: String): String {
        return if (numberOrId.isNotBlank()) {
            "num:${numberOrId.lowercase().replace(" ", "")}"
        } else {
            "title:${title.lowercase().trim()}|inst:${institution.lowercase().trim()}"
        }
    }

    private fun findColumnIndex(headers: List<String>, possibleNames: List<String>): Int {
        for (name in possibleNames) {
            val idx = headers.indexOfFirst { it == name || it.contains(name) }
            if (idx != -1) return idx
        }
        return -1
    }

    fun normalizeCategory(raw: String, title: String, institution: String): String {
        val text = "${raw.uppercase()} ${title.uppercase()} ${institution.uppercase()}"
        return when {
            text.contains("INSUR") || text.contains("POLICY") || text.contains("LIC") || text.contains("HEALTH") -> "INSURANCE"
            text.contains("INVEST") || text.contains("DEMAT") || text.contains("SHARE") || text.contains("MUTUAL") || text.contains("MF") || text.contains("ZERODHA") || text.contains("STOCK") || text.contains("PPF") -> "INVESTMENT"
            text.contains("LOCKER") || text.contains("SAFE") || text.contains("VAULT") -> "LOCKER"
            text.contains("PROP") || text.contains("LAND") || text.contains("FLAT") || text.contains("HOUSE") || text.contains("REGIST") -> "PROPERTY"
            text.contains("CARD") || text.contains("CREDIT") || text.contains("DEBIT") -> "CARD"
            text.contains("LOAN") || text.contains("LIABIL") || text.contains("DEBT") || text.contains("EMI") -> "LIABILITY"
            text.contains("DOC") || text.contains("AADHAAR") || text.contains("PAN") || text.contains("PASSPORT") || text.contains("WILL") -> "DOCUMENT"
            text.contains("TAX") || text.contains("ITR") -> "TAX"
            else -> "BANK"
        }
    }
}
