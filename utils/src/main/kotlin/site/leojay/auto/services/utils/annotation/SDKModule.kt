package site.leojay.auto.services.utils.annotation

import kotlin.reflect.KClass

/**
 * SDK 模块标记
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SDKModule
