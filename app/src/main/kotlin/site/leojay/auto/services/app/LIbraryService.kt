package site.leojay.auto.services.app

import site.leojay.auto.services.app.register.AppFactory
import site.leojay.auto.services.app.register.AppSDK
import site.leojay.auto.services.utils.annotation.RegisterSDKSingeInstance
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
@RegisterSDKSingeInstance("AppSDK", packageSuffix = "core")
class AppNoMainLibraryService


@RegisterSDKSingeInstance(
    "AppSDK",
//    implInterface = SDKFactory::class,
//    innerInterface = ,
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
    implInterface = AppRegisterThisLibraryService::class,
    packageSuffix = "register.sdk"
)
class AppRegisterImpl : AppSDK.AbstractSDKFactory() {

    init {
        app.registerConfig(Config("AppRegister 配置"))
    }

    override fun init() {

    }
}