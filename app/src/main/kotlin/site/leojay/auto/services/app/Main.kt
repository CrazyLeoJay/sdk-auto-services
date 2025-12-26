package site.leojay.auto.services.app

import site.leojay.auto.services.utils.ProxyHelperBuilder
import site.leojay.auto.services.utils.ProxyInstance
import site.leojay.auto.services.utils.annotation.MakeSingleObject
import site.leojay.auto.services.utils.annotation.SDKModule
import java.util.logging.Logger

/**
 *
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */

fun main(args: Array<String>) {
    LeojayProxySDK.instance().init()
}

interface SDKFactory : AuthFactory, PayFactory, InitFactory

@SDKModule
interface InitFactory {
    fun init()
}

@SDKModule
interface AuthFactory : InitFactory

@SDKModule
interface PayFactory : InitFactory

@MakeSingleObject("LeojaySDK", implInterface = SDKFactory::class, proxy = false)
class SingleService(val module: ProxyHelperBuilder<SDKFactory>) : SDKFactory {
    companion object {
        private val log = Logger.getLogger(SingleService::class.java.name)
    }

    override fun init() {
        log.info("LeojaySDK init")
    }
}

@MakeSingleObject("LeojayProxySDK", implInterface = SDKFactory::class, proxy = true)
class ProxySingleService(module: ProxyHelperBuilder<SDKFactory>) : ProxyInstance<SDKFactory>(module), SDKFactory {

    companion object {
        private val log = Logger.getLogger(ProxySingleService::class.java.name)
    }

    override fun defaultSDKEntity(): SDKFactory {
        // 不可以new新实例，最好是在Class中创建后再引用，或者使用this
        return this
    }

    override fun init() {
        log.info("default SDK init")
    }
}

@SDKModule
class AuthFactoryImpl() : AuthFactory {
    override fun init() {
        println("AuthFactory impl init")
    }
}

@SDKModule
class PayFactoryImpl() : PayFactory {
    override fun init() {
        println("PayFactory impl init")
    }
}

