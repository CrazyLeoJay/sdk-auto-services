package site.leojay.auto.services.app;

import org.junit.jupiter.api.Test;

/**
 *
 *
 * @author leojay`Fu
 * create for 2025/12/22
 */
public class ProxySingleServiceJavaTest {

    @Test
    public void initInvoke() {
        LeojayProxySDK.instance().init();
        LeojayProxySDK.Companion.instance().init();
    }
}
