package site.leojay.auto.services.processor

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import site.leojay.auto.services.utils.ProxyInstance
import kotlin.test.Test

/**
 *
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
class SingleProcessorTest {
    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun test1(){
        println(ProxyInstance::class.qualifiedName)
        println(ProxyInstance::class.simpleName)
    }

}