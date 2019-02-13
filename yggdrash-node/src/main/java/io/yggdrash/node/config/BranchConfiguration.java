/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.config;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.contract.ContractClassLoader;
import io.yggdrash.core.contract.ContractManager;
import io.yggdrash.core.net.PeerNetwork;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.node.ChainTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@EnableScheduling
public class BranchConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BranchConfiguration.class);

    private final StoreBuilder storeBuilder;

    private PeerNetwork peerNetwork;

    @Value("classpath:/branch-stem.json")
    Resource stemResource;

    @Value("classpath:/branch-yeed.json")
    Resource yeedResource;

    @Value("classpath:/branch-sw.json")
    Resource swResource;

    @Value("classpath:/branch-asset.json")
    Resource assetResource;

    @Autowired
    BranchConfiguration(StoreBuilder storeBuilder) {
        this.storeBuilder = storeBuilder;
    }

    @Autowired
    public void setPeerNetwork(PeerNetwork peerNetwork) {
        this.peerNetwork = peerNetwork;
    }

    @Bean
    @ConditionalOnProperty(name = "yggdrash.node.seed",
            havingValue = "false", matchIfMissing = true)
    BlockChain stem(BranchGroup branchGroup) throws IOException {
        return addBranch(stemResource.getInputStream(), branchGroup);
    }

    @Bean
    @ConditionalOnProperty("yggdrash.node.chain.enabled")
    BlockChain yeed(BranchGroup branchGroup) throws IOException {
        return addBranch(yeedResource.getInputStream(), branchGroup);
    }

    @Bean
    @ConditionalOnProperty("yggdrash.node.chain.enabled")
    BlockChain sw(BranchGroup branchGroup) throws IOException {
        return addBranch(swResource.getInputStream(), branchGroup);
    }

    @Bean
    BranchGroup branchGroup(BranchLoader loader) {
        BranchGroup branchGroup = new BranchGroup();
        try {
            for (GenesisBlock genesis : loader.getGenesisBlockList()) {
                addBranch(genesis, branchGroup);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return branchGroup;
    }

    @Bean
    BranchLoader branchLoader(DefaultConfig defaultConfig) {
        if (defaultConfig.isProductionMode()) {
            ContractClassLoader.copyResourcesToContractPath(defaultConfig.getContractPath());
        }
        return new BranchLoader(defaultConfig.getBranchPath());
    }

    @Bean
    ContractManager contractManager(DefaultConfig defaultConfig) {
        if (defaultConfig.isProductionMode()) {
            ContractClassLoader.copyResourcesToContractPath(defaultConfig.getContractPath());
        }
        return new ContractManager(defaultConfig.getContractPath());
    }

    private BlockChain addBranch(InputStream is, BranchGroup branchGroup)
            throws IOException {
        GenesisBlock genesis = GenesisBlock.of(is);
        return addBranch(genesis, branchGroup);
    }

    private BlockChain addBranch(GenesisBlock genesis,
                                 BranchGroup branchGroup) {
        try {
            BlockChain branch = BlockChainBuilder.Builder()
                    .addGenesis(genesis)
                    .setStoreBuilder(storeBuilder)
                    .build();
            branchGroup.addBranch(branch, peerNetwork);
            peerNetwork.init(branch.getBranchId());
            return branch;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    /**
     * Scheduling Beans
     */

    @Bean
    @ConditionalOnProperty("yggdrash.node.chain.gen")
    public ChainTask chainTask() {
        return new ChainTask();
    }
}
