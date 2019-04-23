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

package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.crypto.HexUtil;
import io.yggdrash.common.utils.ByteUtil;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.gateway.dto.TransactionDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.yggdrash.node.api.JsonRpcConfig.BLOCK_API;
import static io.yggdrash.node.api.JsonRpcConfig.TX_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class TransactionApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionApiImplTest.class);

    private final int blockNumber = 3;
    private final int txIndexPosition = 2;
    private final String yggdrashBranch = TestConstants.yggdrash().toString();

    @Before
    public void setUp() {
        sendTransactionTest();
    }

    @Test
    public void blockApiIsNotNull() {
        assertThat(BLOCK_API).isNotNull();
    }

    @Test
    public void txApiIsNotNull() {
        assertThat(TX_API).isNotNull();
    }

    @Test
    public void getBlockTransactionCountByHashTest() {
        try {
            assertThat(TX_API.getTransactionCountByBlockHash(yggdrashBranch,
                    "d52fffa14f5b88b141d05d8e28c90d8131db1aa63e076bfea9c28c3060049e12"))
                    .isNotZero();
        } catch (Exception exception) {
            log.debug("\n\ngetBlockTransactionCountByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getBlockTransactionCountByNumberTest() {
        try {
            assertThat(TX_API.getTransactionCountByBlockNumber(yggdrashBranch, blockNumber))
                    .isNotZero();
        } catch (Throwable exception) {
            log.debug("\n\ngetBlockTransactionCountByNumberTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByHashTest() {
        try {
            assertThat(TX_API.getTransactionByHash(yggdrashBranch,
                    "f5912fde84c6a3a44b4e529077ca9bf28feccd847137e44a77cd17e9fb9c1353"))
                    .isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockHashTest() {
        try {
            assertThat(TX_API.getTransactionByBlockHash(yggdrashBranch,
                    "5ef71a90c6d99c7bc13bfbcaffb50cb89210678e99ed6626c9d2f378700b392c",
                    2)).isNotNull();
        } catch (Exception exception) {
            log.debug("\n\ngetTransactionByBlockHashTest :: exception => " + exception);
        }
    }

    @Test
    public void getTransactionByBlockNumberTest() {
        try {
            assertThat(TX_API.getTransactionByBlockNumber(
                    yggdrashBranch, blockNumber, txIndexPosition))
                    .isNotNull();
        } catch (Exception e) {
            log.debug("\n\ngetTransactionByBlockNumberTest :: exception => " + e);
        }
    }

    @Test
    public void getTransactionByBlockNumberWithTagTest() {
        try {
            String tag = "latest";
            Assert.assertNotNull(TX_API.getTransactionByBlockNumber(yggdrashBranch, tag, txIndexPosition));
        } catch (Exception e) {
            log.debug("\n\ngetTransactionByBlockNumberWithTagTest :: exception => " + e);
        }
    }

    @Test
    public void checkTransactionJsonFormat() throws IOException {
        TransactionHusk tx = createTx();
        String txString = new ObjectMapper().writeValueAsString(TransactionDto.createBy(tx));
        Assert.assertTrue(txString.contains(tx.getBranchId().toString()));
    }

    @Test
    public void sendTransactionTest() {
        TransactionHusk tx = createTx();

        // Request Transaction with jsonStr
        try {
            assertThat(TX_API.sendTransaction(TransactionDto.createBy(tx))).isNotEmpty();
        } catch (Exception exception) {
            log.debug("\n\nsendTransactionTest :: exception => " + exception);
        }
    }

    @Test
    public void sendRawTransactionTest() {
        // Request Transaction with byteArr
        try {
            byte[] input = createTx().toBinary();
            // Convert byteArray to Transaction
            assertThat(TX_API.sendRawTransaction(input)).isNotEmpty();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    @Test
    public void sendRawTransactionTest2() {
        String byteArray = "a5f436a66ce5ca5b7dbd6bbf8460701b8cbf0485000000000000000000000000000000000000016a498c743a0100879b01624d75b33aafb7861c701c1815f8ef56e9db2c8542e61bc31d146400000000000000611b0078ca8017a4b695d809dd733e7e238dec4f5638436ed2e45871c835eaec9e1f02708a739f236a45c4f52b23c98cf4691ad748cf56a4ec801e7b8eed3021b6455b7b226d6574686f64223a227472616e73666572222c22706172616d223a7b22746f223a2232646265353838646137306361666539386264313739373131396539363136356138653734313931222c22616d6f756e74223a2231303030227d7d5d";

        byte[] input = HexUtil.hexStringToBytes(byteArray);
        Transaction tx = new Transaction(input);
        TransactionHusk transaction = new TransactionHusk(tx);
        log.debug(tx.getHeader().toString());
        log.debug(transaction.toJsonObject().toString());

    }


    @Test
    public void newPendingTransactionFilterTest() {
        try {
            assertThat(TX_API.newPendingTransactionFilter(yggdrashBranch))
                    .isGreaterThanOrEqualTo(0);
        } catch (Exception e) {
            log.debug("\n\nnewPendingTransactionFilterTest :: exception => " + e);
        }
    }

    @Test
    public void txSigValidateTest() throws IOException {
        // Create Transaction
        TransactionHusk tx = createTx();

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(TransactionDto.createBy(tx));

        // Receive Transaction
        TransactionDto resDto = mapper.readValue(jsonStr, TransactionDto.class);

        // Signature Validation
        assertTrue(TransactionDto.of(resDto).verify());
    }

    private TransactionHusk createTx() {
        return BlockChainTestUtils.createTransferTxHusk();
    }
}
