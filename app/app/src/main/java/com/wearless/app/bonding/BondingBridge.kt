package com.wearless.app.bonding

import com.wearless.app.model.AppLink
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Reflection bridge to claude-owned module com.wearless.bonding.
 *
 * The bonding contract types (BondingEngine, BondingConfig, LinkHandle,
 * LinkKind, LinkStats, BondingSnapshot, Scheduler) are owned by claude and are
 * NOT redefined here. Until that module ships in the APK, this bridge reports
 * "engine not wired" and every measured value in the UI stays NOT MEASURED.
 *
 * When com.wearless.bonding is on the classpath, this bridge locates
 * an implementation via BondingEngineFactory discovery (a class named
 * com.wearless.bonding.BondingEngineFactory with a no-arg create() returning
 * BondingEngine), converts app-side [com.wearless.app.link.AndroidLinkHandle]s
 * into bonding LinkHandles through java.lang.reflect.Proxy, and forwards
 * start/stop/stats.
 */
object BondingBridge {

    data class BridgeSnapshot(
        val links: List<AppLink>,
        val aggregatedBps: Long?,
        val isMeasured: Boolean,
        val failoverActive: Boolean
    ) {
        companion object {
            val NOT_MEASURED = BridgeSnapshot(emptyList(), null, false, false)
        }
    }

    sealed class State {
        object EngineNotWired : State()
        object Running : State()
        object Stopped : State()
        data class Failed(val message: String) : State()
    }

    private const val ENGINE_FACTORY = "com.wearless.bonding.BondingEngineFactory"
    private const val IFACE_ENGINE = "com.wearless.bonding.BondingEngine"
    private const val IFACE_HANDLE = "com.wearless.bonding.LinkHandle"

    val state = MutableStateFlow<State>(State.Stopped)
    private var engine: Any? = null

    fun isEngineAvailable(): Boolean = try {
        Class.forName(ENGINE_FACTORY)
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    /**
     * Attempt to start the bonding engine with the given config. Never fakes
     * success: if the module is absent or start fails, returns the failure.
     */
    fun start(
        serverAddress: String,
        keyRef: String,
        schedulerWlb: Boolean,
        links: List<com.wearless.app.link.AndroidLinkHandle>
    ): Result<Unit> {
        if (!isEngineAvailable()) {
            state.value = State.EngineNotWired
            return Result.failure(IllegalStateException(
                "Bonding engine (com.wearless.bonding) is not wired into this build. " +
                "Tunnel is NOT up. No speeds exist."))
        }
        return try {
            val factory = Class.forName(ENGINE_FACTORY).getDeclaredConstructor().newInstance()
            engine = factory.javaClass.getMethod("create").invoke(factory)
            val handleIface = Class.forName(IFACE_HANDLE)
            val proxies = links.map { h ->
                Proxy.newProxyInstance(
                    handleIface.classLoader, arrayOf(handleIface),
                    java.lang.reflect.InvocationHandler { _, method, _ ->
                        when (method.name) {
                            "getId" -> h.id
                            "getKind" -> enumConstant(method.returnType, h.kindValue)
                            "hashCode" -> h.id.hashCode()
                            "equals" -> false
                            "toString" -> "AndroidLinkHandle(${h.id})"
                            else -> null
                        }
                    })
            }
            val configCtor = Class.forName("com.wearless.bonding.BondingConfig")
                .getConstructor(String::class.java, String::class.java)
            val config = configCtor.newInstance(serverAddress, keyRef)
            val result = Class.forName(IFACE_ENGINE).getMethod(
                "start", Class.forName("com.wearless.bonding.BondingConfig"), List::class.java
            ).invoke(engine, config, proxies) as Result<*>
            if (result.isSuccess) {
                state.value = State.Running
                Result.success(Unit)
            } else {
                state.value = State.Failed(result.exceptionOrNull()?.message ?: "engine start failed")
                Result.failure(result.exceptionOrNull() ?: IllegalStateException("engine start failed"))
            }
        } catch (t: Throwable) {
            state.value = State.Failed(t.message ?: t.javaClass.name)
            Result.failure(t)
        }
    }

    fun stop() {
        try {
            engine?.let { e ->
                Class.forName(IFACE_ENGINE).getMethod("stop").invoke(e)
            }
        } catch (_: Throwable) {
            // engine absent or already stopped
        }
        engine = null
        state.value = State.Stopped
    }

    /**
     * Latest snapshot converted to app-side values. Without a wired engine this
     * always returns NOT_MEASURED data — no invented numbers.
     */
    fun snapshot(links: List<AppLink>): BridgeSnapshot {
        if (engine == null || !isEngineAvailable()) return BridgeSnapshot.NOT_MEASURED
        return try {
            // BondingSnapshot is a data class; read its fields reflectively.
            val statsFlow = Class.forName(IFACE_ENGINE).getMethod("stats").invoke(engine)
            // Without a coroutine collector on the engine's Flow type we cannot
            // pull a value generically; the concrete wiring happens when claude's
            // module lands. Until then: not measured.
            BridgeSnapshot.NOT_MEASURED
        } catch (_: Throwable) {
            BridgeSnapshot.NOT_MEASURED
        }
    }

    private fun enumConstant(enumType: Class<*>, ordinal: Int): Any? =
        enumType.enumConstants?.getOrNull(ordinal)

    fun stateFlow(): StateFlow<State> = state
}
