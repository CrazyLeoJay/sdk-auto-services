package site.leojay.auto.services.processor.builder

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import site.leojay.auto.services.processor.AppProcessorException
import site.leojay.auto.services.processor.builder.SDKProcessor2Builder.SDKSingleConfig.Companion.createSDKConfig
import site.leojay.auto.services.processor.getPackagePath
import site.leojay.auto.services.processor.getTypeForTry
import site.leojay.auto.services.processor.joinToPackage
import site.leojay.auto.services.utils.AutoProxy
import site.leojay.auto.services.utils.ModulesHelper
import site.leojay.auto.services.utils.ProxyHelperBuilder
import site.leojay.auto.services.utils.annotation.RegisterSDKSingeInstance
import site.leojay.auto.services.utils.annotation.SDKLibrary
import site.leojay.auto.services.utils.annotation.SDKModule
import site.leojay.auto.services.utils.annotation.SingleInstance
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import javax.annotation.processing.Filer
import javax.annotation.processing.FilerException
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import kotlin.reflect.KClass

/**
 * SDK新构建方案
 * 正对第一版进行升级
 * 解决一些问题，主要调整内部调用
 * - 内部调用需要跟外部调用使用同一个主体
 * - 如果存在一套代码多个主体，那么内部调用和外部调用会有冲突
 *
 * 修改方向
 * - 根据被注解实体类，如果没有指定的父类，则创建一个没有主体的单例，添加一个默认空实现
 * - 如果被注解类实现了当前指定的接口，则将当前类作为实例的默认实现，第一个调用。
 * - 构造函数内不得包含参数传入，减少一些代码耦合，更方便使用。
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class SDKProcessor2Builder private constructor(
    private val roundEnv: RoundEnvironment,
    private val processingEnv: ProcessingEnvironment,
) {
    companion object {
        val ANNOTATIONS = setOf(
            RegisterSDKSingeInstance::class.java.canonicalName,
            SDKModule::class.java.canonicalName,
            SDKLibrary::class.java.canonicalName,
            SingleInstance::class.java.canonicalName,
        )

        fun makeFile(
            roundEnv: RoundEnvironment,
            processingEnv: ProcessingEnvironment,
        ) {
            val processorBuilder = SDKProcessor2Builder(roundEnv, processingEnv)

            // 构建SDK单例
            processorBuilder.makeBuilder {
                it.build().writeTo(processingEnv.filer)
                it.buildInterface(processingEnv.filer)
            }

            // 构建单例
            processorBuilder.makeSingleBuilder {
                it.buildSingleInstance()
            }

        }
    }

    fun makeBuilder(invoke: (Builder) -> Unit) {
        roundEnv.getElementsAnnotatedWith(RegisterSDKSingeInstance::class.java).forEach { element ->
            invoke(Builder(element, roundEnv, processingEnv))
        }
    }

    class SDKSingleConfig(element: Element, processingEnv: ProcessingEnvironment) {
        companion object {
            fun ProcessingEnvironment.createSDKConfig(element: Element): SDKSingleConfig {
                return SDKSingleConfig(element, this)
            }
        }

        val annotation = element.getAnnotation(RegisterSDKSingeInstance::class.java)!!
        val packageName: String = listOf(
            annotation.getPackagePath(element),
            annotation.packageSuffix,
        ).joinToPackage()

        val defaultImplTypeName = ClassName.bestGuess("${packageName}.SDKFactory")
        val defaultInnerTypeName = ClassName.bestGuess("${packageName}.AppFactory")
        val sdkFactoryType = processingEnv.getTypeNameNotDefOrDefault({ annotation.implInterface }, defaultImplTypeName)
        val appFactoryType =
            processingEnv.getTypeNameNotDefOrDefault({ annotation.innerInterface }, defaultInnerTypeName)

        //    val thisInstanceType: TypeName = sdkFactoryType
        val thisInstanceType: TypeName = appFactoryType

        val abstractSDKFactoryClassName = ClassName.bestGuess("${packageName}.${annotation.value}")

    }


    class Builder(
        private val element: Element,
        private val roundEnv: RoundEnvironment,
        private val processingEnv: ProcessingEnvironment,
    ) {
        val config = SDKSingleConfig(element, processingEnv)

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

        private fun createSDKFactory() = TypeSpec.interfaceBuilder(config.defaultImplTypeName)
            .addSuperinterfaces(instanceTypeClass)
            .build()

        private fun createAppFactory() = TypeSpec.interfaceBuilder(config.defaultInnerTypeName)
            .addSuperinterfaces(listOf(config.sdkFactoryType, *innerInstanceTypeClass.toTypedArray()))
            .build()

        private fun isSuperTypeOrInterface(element: Element, targetTypeName: TypeName): Boolean {
            // 获取 Element 的 TypeMirror
            val typeMirror = element.asType()

            // 获取目标类型的全限定名（不带泛型）
            val fullQualifiedName = targetTypeName.toString().substringBefore('<')

            // 通过全限定名获取目标类型的 TypeElement
            // 如果找不到目标类型，返回 false
            val targetElement = processingEnv.elementUtils.getTypeElement(fullQualifiedName)
            if (null != targetElement) {
                // 获取目标类型的 TypeMirror
                val targetTypeMirror = targetElement.asType()

                // 使用 Types 工具类判断是否是父类或接口实现
                val bool = processingEnv.typeUtils.isSubtype(typeMirror, targetTypeMirror)
                return bool
            } else {
                return false
            }
        }


        private fun makeCommonsType(): TypeSpec {
            val allType = listOf(*instanceTypeClass.toTypedArray(), *innerInstanceTypeClass.toTypedArray())
            val builder = TypeSpec.objectBuilder("Commons")
                .addKdoc("定义常量参数")
//            .addModifiers(KModifier.INTERNAL)
                .addModifiers(KModifier.PRIVATE)
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
                    PropertySpec.builder("modulesHelper", ModulesHelper::class, KModifier.INTERNAL)
                        .addKdoc("模块辅助工具")
                        .initializer("%T(instances, instanceTypes)", ModulesHelper::class)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                        "builder", ProxyHelperBuilder::class.asTypeName()
                            .parameterizedBy(config.thisInstanceType)
                    )
                        .addModifiers(KModifier.INTERNAL)
                        .addKdoc("代理辅助工具")
                        .initializer(
                            "%T(%T::class, modulesHelper, %T())",
                            ProxyHelperBuilder::class.asTypeName().parameterizedBy(config.thisInstanceType),
                            config.thisInstanceType,
                            element.asType(),
                        )
                        .build()
                )
            return builder.build()
        }

        private fun createAbstractSDKFactory(): TypeSpec {
            val sdkName = config.abstractSDKFactoryClassName
            val type = config.sdkFactoryType.let {
                ClassName.bestGuess(it.toString()).simpleName
            }

            return TypeSpec.classBuilder("Abstract$type")
                .addModifiers(KModifier.ABSTRACT)
                .addSuperinterface(config.sdkFactoryType)
                .addSuperinterface(InvocationHandler::class)
                .addProperty(
                    PropertySpec.builder("app", config.thisInstanceType, KModifier.PROTECTED)
                        .initializer("%T.instance()", sdkName)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("sdk", config.sdkFactoryType, KModifier.PRIVATE)
                        .initializer("%T.instance()", sdkName)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("modulesHelper", ModulesHelper::class, KModifier.PROTECTED)
                        .initializer("%T.Commons.modulesHelper", sdkName)
                        .build()
                )
                .addFunction(
                    FunSpec.builder("invoke")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("proxy", ANY.copy(true))
                        .addParameter("method", Method::class)
                        .addParameter("args", ARRAY.parameterizedBy(ANY.copy(true)).copy(true))
                        .returns(ANY.copy(true))
                        .addCode(
                            """
                        |return method.autoInvoke(this, args).also {
                        |   method.autoInvoke(sdk, args)
                        |}
                    """.trimMargin()
                        )
                        .build()
                )
                .build()
        }

        fun build() = FileSpec.builder(config.packageName, config.annotation.value)
            .addImport(AutoProxy::class, "autoInvoke")
            .addType(
                TypeSpec.classBuilder(config.annotation.value)
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
                                    .addCode("return SingleEnum.INSTANCE.factory")
                                    .returns(config.thisInstanceType)
                                    .build()
                            )
                            .build()
                    )
                    .addType(createSingleEnum(config.thisInstanceType, {
                        addKdoc("直接用注解类实例实现")
                        addSuperclassConstructorParameter("Commons.builder.build()")
                    }))
                    .addType(makeCommonsType())
                    .addType(createAbstractSDKFactory())
                    .build()
            )
            .build()

        /**
         * 接口定义类实现
         */
        fun buildInterface(filer: Filer) {
            if (config.annotation.isAny { this.implInterface }) {
                try {
                    FileSpec.builder(config.defaultImplTypeName)
                        .addType(createSDKFactory())
                        .build().writeTo(filer)
                } catch (e: Exception) {
                }
            }

            if (config.annotation.isAny { this.innerInterface }) {
                try {
                    FileSpec.builder(config.defaultInnerTypeName)
                        .addType(createAppFactory())
                        .build().writeTo(filer)
                } catch (e: FilerException) {
                }
            }
        }
    }

    fun makeSingleBuilder(invoke: (SingleBuilder) -> Unit) {
        roundEnv.getElementsAnnotatedWith(SingleInstance::class.java).forEach { element ->
            invoke(SingleBuilder(element, roundEnv, processingEnv))
        }
    }

    class SingleBuilder(
        private val element: Element,
        private val roundEnv: RoundEnvironment,
        private val processingEnv: ProcessingEnvironment,
    ) {

        fun buildSingleInstance() {
            val singleInstance = element.getAnnotation(SingleInstance::class.java)!!
            val packageName = listOf(
                element.getPackagePath(singleInstance.packagePath),
                singleInstance.packageSuffix,
            ).joinToPackage()

            val returnType: TypeName = getTypeForTry { singleInstance.implInterface }!!.let {
                val type = processingEnv.typeUtils.asElement(it)
                if (type.kind == ElementKind.CLASS) {
                    val annotation = type.getAnnotation(RegisterSDKSingeInstance::class.java)
                    if (null != annotation) {
                        return@let processingEnv.createSDKConfig(type).sdkFactoryType
                    }
                    throw AppProcessorException("注解参数 SingleInstance#implInterface 是类，但没有注解 RegisterSDKSingeInstance")
                }
                if (type.kind != ElementKind.INTERFACE) throw AppProcessorException("必须是接口类，或者注解了 @RegisterSDKSingeInstance 的类")
                if (type.getPackagePath() == "<Any>?") throw AppProcessorException("${type.toString()}未找到，如果是通过注解 RegisterSDKSingeInstance 生成的SDKFactory，可以直接修改为注解导入注解了RegisterSDKSingeInstance的实体")
                it.asTypeName()
            }

            FileSpec.builder(packageName, singleInstance.value)
                .addType(
                    TypeSpec.classBuilder(singleInstance.value)
                        .addType(
                            TypeSpec.companionObjectBuilder()
                                .addFunction(
                                    FunSpec.builder("instance")
                                        .addCode("return SingleEnum.INSTANCE.factory")
                                        .returns(returnType)
                                        .build()
                                )
                                .build()
                        )
                        .addType(createSingleEnum(returnType) {
                            addSuperclassConstructorParameter(CodeBlock.of("%T()", element.asType()))
                        })
                        .build()
                )
                .build().writeTo(processingEnv.filer)
        }
    }
}


private fun RegisterSDKSingeInstance.isAny(
    invoke: RegisterSDKSingeInstance.() -> Unit,
): Boolean {
    return getTypeForTry { invoke.invoke(this) }?.let {
        "java.lang.Object" == (it.toString())
    } ?: false
}

/**
 * 获取 TypeName 当没有定义时，返回默认值。
 */
private fun ProcessingEnvironment.getTypeNameNotDefOrDefault(
    invoke: () -> Unit,
    defaultTypeName: TypeName,
): TypeName {
    return getTypeForTry { invoke.invoke() }!!.let {
        if ("java.lang.Object" == (it.toString())) {
            return defaultTypeName
        }
        typeUtils.asElement(it).let {
            if (it.kind != ElementKind.INTERFACE) throw AppProcessorException("必须是接口类")
        }
        it.asTypeName()
    }
}

private fun createSingleEnum(returnKClassType: TypeName, singleInstanceInvoke: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.enumBuilder("SingleEnum")
        .addModifiers(KModifier.PRIVATE)
        .addKdoc("单例枚举")
        .primaryConstructor(
            FunSpec.constructorBuilder().addParameter(
                ParameterSpec.builder("factory", returnKClassType).build()
            ).build()
        )
        .addProperty(
            PropertySpec.builder("factory", returnKClassType)
                .initializer(CodeBlock.of("factory"))
                .build()
        )
        .addEnumConstant(
            "INSTANCE", TypeSpec.anonymousClassBuilder()
                .apply {
                    singleInstanceInvoke(this)
                }
                .build()
        )
        .build()
}
