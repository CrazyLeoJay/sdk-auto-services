package site.leojay.auto.services.utils

import site.leojay.auto.services.utils.AutoProxy.autoInvoke
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * 模块助手
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class ModulesHelper(
    modules: List<Any> = listOf(),
    moduleTypes: List<KClass<*>> = listOf(),
) {

    private val moduleData: MutableMap<KClass<*>, MutableList<Any>> = moduleTypes.make(modules)

    companion object {
        private fun List<KClass<*>>.make(modules: List<Any>): MutableMap<KClass<*>, MutableList<Any>> {
            val map = mutableMapOf<KClass<*>, MutableList<Any>>()
            for (klass in this) {
                map[klass] = modules.mapNotNull {
                    if (klass.isInstance(it)) it else null
                }.toMutableList()
            }
            return map
        }
    }

    operator fun set(clazz: KClass<*>, module: Any) {
        if (moduleData.keys.contains(clazz)) {
            if (!moduleData[clazz]!!.contains(module)) {
                moduleData[clazz]!!.add(module)
            }
        } else {
            moduleData[clazz] = mutableListOf(module)
        }
    }

    /**
     * 实例化但没有返回值
     * 只是调用，不做返回，由于每个类型会有多个实现，所以我们返回值时，建议使用接口返回，而不是返回值
     */
    fun invokeNoReturn(klass: KClass<*>, method: Method, args: Array<out Any?>? = null) {
        moduleData[klass]?.forEach {
            method.autoInvoke(it, args = args)
        }
    }

    fun hasModule(kClass: KClass<*>): Boolean {
        return moduleData.keys.contains(kClass)
    }

}