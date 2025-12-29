package site.leojay.auto.services.utils

import site.leojay.auto.services.utils.AutoProxy.autoInvoke
import java.lang.reflect.InvocationHandler
import kotlin.reflect.KClass

/**
 * SDK回调
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class SDKCallback(private val modulesHelper: () -> ModulesHelper) {

    fun <T : Any> callback(klass: KClass<T>): T {
        val proxy = AutoProxy.proxy(klass.java)
        return AutoProxy.proxy(klass.java, InvocationHandler { _, method, array ->
            modulesHelper().invokeNoReturn(klass, method, array)
            return@InvocationHandler method.autoInvoke(proxy, array)
        })
    }
}