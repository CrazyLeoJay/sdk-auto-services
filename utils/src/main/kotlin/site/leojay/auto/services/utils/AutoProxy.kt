package site.leojay.auto.services.utils

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * 反编译代理工具
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
object AutoProxy {


    @JvmStatic
    fun <T> instance(inner: Class<T>, className: String, classMethod: String): T? {
        try {
            return instanceThrow(inner, className, classMethod)
        } catch (t: LeojayAutoException) {
            t.printStackTrace()
        }
        return null
    }

    @JvmStatic
    fun <T> instanceThrow(inner: Class<T>, className: String, classMethod: String): T {
        try {
            return Class.forName(className)
                .getMethod(classMethod)
                .let { it.invoke(null) as T }
        } catch (t: Throwable) {
            when (t) {
                is ClassNotFoundException,
                is NoSuchMethodException,
                is SecurityException,
                is IllegalAccessException,
                is IllegalArgumentException,
                is InvocationTargetException,
                    -> throw LeojayAutoException("实例化异常", t)

                else -> throw t
            }
        }
    }

    @JvmStatic
    fun <T> proxy(inner: Class<T>, invocationHandler: InvocationHandler): T {
        return Proxy.newProxyInstance(AutoProxy::class.java.classLoader, arrayOf(inner), invocationHandler) as T
    }

    /**
     * 创建一个空的代理实体
     * 传入接口类后，
     */
    @JvmStatic
    fun <T> proxy(inner: Class<T>): T {
        return proxy(inner, InvocationHandler { any, method, array ->
            val returnType = method.returnType
            val value = method.getDefaultValue()
            if (null != value) return@InvocationHandler value
            val typeName = returnType.getName()
            return@InvocationHandler when (typeName) {
                Int::class.javaPrimitiveType!!.getName() -> 0
                Long::class.javaPrimitiveType!!.getName() -> 0L
                Double::class.javaPrimitiveType!!.getName() -> 0.0
                Float::class.javaPrimitiveType!!.getName() -> 0.0f
                Boolean::class.javaPrimitiveType!!.getName() -> false
                else -> null
            }
        })
    }

    @JvmStatic
    fun methodInvoke(any: Any, method: Method, args: Array<out Any?>?): Any? {
        method.setAccessible(true)
        val invoke = if (args.isNullOrEmpty()) {
            method.invoke(any)
        } else {
            method.invoke(any, *args)
        }
        method.setAccessible(false)
        return invoke
    }

    @JvmStatic
    fun Method.autoInvoke(any: Any, args: Array<out Any?>?): Any? {
        return methodInvoke(any, this, args)
    }

}

