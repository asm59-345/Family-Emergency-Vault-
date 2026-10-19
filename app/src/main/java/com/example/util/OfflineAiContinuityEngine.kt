package com.example.util

import com.example.data.ImportantContact
import com.example.data.VaultItem

/**
 * Compatibility wrapper delegating emergency and succession questions
 * to EmergencyKnowledgeBase without fake AI claims or cloud calls.
 */
object OfflineAiContinuityEngine {

    data class AiResponse(
        val answer: String,
        val suggestedFollowups: List<String> = emptyList(),
        val confidenceBadge: String = "Verified Legal & Succession Reference"
    )

    fun generateResponse(
        userQuery: String,
        userName: String,
        nomineeName: String,
        nomineeRelation: String,
        nomineePhone: String,
        contacts: List<ImportantContact>,
        vaultItems: List<VaultItem>,
        isHindi: Boolean = false
    ): AiResponse {
        val result = EmergencyKnowledgeBase.searchGuide(
            query = userQuery,
            userName = userName,
            nomineeName = nomineeName,
            nomineeRelation = nomineeRelation,
            nomineePhone = nomineePhone,
            contacts = contacts,
            vaultItems = vaultItems,
            isHindi = isHindi
        )
        return AiResponse(
            answer = result.answer,
            suggestedFollowups = result.suggestedFollowups,
            confidenceBadge = result.sourceBadge
        )
    }
}
