package site.leojay.auto.services.utils.annotation

import kotlin.reflect.KClass

/**
 * 构建单例对象
 *
 * @author leojay`Fu
 * create for 2025/12/22
 *
 * @param value 单例名称
 * @param implInterface 实现的接口类
 * @param packagePath 包路径，如果没有设置就使用注册类地址
 * @param proxy 是否使用代理实现，即使用 {@link java.lang.reflect.InvocationHandler} 实现效果
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SDKModuleSingleInstance(
    val value: String,
    val implInterface: KClass<*>,
    val packagePath: String = "",
    val proxy: Boolean = false,
)
