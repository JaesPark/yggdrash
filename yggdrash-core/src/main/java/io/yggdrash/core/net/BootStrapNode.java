/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.SyncManager;
import io.yggdrash.core.p2p.BlockChainHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class BootStrapNode implements BootStrap {
    private static final Logger log = LoggerFactory.getLogger(BootStrapNode.class);

    protected NodeStatus nodeStatus;
    protected PeerNetwork peerNetwork;
    protected BranchGroup branchGroup;
    protected SyncManager syncManager;

    @Override
    public void bootstrapping() {
        peerNetwork.init();
        nodeStatus.sync();
        for (BlockChain blockChain : branchGroup.getAllBranch()) {
            List<BlockChainHandler> peerHandlerList = peerNetwork.getHandlerList(blockChain.getBranchId());

            fullSyncBlock(blockChain, peerHandlerList);

            syncTransaction(blockChain, peerHandlerList);
        }
        nodeStatus.up();
    }

    private void fullSyncBlock(BlockChain blockChain, List<BlockChainHandler> peerHandlerList) {
        boolean retry = true;

        while (retry) {
            retry = false;
            for (BlockChainHandler peerHandler : peerHandlerList) {
                try {
                    boolean syncFinish = syncManager.syncBlock(peerHandler, blockChain);
                    if (!syncFinish) {
                        retry = true;
                    }
                } catch (Exception e) {
                    log.warn("[SyncManager] Sync Block ERR occurred: {}", e.getCause().getMessage());
                }
            }
        }
    }

    private void syncTransaction(BlockChain blockChain, List<BlockChainHandler> peerHandlerList) {

        for (BlockChainHandler peerHandler : peerHandlerList) {
            try {
                syncManager.syncTransaction(peerHandler, blockChain);
            } catch (Exception e) {
                log.warn("[SyncManager] Sync Tx ERR occurred: {}", e.getCause().getMessage());
            }
        }
    }

    public void setBranchGroup(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    protected void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public void setPeerNetwork(PeerNetwork peerNetwork) {
        this.peerNetwork = peerNetwork;
    }

    public void setSyncManager(SyncManager syncManager) {
        this.syncManager = syncManager;
    }
}
