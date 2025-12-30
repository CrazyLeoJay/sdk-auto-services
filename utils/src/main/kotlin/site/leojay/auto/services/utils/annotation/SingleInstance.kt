package site.leojay.auto.services.utils.annotation

import kotlin.reflect.KClass

/**
 * 单例
 *
 * @author leojay`Fu
 * create for 2025/12/22
 *
 * @param value SDK 类型
 * @param implInterface 两种情况，
 *                      1、如果是接口类，可以直接写入
 *                      2、如果接口类是通过 @RegisterSDKSingeInstance 注解生成的，可以返回 @RegisterSDKSingeInstance 注解的类，
 *                      如果直接引入生成的类，会导致注解器找不到类，所以可以通过被注解的主类来找到需要返回的SDKFactory接口
 * @param packagePath 包路径，如果为空就使用注解类的路径
 * @param packageSuffix 默认在包路径后面添加后缀
 * @param methodName 单例获取的方法
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SingleInstance(
    val value: String,
    val implInterface: KClass<*>,
    val packagePath: String = "",
    val packageSuffix: String = "auto",
    val methodName: String = "instance",
)