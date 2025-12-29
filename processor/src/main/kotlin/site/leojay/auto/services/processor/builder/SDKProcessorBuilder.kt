package site.leojay.auto.services.processor.builder

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import site.leojay.auto.services.processor.getPackagePath
import site.leojay.auto.services.processor.getTypeForTry
import site.leojay.auto.services.utils.AutoProxy
import site.leojay.auto.services.utils.ModulesHelper
import site.leojay.auto.services.utils.ProxyHelperBuilder
import site.leojay.auto.services.utils.SDKCallback
import site.leojay.auto.services.utils.annotation.SDKLibrary
import site.leojay.auto.services.utils.annotation.SDKModule
import site.leojay.auto.services.utils.annotation.SDKModuleSingleInstance
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import kotlin.reflect.KClass

/**
 * SDK自动构建工具
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class SDKProcessorBuilder(
    private val element: Element,
    private val roundEnv: RoundEnvironment,
    private val processingEnv: ProcessingEnvironment,
) {
    companion object {
        val ANNOTATIONS = setOf(
            SDKModuleSingleInstance::class.java.canonicalName,
            SDKModule::class.java.canonicalName,
            SDKLibrary::class.java.canonicalName,
        )
    }

    val annotation = element.getAnnotation(SDKModuleSingleInstance::class.java)!!

    val thisInstanceType = getTypeForTry { annotation.implInterface }?.asTypeName()!!

    val packageName = annotation.getPackagePath(element)


    val instanceClass = mutableListOf<TypeName>()
    val instanceTypeClass = mutableListOf<TypeName>()
    val innerInstanceTypeClass = mutableListOf<TypeName>()

    init {
        roundEnv.getElementsAnnotatedWith(SDKModule::class.java)?.let { elements ->
            for (element in elements) {
                when (element.kind) {
                    ElementKind.CLASS -> instanceClass.add(element.asType().asTypeName())
                    ElementKind.INTERFACE -> instanceTypeClass.add(element.asType().asTypeName())
                    else -> {}
                }
            }
        }
        roundEnv.getElementsAnnotatedWith(SDKLibrary::class.java)?.let { elements ->
            for (element in elements) {
                when (element.kind) {
                    ElementKind.CLASS -> instanceClass.add(element.asType().asTypeName())
                    ElementKind.INTERFACE -> innerInstanceTypeClass.add(element.asType().asTypeName())
                    else -> {}
                }
            }
        }
    }

    private fun singleEnum(): TypeSpec {
        return TypeSpec.enumBuilder("SingleEnum")
            .addModifiers(KModifier.PRIVATE)
            .addKdoc("单例枚举")
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
                            addKdoc("通过代理创建实现接口")
                            addSuperclassConstructorParameter(
                                "%T.proxy(%T::class.java, %T(Commons.builder))",
                                AutoProxy::class.java,
                                thisInstanceType,
                                element.asType()
                            )
                        } else {
                            addKdoc("直接用注解类实例实现")
                            addSuperclassConstructorParameter(
                                "%T(Commons.builder)",
                                element.asType()
                            )
                        }

                    }
                    .build()
            )
            .build()
    }

    private fun makeCommonsType(): TypeSpec {
        val allType = listOf(*instanceTypeClass.toTypedArray(), *innerInstanceTypeClass.toTypedArray())
        val builder = TypeSpec.objectBuilder("Commons")
            .addKdoc("定义常量参数")
            .addModifiers(KModifier.PRIVATE)
            .addProperty(
                PropertySpec.builder("sdkCallback", SDKCallback::class)
                    .initializer("%T({ modulesHelper })", SDKCallback::class)
                    .build()
            )
            .addProperty(
                PropertySpec.builder(
                    "instances",
                    List::class
                        .parameterizedBy(Any::class),
                    KModifier.PRIVATE
                )
                    .addKdoc("定义SDK中所有的功能实现")
                    .initializer(
                        "listOf(${
                            instanceClass.joinToString { "%T()" }
                        })", *(instanceClass.toTypedArray())
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder(
                    "instanceTypes",
                    List::class.asTypeName()
                        .parameterizedBy(
                            KClass::class.asTypeName()
                                .parameterizedBy(TypeVariableName.Companion("*"))
                        ),
                    KModifier.PRIVATE
                )
                    .addKdoc("定义的不同SDK模块")
                    .initializer(
                        "listOf(${allType.joinToString { "%T::class" }})",
                        *(allType.toTypedArray())
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder("modulesHelper", ModulesHelper::class, KModifier.PRIVATE)
                    .addKdoc("模块辅助工具")
                    .initializer("%T(instances, instanceTypes)", ModulesHelper::class)
                    .build()
            )
            .addProperty(
                PropertySpec.builder(
                    "builder", ProxyHelperBuilder::class.asTypeName()
                        .parameterizedBy(thisInstanceType)
                )
                    .addKdoc("代理辅助工具")
                    .initializer("%T(%T::class, modulesHelper)", ProxyHelperBuilder::class, thisInstanceType)
                    .build()
            )

            .addProperty(
                PropertySpec.builder(
                    "innerBuilder", ProxyHelperBuilder::class.asTypeName()
                        .parameterizedBy(className)
                )
                    .addKdoc("内部工具")
                    .initializer("%T(%T::class, modulesHelper)", ProxyHelperBuilder::class, className)
                    .build()
            )
        return builder.build()
    }

    private val className = ClassName.bestGuess("${packageName}.${annotation.value}.InnerApiFactory")

    private fun innerInstanceType(): TypeSpec {
        return TypeSpec.interfaceBuilder(className)
            .addSuperinterfaces(listOf(thisInstanceType, *innerInstanceTypeClass.toTypedArray()))
            .build()
    }

    private fun innerSingleType(): TypeSpec {
        return TypeSpec.enumBuilder("InnerApiSingleEnum")
            .addModifiers(KModifier.PRIVATE)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("innerApi", className)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("innerApi", className)
                    .initializer(CodeBlock.of("innerApi"))
                    .build()
            )
            .addEnumConstant(
                "INSTANCE", TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter(CodeBlock.of("Commons.innerBuilder.build()"))
                    .build()
            )
            .build()
    }

    fun build(): FileSpec {
        return FileSpec.builder(packageName, annotation.value)
            .addType(
                TypeSpec.classBuilder(annotation.value)
                    .apply {
                        try {
                            Class.forName("android.support.annotation.Keep")
                            addAnnotation(ClassName("android.support.annotation", "Keep"))
                        } catch (e: ClassNotFoundException) {
                            // 如果没有Keep类，就忽略，因为Android需要配置混淆，
                            // SDK需要保留名称，且有时候生成的单例会通过反射获取，所以也要保留
                        }
                    }
                    .addType(
                        TypeSpec.companionObjectBuilder()
                            .addFunction(
                                FunSpec.builder("instance")
                                    .addAnnotation(JvmStatic::class)
                                    .addKdoc("SDK 实例")
                                    .addCode("return SingleEnum.INSTANCE.instance")
                                    .returns(thisInstanceType)
                                    .build()
                            )
                            .build()
                    )
                    .addType(singleEnum())
                    .addType(makeCommonsType())
                    .addType(innerInstanceType())
                    .addType(innerSingleType())
                    .build()
            )
            .build()

    }
}