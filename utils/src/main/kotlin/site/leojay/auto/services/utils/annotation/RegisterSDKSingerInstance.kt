package site.leojay.auto.services.utils.annotation

import kotlin.reflect.KClass

/**
 * 构建单例对象
 *
 * @author leojay`Fu
 * create for 2025/12/22
 *
 * @param value 单例名称
 * @param implInterface 实现的接口类，如果没有就创建一个默认的
 * @param innerInterface 内部调用的接口类，如果没有指定就生成一个默认的
 * @param packagePath 包路径，如果没有设置就使用注册类地址
 * @param proxy 是否使用代理实现，即使用 {@link java.lang.reflect.InvocationHandler} 实现效果
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class RegisterSDKSingerInstance(
    val value: String,
    val implInterface: KClass<*> = Any::class,
    val innerInterface: KClass<*> = Any::class,
    val packagePath: String = "",
    val packageSuffix: String = "auto",
    val proxy: Boolean = false,
)
