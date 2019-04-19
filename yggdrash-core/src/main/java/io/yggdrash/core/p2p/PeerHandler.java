package io.yggdrash.core.p2p;

import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;

import java.util.List;
import java.util.concurrent.Future;

public interface PeerHandler {
    List<Peer> findPeers(BranchId branchId, Peer targetPeer);

    String ping(BranchId branchId, Peer owner, String message);

    Peer getPeer();

    void stop();

    Future<List<ConsensusBlock>> syncBlock(BranchId branchId, long offset);

    Future<List<Transaction>> syncTx(BranchId branchId);

    void broadcastBlock(ConsensusBlock block);

    void broadcastTx(Transaction txHusk);
}
