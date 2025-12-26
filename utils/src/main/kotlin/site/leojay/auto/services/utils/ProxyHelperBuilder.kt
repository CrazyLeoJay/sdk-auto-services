package site.leojay.auto.services.utils

import java.lang.String
import kotlin.Any
import kotlin.apply
import kotlin.reflect.KClass

/**
 * 代理辅助工具
 *
 * 使用场景：当有多个接口，或者多个工厂需要统一调用时，
 * 即所有的接口做一个集合，但每个接口都有对应的实现，并不能合并到一起时，这里使用动态代理加注册的方式，自动识别调用的方法是哪个类的实现。并且调用。
 * 但注意，这些接口的名称不能有重复，否则会有冲突
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class ProxyHelperBuilder<T : Any>(
    interfaceClass: KClass<T>,
    private val modulesHelper: ModulesHelper = ModulesHelper(),
) {
    private var throwListener: ThrowListener? = null
    private val empty = AutoProxy.proxy(interfaceClass.java)
    private val proxyEntity = AutoProxy.proxy(interfaceClass.java) { _, method, args ->
        // 调用接口方法所在的接口类
        val declaringClass = method.declaringClass

        // 获取注册的功能模块
        if (modulesHelper.hasModule(declaringClass.kotlin)) {
            // 如果该功能模块注册过，就直接执行
            // 每个功能模块可以注册多个，并且多次
            modulesHelper.invokeNoReturn(declaringClass.kotlin, method, args)
        } else {
            // 如果执行到这里 说明调用的方法 没有注册该类的实现
            try {
                throw LeojayAutoException(
                    String.format(
                        "接口方法 %s#%s 没有注册实现",
                        declaringClass.getName(),
                        method.name
                    )
                )
            } catch (e: LeojayAutoException) {
                if (null != throwListener) {
                    // 如果有注册处理接口，就不打印
                    throwListener!!.exception(e)
                } else {
                    throw e
                }
            }
        }
        return@proxy AutoProxy.methodInvoke(empty, method, args)
    }

    /**
     * 注册异常监听
     *
     * @param listener 异常问题
     * @return this
     */
    fun registerThrowListener(listener: ThrowListener): ProxyHelperBuilder<T> = apply {
        throwListener = listener
    }

    fun build() = proxyEntity

    companion object {
        /**
         * 注册响应的接口实现到实例中
         * 定义拓展函数，可以指定 K 是 T 的父类，防止异常注入
         *
         * @param key   要注册的事件，接口[.] 要继承这个类接口
         * @param value 注册事件的实现
         * @param <T>   实际调用类型
         * @param <K>   要注册的事件，接口[.] 要继承这个类接口
         * @param <V>   事件 [K]  的具体实现类
         * @return this
         */
        fun <T : K, K : Any, V : K> ProxyHelperBuilder<T>.register(key: KClass<K>, value: V): ProxyHelperBuilder<T> {
            return apply {
                modulesHelper[key] = value
            }
        }

    }
}


/**
 * 异常监听
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
fun interface ThrowListener {
    fun exception(t: LeojayAutoException?)
}
