package site.leojay.auto.services.processor

import com.google.auto.service.AutoService
import site.leojay.auto.services.processor.builder.SDKProcessor2Builder
import site.leojay.auto.services.processor.builder.SDKProcessorBuilder
import site.leojay.auto.services.utils.annotation.SDKModuleSingleInstance
import java.util.logging.Logger
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * 单例注解实现器
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor::class)
class SingleProcessor : AbstractProcessor() {
    companion object {
        private val log = Logger.getLogger(SingleProcessor::class.java.canonicalName)
    }

    override fun process(
        annotations: Set<TypeElement?>?,
        roundEnv: RoundEnvironment?,
    ): Boolean {
        roundEnv?.getElementsAnnotatedWith(SDKModuleSingleInstance::class.java)?.forEach { element ->
//            createSingleObject(element, roundEnv).writeTo(processingEnv.filer)
            SDKProcessorBuilder(element, roundEnv, processingEnv).build().writeTo(processingEnv.filer)
        }
        roundEnv?.let { SDKProcessor2Builder.makeFile(roundEnv, processingEnv) }
        return true
    }

    override fun getSupportedAnnotationTypes(): Set<String?> {
        return setOf(
            *SDKProcessorBuilder.ANNOTATIONS.toTypedArray(),
            *SDKProcessor2Builder.ANNOTATIONS.toTypedArray(),
        )
    }
}
