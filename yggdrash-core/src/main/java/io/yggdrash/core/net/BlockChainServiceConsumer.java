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
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BlockChainServiceConsumer implements BlockChainConsumer {
    private static final Logger log = LoggerFactory.getLogger(BlockChainServiceConsumer.class);
    private final BranchGroup branchGroup;
    private CatchUpSyncEventListener listener;

    public BlockChainServiceConsumer(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @Override
    public void setListener(CatchUpSyncEventListener listener) {
        this.listener = listener;
    }

    private boolean isValidRequest(long curIndex, long reqIndex, Peer from) {
        // TODO limit maxDiff
        long maxDiffBetweenCurrentAndReceivedBlockHeight = 10000;
        if (curIndex < reqIndex) {
            long diff = reqIndex - curIndex;
            return diff < maxDiffBetweenCurrentAndReceivedBlockHeight;
        }
        // TODO 'from' is a bad peer! Add 'from' peer to the blacklist
        return false;
    }

    @Override
    public List<BlockHusk> syncBlock(BranchId branchId, long offset, long limit) {
        return syncBlock(branchId, offset, limit, null);
    }

    @Override
    public List<BlockHusk> syncBlock(BranchId branchId, long offset, long limit, Peer from) {
        long curBestBlock = branchGroup.getLastIndex(branchId);
        List<BlockHusk> blockHuskList = new ArrayList<>();
        if (isValidRequest(curBestBlock, offset, from)) {
            // Catchup Event!
            if (listener != null) {
                listener.catchUpRequest(branchId, offset);
            }
        } else {
            BlockChain blockChain = branchGroup.getBranch(branchId);

            if (blockChain == null) {
                log.warn("Invalid syncBlock request for branchId={}", branchId);
                return blockHuskList;
            }
            if (offset < 0) {
                offset = 0;
            }

            for (int i = 0; i < limit; i++) {
                BlockHusk block = branchGroup.getBlockByIndex(branchId, offset++);
                if (block == null) {
                    break;
                }
                blockHuskList.add(block);
            }
        }
        return blockHuskList;
    }

    @Override
    public List<TransactionHusk> syncTx(BranchId branchId) {
        return branchGroup.getUnconfirmedTxs(branchId);
    }

    @Override
    public void broadcastBlock(BlockHusk block) {
        try {
            long nextIndex = branchGroup.getLastIndex(block.getBranchId()) + 1;
            long receivedIndex = block.getIndex();

            if (nextIndex < receivedIndex) {
                // Catchup Event!
                if (listener != null) {
                    listener.catchUpRequest(block);
                } else {
                    log.debug("[BlockChainServiceConsumer] No listener!");
                }
            } else {
                branchGroup.addBlock(block, true);
            }
        } catch (Exception e) {
            log.warn("[BlockChainServiceConsumer] BroadcastBlock ERR={}", e.getMessage());
            /*
            if (listener != null) {
                listener.catchUpRequest(block);
            }
            */
        }
    }

    @Override
    public void broadcastTx(TransactionHusk tx) {
        try {
            branchGroup.addTransaction(tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
