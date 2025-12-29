package site.leojay.auto.services.app

import site.leojay.auto.services.utils.annotation.SDKLibrary
import site.leojay.auto.services.utils.annotation.SDKModule

/**
 *
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */

interface SDKFactory : AuthFactory, PayFactory, InitFactory

@SDKModule
interface InitFactory {
    fun init()
}

@SDKModule
interface AuthFactory : InitFactory

@SDKModule
interface PayFactory : InitFactory

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


data class Config(val data:String = "配置")

@SDKLibrary
interface ConfigFactory{

    fun registerConfig(config: Config)
}