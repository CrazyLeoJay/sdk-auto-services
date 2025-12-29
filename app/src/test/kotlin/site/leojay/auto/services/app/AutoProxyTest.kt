package site.leojay.auto.services.app

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.leojay.auto.services.utils.AutoProxy
import site.leojay.auto.services.utils.ProxyHelperBuilder
import site.leojay.auto.services.utils.ProxyHelperBuilder.Companion.register
import site.leojay.auto.services.utils.annotation.SDKModuleSingleInstance

/**
 *
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class AutoProxyTest {
    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }


    @Test
    fun instanceThrow() {
        getAppMarket()?.login()
    }

    fun getAppMarket(): AppMarketFactory? {
        val className = "site.leojay.android.sdk.AppMarket"
        val method = "instance"
        try {
            val sdk: AppMarketFactory = AutoProxy.instanceThrow(AppMarketFactory::class.java, className, method)
            return sdk
        } catch (e: Throwable) {
            println("AppMarket 模块不存在")
        }
        return null
    }

}

interface AppMarketFactory {
    fun login()
}

@SDKModuleSingleInstance(
    "AppMarket",
    AppMarketFactory::class,
    packagePath = "site.leojay.android.sdk"
)
class AppMarketFactoryImpl(builder: ProxyHelperBuilder<AppMarketFactory>) : AppMarketFactory {
    init {
        builder.register(AppMarketFactory::class, builder.build())
    }

    override fun login() {
        println("AppMarketFactory login")
    }
}
