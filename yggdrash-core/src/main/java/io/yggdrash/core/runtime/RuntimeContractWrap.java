/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.core.runtime;

import com.google.gson.JsonObject;
import io.yggdrash.core.contract.Contract;
import io.yggdrash.core.contract.ContractId;
import io.yggdrash.core.contract.TransactionReceipt;
import io.yggdrash.core.store.Store;
import io.yggdrash.core.store.TempStateStore;
import java.lang.reflect.InvocationTargetException;

public class RuntimeContractWrap {
    private RuntimeInvoke contractInvoke;
    private RuntimeQuery runtimeQuery;
    private Contract contract;
    private ContractId contractId;

    public RuntimeContractWrap(ContractId contractId, Contract contract) {
        this.contract = contract;
        contractInvoke = new RuntimeInvoke(contract);
        runtimeQuery = new RuntimeQuery(contract);
    }

    public void setStore(Store store) {
        runtimeQuery.setStore(store);
    }

    public ContractId getContractId() {
        return this.contractId;
    }


    public TempStateStore invokeTransaction(JsonObject txBody, TransactionReceipt txReceipt, Store origin)
            throws InvocationTargetException, IllegalAccessException {
        return this.contractInvoke.invokeTransaction(txBody, txReceipt, origin);
    }

    public Object query(String method, JsonObject params) throws Exception {
        return runtimeQuery.query(method, params);
    }

}