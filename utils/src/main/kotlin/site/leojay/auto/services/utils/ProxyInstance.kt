package site.leojay.auto.services.utils

import site.leojay.auto.services.utils.AutoProxy.autoInvoke
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 代理实例
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
abstract class ProxyInstance<T : Any>(builder: ProxyHelperBuilder<T>) : InvocationHandler {

    private val registerInstance: T = builder.build()
//    private val defaultInstance = defaultSDKEntity()

    companion object {
        private val log = Logger.getLogger(ProxyInstance::class.java.name)
    }

    init {
        builder.registerThrowListener {
            log.log(Level.SEVERE, "proxy invoke error", it)
        }
    }

    protected abstract fun defaultSDKEntity(): T

    override fun invoke(
        proxy: Any?,
        method: Method,
        args: Array<out Any?>?,
    ): Any? {
        // 先执行默认实例，调用后返回
        return method.autoInvoke(defaultSDKEntity(), args).also {
            // 默认实例调用后，调用注册实例
            method.autoInvoke(registerInstance, args)
        }
    }
}