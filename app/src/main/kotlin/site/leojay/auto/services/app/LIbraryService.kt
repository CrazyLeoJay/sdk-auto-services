package site.leojay.auto.services.app

import site.leojay.auto.services.app.register.AppFactory
import site.leojay.auto.services.app.register.AppSDKRegisterThis
import site.leojay.auto.services.utils.annotation.RegisterSDKSingerInstance
import site.leojay.auto.services.utils.annotation.SingleInstance

/**
 * 第二种用途
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */

/**
 * 没有主体的 SDK 单例
 */
@RegisterSDKSingerInstance("AppSDK", packageSuffix = "core")
class AppNoMainLibraryService


@RegisterSDKSingerInstance(
    "AppSDKRegisterThis",
    SDKFactory::class,
    packageSuffix = "register"
)
class AppRegisterThisLibraryService : AppFactory {
    override fun init() {

    }

    override fun registerConfig(config: Config) {

    }
}


@SingleInstance(
    "LeojaySDK",
    implInterface = SDKFactory::class,
    packageSuffix = "register.sdk"
)
class AppRegisterImpl : AppSDKRegisterThis.AbstractSDKFactory() {

    init {
        app.registerConfig(Config("AppRegister 配置"))
    }

    override fun init() {

    }
}