package com.wlm.freeAir

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    val tvStatus: TextView by lazy { findViewById(R.id.tv_service_status) }
    val tvRuleStatus: TextView by lazy { findViewById(R.id.tv_rule_status) }
    val openService: Button by lazy { findViewById(R.id.bt_open_service) }
    val setBattery: Button by lazy { findViewById(R.id.bt_set_battery) }
    val loadRule: Button by lazy { findViewById(R.id.bt_load_rule) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        setBattery.setOnClickListener {

            try {
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                val intent: Intent
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                } else {
                    intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        FlowBus.with(ServiceEnableEvent::class.java).register(this) {
            refreshServiceStatusUI()
        }
        FlowBus.with(RuleLoadEvent::class.java).register(this) {
            refreshServiceStatusUI()
        }

        loadRule.setOnClickListener {

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    RuleHelper.initRule(resources)
                }
                if (RuleHelper.ruleList.isNullOrEmpty()) {
                    Toast.makeText(this@MainActivity, "自定义规则列表加载失败...", Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(this@MainActivity, "自定义规则列表已加载...", Toast.LENGTH_SHORT).show();
                }
            }
        }


    }

    override fun onResume() {
        super.onResume()
        refreshServiceStatusUI()
    }

    /**
     * 刷新无障碍服务状态的UI
     * */
    private fun refreshServiceStatusUI() {
        if (GuardService.isServiceEnable) {
            tvStatus.text = "跳过广告服务状态：已开启"
//            mToOpenBt.visibility = View.GONE
        } else {
            tvStatus.text = "跳过广告服务状态：未开启"
//            mToOpenBt.visibility = View.VISIBLE
        }
        if (!RuleHelper.ruleList.isNullOrEmpty()) {
            tvRuleStatus.text = "规则加载状态(加载规则后支持更多场景)：已加载"
        } else {
            tvRuleStatus.text = "规则加载状态(加载规则后支持更多场景)：未加载"
        }

    }
}