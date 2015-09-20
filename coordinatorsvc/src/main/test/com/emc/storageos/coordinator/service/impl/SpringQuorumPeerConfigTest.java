package com.emc.storageos.coordinator.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.zookeeper.server.quorum.QuorumPeer.LearnerType;
import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringQuorumPeerConfigTest {
    private static final Logger log = LoggerFactory.getLogger(SpringQuorumPeerConfigTest.class);

    private Properties properties;
    private SpringQuorumPeerConfig springQuorumPeerConfig;

    @Before
    public void setup() {
        properties = new Properties();
        properties.setProperty("tickTime", "2000");
        properties.setProperty("dataDir", "/tmp/zk");
        properties.setProperty("clientPort", "2181");
        properties.setProperty("initLimit", "5");
        properties.setProperty("syncLimit", "2");
        properties.setProperty("server.1", "192.168.1.1:2888:3888;2181");
        properties.setProperty("server.2", "hostname:2888:3888;2181");
        properties.setProperty("server.3", "[fe80:0:0:0:81fe:4fd:95b1:8bbf]:2888:3888;2181");

        properties.setProperty(SpringQuorumPeerConfig.staticCfgFileKey, "zk-static.cfg");
        properties.setProperty(SpringQuorumPeerConfig.dynamicCfgFileKey, "zk-dynamic.cfg");

        springQuorumPeerConfig = new SpringQuorumPeerConfig();
    }

    @Test
    public void testInitWithCustomizedSeperator() throws Exception {
        springQuorumPeerConfig.setProperties(properties);
        springQuorumPeerConfig.init();

        assertTrue(springQuorumPeerConfig.getServers().size() == 3);

        QuorumServer server1 = springQuorumPeerConfig.getServers().get(new Long(1));
        assertTrue(server1.addr.toString().equals("/192.168.1.1:2888"));
        assertTrue(server1.electionAddr.toString().equals("/192.168.1.1:3888"));
        assertTrue(server1.type == LearnerType.PARTICIPANT);

        QuorumServer server2 = springQuorumPeerConfig.getServers().get(new Long(2));
        assertTrue(server2.addr.toString().equals("hostname:2888"));
        assertTrue(server2.electionAddr.toString().equals("hostname:3888"));
        assertTrue(server2.type == LearnerType.PARTICIPANT);

        QuorumServer server3 = springQuorumPeerConfig.getServers().get(new Long(3));
        assertTrue(server3.addr.toString().equals("/fe80:0:0:0:81fe:4fd:95b1:8bbf:2888"));
        assertTrue(server3.electionAddr.toString().equals("/fe80:0:0:0:81fe:4fd:95b1:8bbf:3888"));
        assertTrue(server3.type == LearnerType.PARTICIPANT);
    }

    @Test
    public void testServerPropertiesHasbeenRemoved() throws Exception {
        SpringQuorumPeerConfig target = new SpringQuorumPeerConfig();

        target.setProperties(properties);
        target.init();

        String staticCfgFile = properties.getProperty(SpringQuorumPeerConfig.staticCfgFileKey);
        String dynamicCfgFile = properties.getProperty(SpringQuorumPeerConfig.dynamicCfgFileKey);
        File dataDir = target.getDataDir();


        File cfgFile = new File(dataDir, staticCfgFile);
        assertTrue(cfgFile.exists());

        //make sure we can load static properties
        FileInputStream in = new FileInputStream(cfgFile);
        Properties staticProperty = new Properties();
        staticProperty.load(in);

        //check dynamic config file
        cfgFile = new File(dataDir, dynamicCfgFile);
        assertTrue(cfgFile.exists());

        in = new FileInputStream(cfgFile);
        Properties dynamicProperty = new Properties();
        dynamicProperty.load(in);

        // check whether server properties are removed
        for (String key : dynamicProperty.stringPropertyNames()) {
            assertTrue(key.trim().startsWith("server."));
        }
    }
}
