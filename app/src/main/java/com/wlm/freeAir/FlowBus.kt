package com.wlm.freeAir

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * bus总线
 * flow eventBus
 */
object FlowBus {
    const val TAG = "FlowBus"
    private val busMap = hashMapOf<Class<*>, EventBus<*>>()

    /**
     * 根据类获取指定bus
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> with(clazz: Class<T>): EventBus<T> {
        var bus = busMap[clazz]
        if (bus == null) {
            bus = EventBus(clazz)
            busMap[clazz] = bus
        }
        return bus as EventBus<T>
    }

    fun with(vararg clazz: Class<*>): List<EventBus<*>> {
        val busList = mutableListOf<EventBus<*>>()
        clazz.forEach {
            var bus = busMap[it]
            if (bus == null) {
                bus = EventBus(it)
                busMap[it] = bus
            }
            busList.add(bus)
        }
        return busList
    }

    private fun getBusClazz(event: Any): Class<Any>? {
        var clazz: Class<Any>? = event.javaClass
        var whileCount = 0
        while (clazz != null && !busMap.containsKey(clazz)) {
            whileCount++
            clazz = clazz.superclass
        }
        return clazz
    }

    suspend fun suspendPostEvent(event: Any, delayMillis: Long = 0, isSticky: Boolean = false) {
        val clazz = getBusClazz(event)
        if (clazz != null) {
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            with(clazz).postEvent(event)
        } else if (isSticky) {
            with(event.javaClass).postEvent(event)
        }
    }

    /**
     * @param isSticky 是否是沾性事件
     */
    @JvmOverloads
    fun postEvent(event: Any, lifecycleOwner: LifecycleOwner? = null, delayMillis: Long = 0, isSticky: Boolean = false) {
        if (lifecycleOwner != null) {
            lifecycleOwner.lifecycleScope.launchWhenCreated {
                suspendPostEvent(event, delayMillis, isSticky)
            }
        } else {
            MainScope().launch {
                suspendPostEvent(event, delayMillis, isSticky)
            }
        }
    }

    /**
     * 针对某一类event的bus
     */
    class EventBus<T>(private val clazz: Class<T>) : DefaultLifecycleObserver {

        private val events = MutableSharedFlow<T>()

        private val jobMap = hashMapOf<LifecycleOwner, Job>()

        /**
         * 注册事件
         */
        fun register(lifecycleOwner: LifecycleOwner, action: (T) -> Unit) {
            lifecycleOwner.lifecycle.addObserver(this)
            //activity created后接收事件
            val job = lifecycleOwner.lifecycleScope.launchWhenCreated {
                events.collectLatest {
                    try {
                        action.invoke(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            jobMap[lifecycleOwner] = job
        }

        fun unregister(owner: LifecycleOwner){
            jobMap[owner]?.cancel()
            jobMap.remove(owner)
            if (jobMap.isEmpty()) {
                busMap.remove(clazz)
            }
        }

        fun registerAnyway(action: (T) -> Unit) {
            register(ProcessLifecycleOwner.get(), action)
        }

        suspend fun postEvent(event: T) {
            events.emit(event)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            //destroy 生命周期后将协程取消
            unregister(owner)
        }
    }
}