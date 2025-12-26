package site.leojay.auto.services.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import site.leojay.auto.services.utils.AutoProxy
import site.leojay.auto.services.utils.ModulesHelper
import site.leojay.auto.services.utils.ProxyHelperBuilder
import site.leojay.auto.services.utils.annotation.MakeSingleObject
import site.leojay.auto.services.utils.annotation.SDKModule
import java.util.logging.Logger
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

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
        roundEnv?.getElementsAnnotatedWith(MakeSingleObject::class.java)?.forEach { element ->
            createSingleObject(element, roundEnv).writeTo(processingEnv.filer)
        }
        return true
    }

    fun createSingleObject(element: Element, roundEnv: RoundEnvironment): FileSpec {
        val annotation = element.getAnnotation(MakeSingleObject::class.java)!!

        val thisInstanceType = getTypeForTry { annotation.implInterface }?.asTypeName()!!
        return FileSpec.builder(annotation.getPackagePath(element), annotation.value)
            .addImport("${ProxyHelperBuilder::class.qualifiedName}.Companion", "register")
            .addType(
                TypeSpec.objectBuilder(annotation.value)
                    .addFunction(
                        FunSpec.builder("instance")
                            .addCode("return SingleEnum.INSTANCE.instance")
                            .returns(thisInstanceType)
                            .build()
                    )
                    .addType(
                        TypeSpec.enumBuilder("SingleEnum")
                            .addModifiers(KModifier.PRIVATE)
                            .primaryConstructor(
                                FunSpec.constructorBuilder().addParameter(
                                    ParameterSpec.builder("instance", thisInstanceType).build()
                                ).build()
                            )
                            .addProperty(
                                PropertySpec.builder("instance", thisInstanceType)
                                    .initializer(CodeBlock.of("instance"))
                                    .build()
                            )
                            .addEnumConstant(
                                "INSTANCE", TypeSpec.anonymousClassBuilder()
                                    .apply {
                                        if (annotation.proxy) {
                                            addSuperclassConstructorParameter(
                                                "%T.proxy(%T::class.java, %T(Commons.builder))",
                                                AutoProxy::class.java,
                                                thisInstanceType,
                                                element.asType()
                                            )
                                        } else {
                                            addSuperclassConstructorParameter(
                                                "%T(Commons.builder)",
                                                element.asType()
                                            )
                                        }

                                    }
                                    .build()
                            )
                            .build()
                    )
                    .addType(makeCommonsType(roundEnv, thisInstanceType))
                    .build()
            )
            .build()
    }

    fun makeCommonsType(roundEnv: RoundEnvironment, thisInstanceType: TypeName): TypeSpec {
        val builder = TypeSpec.objectBuilder("Commons").addModifiers(KModifier.PRIVATE)

        val instanceClass = mutableListOf<TypeName>()
        val instanceTypeClass = mutableListOf<TypeName>()
        roundEnv.getElementsAnnotatedWith(SDKModule::class.java)?.let { elements ->
            for (element in elements) {
                when (element.kind) {
                    ElementKind.CLASS -> instanceClass.add(element.asType().asTypeName())
                    ElementKind.INTERFACE -> instanceTypeClass.add(element.asType().asTypeName())
                    else -> {}
                }
            }
        }
        builder.addProperty(
            PropertySpec.builder(
                "instances",
                List::class
                    .parameterizedBy(Any::class),
                KModifier.PRIVATE
            )
                .initializer("listOf(${instanceClass.joinToString { "%T()" }})", *(instanceClass.toTypedArray()))
                .build()
        )
        builder.addProperty(
            PropertySpec.builder(
                "instanceTypes",
                List::class.asTypeName()
                    .parameterizedBy(
                        KClass::class.asTypeName()
                            .parameterizedBy(TypeVariableName("*"))
                    ),
                KModifier.PRIVATE
            )
                .initializer(
                    "listOf(${instanceTypeClass.joinToString { "%T::class" }})",
                    *(instanceTypeClass.toTypedArray())
                )
                .build()
        )

        builder.addProperty(
            PropertySpec.builder("modulesHelper", ModulesHelper::class, KModifier.PRIVATE)
                .initializer("%T(instances, instanceTypes)", ModulesHelper::class)
                .build()
        )


        builder.addProperty(
            PropertySpec.builder(
                "builder", ProxyHelperBuilder::class.asTypeName()
                    .parameterizedBy(thisInstanceType)
            )
                .initializer("%T(%T::class, modulesHelper)", ProxyHelperBuilder::class, thisInstanceType)
                .build()
        )

        return builder.build()

    }

    override fun getSupportedAnnotationTypes(): Set<String?> {
        return setOf(
            MakeSingleObject::class.java.canonicalName,
            SDKModule::class.java.canonicalName,
        )
    }
}
