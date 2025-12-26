package site.leojay.auto.services.processor

import site.leojay.auto.services.utils.annotation.MakeSingleObject
import javax.lang.model.element.Element
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror


/**
 * 拓展
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */

fun MakeSingleObject.getPackagePath(element: Element): String {
    if (this.packagePath.isBlank()) {
        return element.toString().replace("." + element.simpleName, "")
    }
    return this.packagePath
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