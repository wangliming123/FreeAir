package com.wlm.freeAir

import android.content.res.Resources
import android.util.Log
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object RuleHelper {

    private const val TAG = "GuardService"


    val ruleMap = mutableMapOf<String, RuleEntity>()

    fun initRule(resources: Resources) {

        readJsonToRuleList(resources)
    }

    /**
     * 读取json文件生成规则实体列表
     * */
    private fun readJsonToRuleList(resources: Resources) {
        ruleMap.clear()
        try {
            val inputStream = resources.openRawResource(R.raw.rule)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            reader.use {
                var line: String? = it.readLine()
                while (line != null) {
                    sb.append(line)
                    line = it.readLine()
                }
            }
            val ruleBean = sb.toString().toObject(RuleBean::class.java)
            Log.i(TAG, "自定义规则加载成功 ${ruleBean.list.size}")

            if (ruleBean.list.isEmpty()) return
            ruleBean.list.forEach {
                it.forEach { (t, u) ->
                    ruleMap[t] = u.toObject(RuleEntity::class.java)
                }
            }
//            val jsonArray = JSONArray(sb.toString())
//            for (i in 0 until jsonArray.length()) {
//                val jsonObject = jsonArray.getJSONObject(i)
//                val keys = jsonObject.keys()
//                while (keys.hasNext()) {
//                    val key = keys.next()
//                    val value = jsonObject.getString(key)
//                    val ruleEntityJson = JSONObject(value)
//                    val popupRules = ruleEntityJson.getJSONArray("popup_rules")
//                    val ruleEntity = RuleEntity(arrayListOf())
//                    for (j in 0 until popupRules.length()) {
//                        val ruleObject = popupRules.getJSONObject(j)
//                        val ruleDetail = RuleDetail(ruleObject.getString("id"), ruleObject.getString("action"))
//                        ruleEntity.popupRules.add(ruleDetail)
//                    }
//                    ruleEntityList.add(ruleEntity)
//                }
//            }
            Log.d(TAG, "自定义规则列表已加载...${ruleMap.size}")
//
            FlowBus.postEvent(RuleLoadEvent())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "自定义规则列表加载失败...")
        } catch (e: JSONException) {
//            Log.d(TAG, "$ruleEntityList")
            e.printStackTrace()
            Log.e(TAG, "自定义规则列表加载失败...")
        } catch (e: JsonParseException) {
            e.printStackTrace()
            Log.e(TAG, "自定义规则列表加载失败...")

        }
    }


}