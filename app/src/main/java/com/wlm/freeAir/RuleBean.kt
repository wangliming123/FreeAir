package com.wlm.freeAir

data class RuleEntity(
    val popupRules: ArrayList<RuleDetail>
)

data class RuleDetail(
    val id: String,
    val action: String
)