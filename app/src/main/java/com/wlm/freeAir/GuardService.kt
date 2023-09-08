package com.wlm.freeAir

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern


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
        RuleHelper.initRule(resources)
        instance = this
        FlowBus.postEvent(ServiceEnableEvent())
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

//    private var lastPackageName: String? = null
//    private var lastTime: Long = 0
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == null) return
        if (event.packageName.toString() == "com.wlm.freeAir") return
        if (event.packageName.toString() == "com.miui.home") return
        try {
            val pattern = "^com.android.*"
            if (Pattern.matches(pattern, event.packageName.toString())) return
//            if (event.packageName.toString() == "android") return
            if (event.packageName.toString().contains("inputmethod")) return
            if (event.className.toString().contains("android.view.")) return

//            if (lastPackageName == null) {
//                this.lastPackageName = event.packageName.toString()
//                this.lastTime = 0
//            } else {
//                if (this.lastPackageName.equals(event.packageName.toString())) {
//                    this.lastTime++
//                    if (this.lastTime >= 30) {
////                        Log.d(TAG, "onAccessibilityEvent: 不执行...")
//                        return
//                    }
//                } else {
//                    this.lastPackageName = event.packageName.toString()
//                    this.lastTime = 0
//                    Log.d(TAG, "onAccessibilityEvent: " + this.lastPackageName + "新开启应用...重置时间")
//                }
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
//        Log.e(TAG, "${event.eventType}")

//        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            try {
                skip(event)
            } catch (e: Exception) {
                Log.e(TAG, "skip error", e)
            }
//        }



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

    private fun skip(event: AccessibilityEvent) {
        val node = event.source
        node ?: return
        Log.d(TAG, "try skip ${event.packageName} ${event.packageName.hashCode()} ${RuleHelper.ruleMap.contains(event.packageName.hashCode().toString())}")

        MainScope().launch {
            withContext(Dispatchers.Default) {
                if (!skipConfig(event)) {
                    skipText(node)
                }
            }
        }
    }

    private suspend fun skipConfig(event: AccessibilityEvent): Boolean {

        val node = event.source
        node ?: return false
        if (node.packageName.isNullOrEmpty()) {
            return false
        }
        RuleHelper.ruleMap[event.packageName.hashCode().toString()]?.popupRules?.forEach { rule ->
            try {
                if (search(node, rule.id) != null) {
                    val actionNode = search(node, rule.action)
                    if (actionNode != null) {
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "skipConfig ${node.packageName} ${rule.id} ${rule.action}")
                            skipClick(actionNode)
                        }
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.i(TAG, "error ${rule.id} ${rule.action}")
            }
        }
        return false
    }

    private fun search(node: AccessibilityNodeInfo, filter: String): AccessibilityNodeInfo? {
        node.findAccessibilityNodeInfosByText(filter).takeUnless { e -> e.isNullOrEmpty() }?.let {
            return it[0]
        }
        node.findAccessibilityNodeInfosByViewId("${node.packageName}:id/$filter")
            .takeUnless {  e -> e.isNullOrEmpty() }?.let { return it[0] }

        return null
    }

    private suspend fun skipText(nodeInfo: AccessibilityNodeInfo?) {
        nodeInfo?:return

        Log.d(TAG, "try skipText  ${nodeInfo.packageName}")
        val accessibilityNodeInfoList =
            nodeInfo.findAccessibilityNodeInfosByText("跳过").takeUnless { e -> e.isNullOrEmpty() }
        if (!accessibilityNodeInfoList.isNullOrEmpty()) {
            val findNodeInfo = accessibilityNodeInfoList[0]
            var text = findNodeInfo.text
            if (text.length <= 10) {
                text = text.toString().replace(" ", "")
                val pattern = "^[0-9]跳过.*"
                val pattern002 = "^跳过[\\s\\S]{0,5}"
                val pattern003 = "^[0-9][sS秒]跳过.*"
                if (Pattern.matches(pattern, text) || Pattern.matches(
                        pattern002,
                        text
                    ) || Pattern.matches(pattern003, text)
                ) {
                    Log.d(TAG, "skipText 找到了跳过 ${findNodeInfo.packageName}")
                    withContext(Dispatchers.Main) {
                        skipClick(findNodeInfo)
                    }
                }
            }
        } else {
//            Log.d(TAG, "findJumpText: 找不到跳过")
        }
    }

    private fun skipClick(nodeInfo: AccessibilityNodeInfo) {
        val isClick: Boolean = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val rect = Rect()
        nodeInfo.getBoundsInScreen(rect)
//        dispatchGesture(new GestureDescription())
        //        dispatchGesture(new GestureDescription())
        if (!isClick) {
            onTouch(rect)
        }
    }
    private fun onTouch(rect: Rect) {
        Log.d(TAG, "====onTouch====")
        if (rect.bottom > 0 && rect.right > 0) {
            val rectHeight = rect.bottom - rect.top
            val rectWidth = rect.right - rect.left
            rect.left = rect.left + rectWidth / 3
            rect.top = rect.top + rectHeight / 3
        }
        val b = dispatchGesture(createClick(rect.left.toFloat(), rect.top.toFloat()), object : GestureResultCallback() {
            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.d(TAG, "onCancelled====")
            }

            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "onCompleted===")
            }
        }, null)
        Log.d(TAG, "dispatchGesture====>>>$b")
    }

    private fun createClick(x: Float, y: Float): GestureDescription {
        // for a single tap a duration of 1 ms is enough
        val DURATION = 1
        val clickPath = Path()
        clickPath.moveTo(x, y)
        val clickStroke = StrokeDescription(clickPath, 0, DURATION.toLong())
        val clickBuilder = GestureDescription.Builder()
        clickBuilder.addStroke(clickStroke)
        return clickBuilder.build()
    }


}