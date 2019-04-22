/*
 * Copyright 2019 Akashic Foundation
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

package io.yggdrash.core.blockchain;

import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.blockchain.osgi.ContractContainer;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.store.TransactionReceiptStore;

import java.util.Collection;
import java.util.List;

public interface BlockChain<T, V> extends ConsensusBlockChain<T, V> {

    Branch getBranch();

    Block<T> addBlock(Block<T> block, boolean broadcast);

    TransactionHusk addTransaction(TransactionHusk tx);

    long getLastIndex();

    Collection<TransactionHusk> getRecentTxs();

    TransactionHusk getTxByHash(Sha3Hash hash);

    Block<T> getBlockByIndex(long index);

    Block<T> getBlockByHash(Sha3Hash hash);

    StateStore getStateStore();

    TransactionReceiptStore getTransactionReceiptStore();

    TransactionReceipt getTransactionReceipt(String transactionId);

    List<TransactionHusk> getUnconfirmedTxs();

    ContractContainer getContractContainer();

    long countOfTxs();

    List<BranchContract> getBranchContracts();

    void close();

    void addListener(BranchEventListener listener);
}
