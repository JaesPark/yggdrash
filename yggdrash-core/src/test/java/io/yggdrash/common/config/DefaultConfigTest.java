package io.yggdrash.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultConfigTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigTest.class);

    private DefaultConfig defaultConfig;

    @Before
    public void setUp() {
        this.defaultConfig = new DefaultConfig();
    }

    /**
     * This is the default config test code.
     * get system config, spring config, yggdrash.conf config.
     */
    @Test
    public void defaultConfigTest() {
        log.debug(defaultConfig.toString());

        assertThat(defaultConfig.getString("java.version")).startsWith("1.8");
        log.debug("DefaultConfig java.version: "
                + defaultConfig.getString("java.version"));

        assertThat(defaultConfig.getString("node.name")).isEqualTo("yggdrash");
        log.debug("DefaultConfig node.name: "
                + defaultConfig.getString("node.name"));

        assertThat(defaultConfig.getString("network.port")).isEqualTo("32918");
        log.debug("DefaultConfig network.port: "
                + defaultConfig.getString("network.port"));

    }

    @Test
    public void productionConfigTest() {
        DefaultConfig productionConfig = new DefaultConfig(true);
        String yggDataPath = productionConfig.getYggDataPath();
        String expectedDataPath;
        if ("root".equals(System.getProperty("user.name"))) {
            expectedDataPath = System.getProperty("user.dir") + File.separator + ".yggdrash";
        } else {
            expectedDataPath = System.getProperty("user.home") + File.separator + ".yggdrash";
        }
        assertThat(yggDataPath).isEqualTo(expectedDataPath);
    }

    /**
     * This is the test for new config file yggdrash_sample.conf.
     * get system config, spring config, yggdrash.conf & yggdrash_sample.conf config.
     */
    @Test
    public void newConfigFileTest() {
        Config config = ConfigFactory.parseResources("yggdrash_sample.conf");
        DefaultConfig defaultConfig = new DefaultConfig(config);

        assertThat(defaultConfig.getString("key.path")).endsWith("nodePri2.key");

        log.debug("newConfigFile key.path: "
                + defaultConfig.getString("key.path"));
    }

    /**
     * This is the test for printing Class.
     */
    @Test
    public void testNodeInfo() {
        assert defaultConfig.getNetworkP2PVersion().equals("0.0.1");
        assert defaultConfig.getNetwork() == DefaultConfig.Network.TEST_NET;
        assert defaultConfig.getNodeName().equals("yggdrash");
        assert defaultConfig.getNodeVersion().equals("0.0.2");

    }

}