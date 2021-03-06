/*
 * Copyright 2018 Akashic Foundation
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

package io.yggdrash.core.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.vo.PrefixKeyEnum;
import io.yggdrash.common.contract.vo.dpoa.ValidatorSet;
import io.yggdrash.common.store.BranchStateStore;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.contract.core.store.ReadWriterStore;
import io.yggdrash.core.blockchain.Branch;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.consensus.ConsensusBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BranchStore implements ReadWriterStore<String, JsonObject>, BranchStateStore {

    private static final Logger log = LoggerFactory.getLogger(BranchStore.class);

    private final ReadWriterStore<String, JsonObject> store;

    private List<BranchContract> contracts;

    BranchStore(ReadWriterStore<String, JsonObject> store) {
        this.store = store;
    }

    @Override
    public void put(String key, JsonObject value) {
        this.store.put(key, value);
    }

    @Override
    public JsonObject get(String key) {
        return this.store.get(key);
    }

    @Override
    public boolean contains(String key) {
        return this.store.contains(key);
    }

    @Override
    public void close() {
        this.store.close();
    }

    public Long getBestBlock() {
        return reStoreToLong(BlockchainMetaInfo.BEST_BLOCK_INDEX.toString(), -1);
    }

    public void setBestBlock(ConsensusBlock block) {
        setBestBlockHash(block.getHash());
        setBestBlock(block.getIndex());
    }

    private void setBestBlock(Long index) {
        storeLongValue(BlockchainMetaInfo.BEST_BLOCK_INDEX.toString(), index);
    }

    Sha3Hash getBestBlockHash() {

        JsonObject bestBlockHashObject = store.get(BlockchainMetaInfo.BEST_BLOCK.toString());
        Sha3Hash bestBlockHash = null;
        if (bestBlockHashObject != null) {
            bestBlockHash = new Sha3Hash(bestBlockHashObject.get("bestBlock").getAsString());
        }
        return bestBlockHash;
    }

    void setBestBlockHash(Sha3Hash hash) {
        JsonObject bestBlock = new JsonObject();
        bestBlock.addProperty("bestBlock", hash.toString());
        store.put(BlockchainMetaInfo.BEST_BLOCK.toString(), bestBlock);
    }

    public Long getLastExecuteBlockIndex() {
        return reStoreToLong(BlockchainMetaInfo.LAST_EXECUTE_BLOCK_INDEX.toString(), -1);
    }

    public Sha3Hash getLastExecuteBlockHash() {

        JsonObject lastBlockHashObject = store.get(BlockchainMetaInfo.LAST_EXECUTE_BLOCK.toString());
        Sha3Hash lastBlockHash = null;
        if (lastBlockHashObject != null) {
            lastBlockHash = new Sha3Hash(lastBlockHashObject.get("lastExecuteBlock").getAsString());
        }
        return lastBlockHash;
    }

    public void setLastExecuteBlock(ConsensusBlock block) {
        storeLongValue(BlockchainMetaInfo.LAST_EXECUTE_BLOCK_INDEX.toString(), block.getIndex());
        JsonObject lastExecuteBlock = new JsonObject();
        lastExecuteBlock.addProperty("lastExecuteBlock", block.getHash().toString());
        store.put(BlockchainMetaInfo.LAST_EXECUTE_BLOCK.toString(), lastExecuteBlock);
    }

    private Long reStoreToLong(String key, long defaultValue) {
        JsonObject value = this.store.get(key);
        if (value != null) {
            return value.get("value").getAsLong();
        } else {
            return defaultValue;
        }
    }

    private void storeLongValue(String key, long value) {
        JsonObject valueObject = new JsonObject();
        valueObject.addProperty("value", value);
        put(key, valueObject);
    }


    public void setBranch(Branch branch) {
        // if Exist Branch Information Did not save
        // Save Branch
        JsonObject json = branch.getJson();
        if (!store.contains(BlockchainMetaInfo.BRANCH.toString())) {
            store.put(BlockchainMetaInfo.BRANCH.toString(), json);
            JsonObject branchId = new JsonObject();
            branchId.addProperty("branchId", branch.getBranchId().toString());
            store.put(BlockchainMetaInfo.BRANCH_ID.toString(), branchId);
        }
    }

    public Branch getBranch() {
        // load Branch
        JsonObject json = store.get(BlockchainMetaInfo.BRANCH.toString());
        return Branch.of(json);
    }

    public BranchId getBranchId() {
        JsonObject branchId = store.get(BlockchainMetaInfo.BRANCH_ID.toString());
        return BranchId.of(branchId.get("branchId").getAsString());
    }
    // TODO UPDATE Branch - Version History

    // Set Genesis Block
    public boolean setGenesisBlockHash(Sha3Hash genesisBlockHash) {
        if (!store.contains(BlockchainMetaInfo.GENESIS_BLOCK.toString())) {
            JsonObject genesisBlock = new JsonObject();
            genesisBlock.addProperty("genesisBlock", genesisBlockHash.toString());
            store.put(BlockchainMetaInfo.GENESIS_BLOCK.toString(), genesisBlock);
            return true;
        }
        return false;
    }

    // Get Genesis Block
    public Sha3Hash getGenesisBlockHash() {
        JsonObject genesisBlock = store.get(BlockchainMetaInfo.GENESIS_BLOCK.toString());
        if (genesisBlock != null) {
            String genesisBlockHashString = genesisBlock.get("genesisBlock").getAsString();
            return new Sha3Hash(genesisBlockHashString);
        }
        return null;
    }

    @Override
    public Sha3Hash getBranchIdHash() {
        return null;
    }

    // Set Validator
    public void setValidators(ValidatorSet validatorSet) {
        JsonObject jsonValidator = JsonUtil.parseJsonObject(JsonUtil.convertObjToString(validatorSet));
        put(PrefixKeyEnum.VALIDATORS.toValue(), jsonValidator);
    }

    // Get Validator
    @Override
    public ValidatorSet getValidators() {
        ValidatorSet validatorSet = null;
        JsonObject jsonValidatorSet = get(PrefixKeyEnum.VALIDATORS.toValue());
        if (jsonValidatorSet != null) {
            validatorSet = JsonUtil.generateJsonToClass(jsonValidatorSet.toString(), ValidatorSet.class);
        }
        return validatorSet;
    }

    @Override
    public boolean isValidator(String address) {
        return getValidators().contains(address);
    }

    // Set Contracts
    // Save Contracts initial values
    public void setBranchContracts(List<BranchContract> contracts) {
        JsonArray array = new JsonArray();
        contracts.forEach(c -> array.add(c.getJson()));
        JsonObject contract = new JsonObject();
        contract.add("contracts", array);
        store.put(BlockchainMetaInfo.BRANCH_CONTRACTS.toString(), contract);

        // init contracts
        if (this.contracts != null) {
            this.contracts = null;
        }
    }

    /**
     * save new contract in store
     * @param contract return BranchContract
     */
    public void addBranchContract(BranchContract contract) {
        this.contracts.add(contract);
        setBranchContracts(this.contracts);
    }

    /**
     * delete contract in store
     * @param contractVersion return contractVersion
     */
    public void removeBranchContract(String contractVersion) {
        this.contracts.remove(getBranchContractByVersion(contractVersion));
        setBranchContracts(this.contracts);
    }

    /***
     * Get Branch's Contract List
     * @return Branch Contract List
     */
    public List<BranchContract> getBranchContacts() {
        if (this.contracts != null) {
            return new ArrayList<>(contracts);
        }
        contracts = new ArrayList<>();
        JsonObject contract = store.get(BlockchainMetaInfo.BRANCH_CONTRACTS.toString());
        if (contract == null) {
            return new ArrayList<>();
        } else {
            JsonArray contractArray = contract.get("contracts").getAsJsonArray();
            for (int i = 0; i < contractArray.size(); i++) {
                JsonObject contractObject = contractArray.get(i).getAsJsonObject();
                contracts.add(BranchContract.of(contractObject));
            }
        }
        return new ArrayList<>(contracts);
    }

    /**
     * get contract list by contract name.
     * @param contractName return contractName
     * @return
     */
    public List<BranchContract> getBranchContractsByName(String contractName) {
        List<BranchContract> result = new ArrayList<>();
        for (BranchContract bc: this.contracts) {
            if (bc.getName().equals(contractName)) {
                result.add(bc);
            }
        }
        return result;
    }

    /**
     * get branch contract by contract version.
     * @param contractVersion return contractVersion
     * @return BranchContract
     */
    private BranchContract getBranchContractByVersion(String contractVersion) {
        Optional<BranchContract> contract = this.contracts.stream()
                .filter(c -> contractVersion.equalsIgnoreCase(c.getContractVersion().toString()))
                .findFirst();
        return contract.orElse(null);
    }

    /**
     * Get contract version by contact name
     * @param contractName find contract Name
     * @return contract version
     */
    @Override
    public String getContractVersion(String contractName) {
        List<BranchContract> contractList = getBranchContacts().stream()
                .filter(c -> contractName.equalsIgnoreCase(c.getName()))
                .collect(Collectors.toList());
        if (!contractList.isEmpty()) {
            return contractList.get(contractList.size() - 1).getContractVersion().toString();
        }
        return null;
    }

    /**
     * Get contract name by contact version
     * @param contractVersion find contract version
     * @return contract name
     */
    @Override
    public String getContractName(String contractVersion) {
        // get contract Name by ContractVersion
        List<BranchContract> contractList = getBranchContacts().stream()
                .filter(c -> contractVersion.equalsIgnoreCase(c.getContractVersion().toString()))
                .collect(Collectors.toList());

        if (!contractList.isEmpty()) {
            return contractList.get(contractList.size() - 1).getName();
        }
        return null;
    }

    public enum  BlockchainMetaInfo {
        BEST_BLOCK,
        BEST_BLOCK_INDEX,
        LAST_EXECUTE_BLOCK,
        LAST_EXECUTE_BLOCK_INDEX,
        BRANCH,
        BRANCH_ID,
        GENESIS_BLOCK,
        VALIDATORS,
        BRANCH_CONTRACTS
    }
}
