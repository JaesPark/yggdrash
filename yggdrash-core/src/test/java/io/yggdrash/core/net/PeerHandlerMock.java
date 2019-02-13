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

package io.yggdrash.core.net;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.yggdrash.TestConstants.wallet;

public class PeerHandlerMock implements PeerHandler {
    public static final PeerHandlerFactory factory = PeerHandlerMock::dummy;
    private static final String NODE_URI_PREFIX = "ynode://75bff16c@127.0.0.1:";
    private static final Peer OWNER = Peer.valueOf(NODE_URI_PREFIX + 32920);

    private final Peer peer;
    private boolean pongResponse = true;

    private PeerHandlerMock(String ynodeUri) {
        this.peer = Peer.valueOf(ynodeUri);
    }

    @Override
    public List<Peer> findPeers(BranchId branchId, Peer peer) {
        List<Peer> result = new ArrayList<>();

        if (OWNER.equals(peer)) {
            for (int port = 32921; port < 32927; port++) { // return 6 peers
                result.add(Peer.valueOf(NODE_URI_PREFIX + port));
            }
        } else {
            for (int port = 32950; port < 32955; port++) { // return 5 peers
                result.add(Peer.valueOf(NODE_URI_PREFIX + port));
            }
        }
        return result;
    }

    @Override
    public Peer getPeer() {
        return peer;
    }

    @Override
    public void stop() {
    }

    @Override
    public String ping(BranchId branchId, Peer owner, String message) {
        if (pongResponse) {
            pongResponse = false;
            return "Pong";
        }
        pongResponse = true;
        return null;
    }

    @Override
    public List<BlockHusk> syncBlock(BranchId branchId, long offset) {
        if (offset == 1) {
            BlockHusk prevBlock = BlockChainTestUtils.genesisBlock();
            BlockHusk newBlock = new BlockHusk(wallet(), Collections.emptyList(), prevBlock);
            return Collections.singletonList(newBlock);
        }
        return Collections.emptyList();
    }

    @Override
    public List<TransactionHusk> syncTransaction(BranchId branchId) {
        return Collections.singletonList(BlockChainTestUtils.createTransferTxHusk());
    }

    @Override
    public void broadcastBlock(BlockHusk blockHusk) {

    }

    @Override
    public void broadcastTransaction(TransactionHusk txHusk) {

    }

    public static PeerHandler dummy(Peer peer) {
        return new PeerHandlerMock(peer.getYnodeUri());
    }
}
