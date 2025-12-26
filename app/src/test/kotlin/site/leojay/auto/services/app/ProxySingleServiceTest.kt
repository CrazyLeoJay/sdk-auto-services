package site.leojay.auto.services.app

import kotlin.test.Test


/**
 *
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class ProxySingleServiceTest {

    @Test
    fun initInvoke() {
        LeojayProxySDK.instance().init()
    }

}