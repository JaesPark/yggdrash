/*
 * Copyright 2019 Akashic Foundation
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package io.yggdrash.core.blockchain;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.core.consensus.ConsensusBlock;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BlockChainManager<T> {

    void initGenesis(Block genesisBlock);

    void loadTransaction();

    int verify(ConsensusBlock<T> block);

    int verify(Transaction transaction);

    void addBlock(ConsensusBlock<T> nextBlock);

    //void addBlock(ConsensusBlock<T> nextBlock, Sha3Hash stateRoot);

    void batchTxs(ConsensusBlock<T> block, Sha3Hash stateRoot);

    void addTransaction(Transaction tx);

    void addTransaction(Transaction tx, Sha3Hash stateRootHash);

    void flushUnconfirmedTxs(Set<Sha3Hash> keys);

    void flushUnconfirmedTx(Sha3Hash key);

    void updateTxCache(Block block);

    //void setPendingStateRoot(Sha3Hash stateRootHash);

    ConsensusBlock<T> getLastConfirmedBlock();

    ConsensusBlock<T> getBlockByHash(Sha3Hash hash);

    ConsensusBlock<T> getBlockByIndex(long index);

    Transaction getTxByHash(Sha3Hash hash);

    Collection<Transaction> getRecentTxs();

    List<Transaction> getUnconfirmedTxs();

    Map<Sha3Hash, List<Transaction>> getUnconfirmedTxsWithStateRoot();

    List<Transaction> getUnconfirmedTxsWithLimit(long limit);

    int getUnconfirmedTxsSize();

    Receipt getReceipt(String txId);

    Sha3Hash getLastHash();

    long getLastIndex();

    long countOfTxs();

    long countOfBlocks();

    boolean containsBlockHash(Sha3Hash blockHash);

    boolean containsTxHash(Sha3Hash txHash);

    boolean contains(Block block);

    boolean contains(Transaction transaction);

    void close();
}
