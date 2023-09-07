package com.wlm.freeAir

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GuardService : AccessibilityService() {

    companion object {
        private const val TAG = "GuardService"
        var instance: GuardService? = null  // 单例
        val isServiceEnable: Boolean get() = instance != null   // 判断无障碍服务是否可用
    }

    override fun onInterrupt() {

    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        instance = this
        FlowBus.postEvent(ServiceEnableEvent())
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * 获得当前视图根节点
     * */
    private fun getCurrentRootNode() = try {
        rootInActiveWindow
    } catch (e: Exception) {
        e.message?.let { Log.e(TAG, it) }
        null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        MainScope().launch {

            withContext(Dispatchers.Default) {
                val node1 = searchNode("跳过")
                if (node1 != null) {
                    Log.e(TAG, "找到跳过")
                    withContext(Dispatchers.Main) {
                        node1.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }
                RuleHelper.ruleList?.forEach {
                    it.popupRules.forEach { rule ->
                        if (searchNode(rule.id) != null) {
                            val node = searchNode(rule.action)
                            if (node != null) {
                                withContext(Dispatchers.Main) {
                                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                }
                            }
                        }
                    }
                }
            }
        }

//        event?.let {
//            // 在这里写跳过广告的逻辑
//            Log.d(TAG, "$it")
//            // 如果查找包含跳过按钮的结点列表不为空，取第一个，然后输出
//            getCurrentRootNode()?.findAccessibilityNodeInfosByText("跳过")
//                .takeUnless { e -> e.isNullOrEmpty() }?.get(0)?.let { node ->
//                    Log.d(TAG, "检测到跳过广告结点：$node")
//                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                }
//        }
    }

    /**
     * 递归遍历查找匹配文本或id结点
     * 结点id的构造规则：包名:id/具体id
     * */
    private fun searchNode(filter: String): AccessibilityNodeInfo? {
        val rootNode = getCurrentRootNode()
        if (rootNode != null) {
            rootNode.findAccessibilityNodeInfosByText(filter).takeUnless { e -> e.isNullOrEmpty() }
                ?.let { return it[0] }
            if (!rootNode.packageName.isNullOrBlank()) {
                rootNode.findAccessibilityNodeInfosByViewId("${rootNode.packageName}:id/$filter")
                    .takeUnless { it.isNullOrEmpty() }?.let { return it[0] }
            }
        }
        return null
    }

}