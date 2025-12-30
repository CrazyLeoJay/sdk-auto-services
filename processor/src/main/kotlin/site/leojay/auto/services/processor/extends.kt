package site.leojay.auto.services.processor

import site.leojay.auto.services.utils.annotation.RegisterSDKSingeInstance
import site.leojay.auto.services.utils.annotation.SDKModuleSingleInstance
import javax.lang.model.element.Element
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror


/**
 * 拓展
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */

fun SDKModuleSingleInstance.getPackagePath(element: Element): String {
    if (this.packagePath.isBlank()) {
        return element.toString().replace("." + element.simpleName, "")
    }
    return this.packagePath
}

fun RegisterSDKSingeInstance.getPackagePath(element: Element): String {
    if (this.packagePath.isBlank()) {
        return element.toString().replace("." + element.simpleName, "")
    }
    return this.packagePath
}

fun Element.getPackagePath(packagePath: String? = null): String {
    if (packagePath.isNullOrBlank()) {
        return this.toString().replace(".$simpleName", "")
    }
    return packagePath
}


fun getTypeForTry(invoke: () -> Any?): TypeMirror? {

    // 不要直接用annotation.value()，而是用TypeMirror
    try {
        // 这里会触发MirroredTypeException
        invoke()
    } catch (e: MirroredTypeException) {
        return e.typeMirror
    }
    return null
}

fun getTypesForTry(invoke: () -> Any?, result: (TypeMirror) -> Unit = {}) {
    // 不要直接用annotation.value()，而是用TypeMirror
    try {
        // 这里会触发MirroredTypeException
        invoke()
    } catch (e: MirroredTypeException) {
        result(e.typeMirror)
    } catch (e: MirroredTypesException) {
        for (mirror in e.typeMirrors) {
            result(mirror)
        }
    }
}

fun List<String>.joinToPackage(): String {
    return mapNotNull { it.ifBlank { null } }.joinToString(".")
}