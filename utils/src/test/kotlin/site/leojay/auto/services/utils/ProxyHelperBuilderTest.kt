package site.leojay.auto.services.utils

import org.junit.jupiter.api.Test
import site.leojay.auto.services.utils.AutoProxy.autoInvoke
import site.leojay.auto.services.utils.ProxyHelperBuilder.Companion.register
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.BeforeTest

/**
 * 测试
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class ProxyHelperBuilderTest {
    companion object {
        private val log = Logger.getLogger(ProxyHelperBuilderTest::class.java.name)
    }

    lateinit var payFactory: PayFactory

    @BeforeTest
    fun before() {
        val payFactoryImpl = object : PayFactory {
            override fun pay() {
                log.info("playFactory impl pay invoke")
            }
        }
        payFactory = AutoProxy.proxy(PayFactory::class.java) { proxy, method, args ->
            log.info("playFactory Proxy pay invoke before")
            return@proxy method.autoInvoke(payFactoryImpl, args).also {
                log.info("playFactory Proxy pay invoke after\n")
            }
        }
    }

    @Test
    fun build() {

        ProxyHelperBuilder(SDKFactory::class, ModulesHelper())
            .registerThrowListener {
                log.log(Level.WARNING, "调用异常: ${it?.message ?: ""}", it)
            }
            .register(AuthFactory::class, object : AuthFactory {
                override fun login(username: String, password: String) {
                    println("invoke AuthFactory::login($username:$password)")
                }
            })
            .register(PayFactory::class, payFactory)
            .build()
            .also { sdkFactory ->
                sdkFactory.login("leojay", "password for leojay")
                sdkFactory.pay()
                sdkFactory.showBat()
                sdkFactory.hideBat()
            }
    }

}

interface FunctionFactory
interface AuthFactory : FunctionFactory {
    fun login(username: String, password: String)
}

interface PayFactory : FunctionFactory {
    fun pay()
}

interface FloatBatFactory : FunctionFactory {
    fun showBat()
    fun hideBat()
}

interface SDKFactory : AuthFactory, PayFactory, FloatBatFactory {
    fun init()
}