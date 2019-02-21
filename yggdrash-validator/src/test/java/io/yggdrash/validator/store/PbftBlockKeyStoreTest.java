package io.yggdrash.validator.store;

import io.yggdrash.StoreTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.store.datasource.LevelDbDataSource;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import io.yggdrash.validator.store.pbft.PbftBlockKeyStore;
import io.yggdrash.validator.util.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.yggdrash.common.config.Constants.EMPTY_BYTE1K;
import static io.yggdrash.common.config.Constants.PBFT_COMMIT;
import static io.yggdrash.common.config.Constants.PBFT_PREPARE;
import static io.yggdrash.common.config.Constants.PBFT_PREPREPARE;
import static io.yggdrash.common.config.Constants.PBFT_VIEWCHANGE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PbftBlockKeyStoreTest {
    private static final Logger log = LoggerFactory.getLogger(PbftBlockKeyStoreTest.class);

    private Wallet wallet;
    private Wallet wallet2;
    private Wallet wallet3;
    private Wallet wallet4;

    private Block block;
    private Block block2;
    private Block block3;
    private Block block4;

    private PbftBlock pbftBlock;
    private PbftBlock pbftBlock2;
    private PbftBlock pbftBlock3;
    private PbftBlock pbftBlock4;

    private PbftMessage prePrepare;
    private PbftMessage prepare;
    private PbftMessage prepare2;
    private PbftMessage prepare3;
    private PbftMessage prepare4;

    private PbftMessage commit;
    private PbftMessage commit2;
    private PbftMessage commit3;
    private PbftMessage commit4;

    private PbftMessage viewChange;
    private PbftMessage viewChange2;
    private PbftMessage viewChange3;
    private PbftMessage viewChange4;

    private PbftMessageSet pbftMessageSet;
    private PbftMessageSet pbftMessageSet2;
    private PbftMessageSet pbftMessageSet3;
    private PbftMessageSet pbftMessageSet4;
    private Map<String, PbftMessage> prepareMap = new TreeMap<>();
    private Map<String, PbftMessage> commitMap = new TreeMap<>();
    private Map<String, PbftMessage> viewChangeMap = new TreeMap<>();

    private LevelDbDataSource ds;
    private PbftBlockKeyStore blockKeyStore;

    @Before
    public void setUp() throws IOException, InvalidCipherTextException {
        wallet = new Wallet();
        wallet2 = new Wallet(null, "/tmp/", "test2", "Password1234!");
        wallet3 = new Wallet(null, "/tmp/", "test3", "Password1234!");
        wallet4 = new Wallet(null, "/tmp/", "test4", "Password1234!");

        block = new TestUtils(wallet).sampleBlock();
        block2 = new TestUtils(wallet).sampleBlock(block.getIndex() + 1, block.getHash());
        block3 = new TestUtils(wallet).sampleBlock(block2.getIndex() + 1, block2.getHash());
        block4 = new TestUtils(wallet).sampleBlock(block3.getIndex() + 1, block3.getHash());

        prePrepare = new PbftMessage(PBFT_PREPREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                block);
        log.debug(prePrepare.toJsonObject().toString());

        prepare = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(prepare.toJsonObject().toString());

        prepare2 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        log.debug(prepare2.toJsonObject().toString());

        prepare3 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        log.debug(prepare3.toJsonObject().toString());

        prepare4 = new PbftMessage(PBFT_PREPARE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);
        log.debug(prepare4.toJsonObject().toString());

        commit = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(commit.toJsonObject().toString());

        commit2 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        log.debug(commit2.toJsonObject().toString());

        commit3 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        log.debug(commit3.toJsonObject().toString());

        commit4 = new PbftMessage(PBFT_COMMIT,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);
        log.debug(commit4.toJsonObject().toString());

        viewChange = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet,
                null);
        log.debug(viewChange.toJsonObject().toString());

        viewChange2 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet2,
                null);
        log.debug(viewChange2.toJsonObject().toString());

        viewChange3 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet3,
                null);
        log.debug(viewChange3.toJsonObject().toString());

        viewChange4 = new PbftMessage(PBFT_VIEWCHANGE,
                0L,
                0L,
                block.getHash(),
                null,
                wallet4,
                null);
        log.debug(viewChange4.toJsonObject().toString());

        prepareMap.put(prepare.getSignatureHex(), prepare);
        prepareMap.put(prepare2.getSignatureHex(), prepare2);
        prepareMap.put(prepare3.getSignatureHex(), prepare3);
        prepareMap.put(prepare4.getSignatureHex(), prepare4);

        commitMap.put(commit.getSignatureHex(), commit);
        commitMap.put(commit2.getSignatureHex(), commit2);
        commitMap.put(commit3.getSignatureHex(), commit3);
        commitMap.put(commit4.getSignatureHex(), commit4);

        viewChangeMap.put(viewChange.getSignatureHex(), viewChange);
        viewChangeMap.put(viewChange2.getSignatureHex(), viewChange2);
        viewChangeMap.put(viewChange3.getSignatureHex(), viewChange3);
        viewChangeMap.put(viewChange4.getSignatureHex(), viewChange4);

        pbftMessageSet = new PbftMessageSet(prePrepare, prepareMap, commitMap, null);
        pbftMessageSet2 = new PbftMessageSet(prePrepare, null, null, null);
        pbftMessageSet3 = new PbftMessageSet(prePrepare, prepareMap, null, null);
        pbftMessageSet4 = new PbftMessageSet(prePrepare, prepareMap, commitMap, viewChangeMap);

        this.pbftBlock = new PbftBlock(this.block, this.pbftMessageSet);
        this.pbftBlock2 = new PbftBlock(this.block2, this.pbftMessageSet2);
        this.pbftBlock3 = new PbftBlock(this.block3, this.pbftMessageSet3);
        this.pbftBlock4 = new PbftBlock(this.block4, this.pbftMessageSet4);

        StoreTestUtils.clearTestDb();

        this.ds = new LevelDbDataSource(StoreTestUtils.getTestPath(), "pbftBlockKeyStoreTest");
        this.blockKeyStore = new PbftBlockKeyStore(ds);
        this.blockKeyStore.put(this.pbftBlock.getIndex(), this.pbftBlock.getHash());
    }

    @Test
    public void putGetTest() {
        byte[] newHash = blockKeyStore.get(this.pbftBlock.getIndex());
        assertArrayEquals(this.pbftBlock.getHash(), newHash);
        assertTrue(blockKeyStore.contains(this.pbftBlock.getIndex()));
        assertFalse(blockKeyStore.contains(this.pbftBlock.getIndex() + 1));
        assertFalse(blockKeyStore.contains(-1L));
        assertEquals(blockKeyStore.size(), 1);
    }

    @Test
    public void putTest_NegativeNumber() {
        int beforeSize = blockKeyStore.size();
        blockKeyStore.put(-1L, this.pbftBlock.getHash());
        assertEquals(blockKeyStore.size(), beforeSize);
    }

    @Test
    public void getTest_NegativeNumber() {
        assertNull(blockKeyStore.get(-1L));
    }

    @Test
    public void closeTest() {
        blockKeyStore.close();
        try {
            blockKeyStore.get(this.pbftBlock.getIndex());
        } catch (NullPointerException ne) {
            assert true;
            this.blockKeyStore = new PbftBlockKeyStore(ds);
            byte[] newHash = blockKeyStore.get(this.pbftBlock.getIndex());
            assertArrayEquals(newHash, this.pbftBlock.getHash());
            return;
        }
        assert false;
    }

    @Test
    public void memoryTest() {
        TestConstants.PerformanceTest.apply();

        long testNumber = 1000000;
        byte[] result;
        List<byte[]> resultList = new ArrayList<>();

        log.debug("Before free memory: " + Runtime.getRuntime().freeMemory());
        for (long l = 0L; l < testNumber; l++) {
            this.blockKeyStore.put(l, EMPTY_BYTE1K);
            result = this.blockKeyStore.get(l);
            resultList.add(result);
        }
        resultList.clear();
        log.debug("blockKeyStore size: " + this.blockKeyStore.size());
        log.debug("After free memory: " + Runtime.getRuntime().freeMemory());
        assertEquals(this.blockKeyStore.size(), testNumber);

//        System.gc();
//        sleep(20000);
    }

}