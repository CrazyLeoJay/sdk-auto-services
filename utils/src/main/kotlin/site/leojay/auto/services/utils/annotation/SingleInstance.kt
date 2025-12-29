package site.leojay.auto.services.utils.annotation

import kotlin.reflect.KClass

/**
 * 单例
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SingleInstance(
    val value: String,
    val implInterface: KClass<*>,
    val packagePath: String = "",
    val packageSuffix: String = "auto",
)