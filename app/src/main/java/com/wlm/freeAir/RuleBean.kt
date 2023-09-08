package com.wlm.freeAir

data class RuleBean(
    val list: List<Map<String, String>>
)
data class RuleEntity(
    val popupRules: ArrayList<RuleDetail>,
    val click_way_popup: Int?,
)

data class RuleDetail(
    val id: String,
    val action: String
)