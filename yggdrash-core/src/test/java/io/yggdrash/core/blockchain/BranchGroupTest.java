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

package io.yggdrash.core.blockchain;

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.exception.DuplicatedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static io.yggdrash.TestConstants.PerformanceTest;
import static io.yggdrash.TestConstants.TRANSFER_TO;
import static io.yggdrash.TestConstants.yggdrash;
import static org.assertj.core.api.Assertions.assertThat;

public class BranchGroupTest {

    private BranchGroup branchGroup;
    private Transaction tx;
    private ConsensusBlock block;
    protected static final Logger log = LoggerFactory.getLogger(BranchGroupTest.class);

    @Before
    public void setUp() {
        branchGroup = BlockChainTestUtils.createBranchGroup();
        tx = BlockChainTestUtils.createBranchTx();
        assertThat(branchGroup.getBranchSize()).isEqualTo(1);
        BlockChain bc = branchGroup.getBranch(tx.getBranchId());
        block = BlockChainTestUtils.createNextBlock(bc.getBlockChainManager().getLastConfirmedBlock());
    }

    @Test(expected = DuplicatedException.class)
    public void addExistedBranch() {
        BlockChain exist = BlockChainTestUtils.createBlockChain(false);
        branchGroup.addBranch(exist);
    }

    @Test
    public void addTransaction() {
        // should be existed tx on genesis block
        assertThat(branchGroup.getRecentTxs(tx.getBranchId()).size()).isEqualTo(3);
        assertThat(branchGroup.countOfTxs(tx.getBranchId())).isEqualTo(3);

        Assert.assertEquals(32000, branchGroup.addTransaction(tx));
        Transaction foundTxBySha3 = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash());
        assertThat(foundTxBySha3.getHash()).isEqualTo(tx.getHash());

        Transaction foundTxByString = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash().toString());
        assertThat(foundTxByString.getHash()).isEqualTo(tx.getHash());

        assertThat(branchGroup.getUnconfirmedTxs(tx.getBranchId()).size()).isEqualTo(1);

        Transaction invalidTx1 = BlockChainTestUtils
                .createInvalidTransferTx(BranchId.of("696e76616c6964"), ContractVersion.of("696e76616c696420"));
        Assert.assertEquals(33001, branchGroup.addTransaction(invalidTx1));

        Transaction invalidTx2 = BlockChainTestUtils
                .createInvalidTransferTx(ContractVersion.of("696e76616c696420"));
        Assert.assertEquals(33002, branchGroup.addTransaction(invalidTx2));
    }

    @Test
    public void generateBlock() {
        branchGroup.addTransaction(tx);
        BlockChainTestUtils.generateBlock(branchGroup, tx.getBranchId());
        long latest = branchGroup.getLastIndex(tx.getBranchId());
        ConsensusBlock chainedBlock = branchGroup.getBlockByIndex(tx.getBranchId(), latest);
        assertThat(latest).isEqualTo(1);
        assertThat(chainedBlock.getBody().getCount()).isEqualTo(1);
        assertThat(branchGroup.getTxByHash(tx.getBranchId(), tx.getHash()).getHash())
                .isEqualTo(tx.getHash());
    }

    /**
     * test generate block with large tx.
     */
    @Test(timeout = 5000L)
    public void generateBlockPerformanceTest() {
        PerformanceTest.apply();
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        for (int i = 0; i < 100; i++) {
            Transaction tx = createTx(i);
            blockChain.addTransaction(tx);
        }

        BlockChainTestUtils.generateBlock(branchGroup, blockChain.getBranchId());
        assertThat(blockChain.getBlockChainManager().countOfTxs()).isEqualTo(101); // include genesis tx
    }

    @Test
    public void addBlock() {
        branchGroup.addTransaction(tx);
        branchGroup.addBlock(block);
        ConsensusBlock newBlock = BlockChainTestUtils.createNextBlock(Collections.singletonList(tx), block);
        branchGroup.addBlock(newBlock);

        assertThat(branchGroup.getLastIndex(newBlock.getBranchId())).isEqualTo(2);
        assertThat(branchGroup.getBlockByIndex(newBlock.getBranchId(), 2).getHash())
                .isEqualTo(newBlock.getHash());
        Transaction foundTx = branchGroup.getTxByHash(tx.getBranchId(), tx.getHash());
        assertThat(foundTx.getHash()).isEqualTo(tx.getHash());
    }

    @Test
    public void specificBlockHeightOfBlockChain() {
        addMultipleBlock(block);
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        assertThat(blockChain.getBlockChainManager().getLastIndex()).isEqualTo(10);
    }

    private void addMultipleBlock(ConsensusBlock block) {
        BlockChain blockChain = branchGroup.getBranch(block.getBranchId());
        while (blockChain.getBlockChainManager().getLastIndex() < 10) {
            log.debug("Last Index : {}", blockChain.getBlockChainManager().getLastIndex());
            branchGroup.addBlock(block);
            ConsensusBlock nextBlock = BlockChainTestUtils.createNextBlock(Collections.emptyList(), block);
            addMultipleBlock(nextBlock);
        }
    }

    private Transaction createTx(int amount) {
        JsonObject txBody = ContractTestUtils.transferTxBodyJson(TRANSFER_TO, amount);
        return BlockChainTestUtils.createTx(yggdrash(), txBody);
    }
}
