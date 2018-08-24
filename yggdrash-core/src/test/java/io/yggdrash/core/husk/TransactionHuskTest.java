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

package io.yggdrash.core.husk;

import com.google.gson.JsonObject;
import io.yggdrash.core.TransactionHusk;
import io.yggdrash.core.Wallet;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.security.SignatureException;

public class TransactionHuskTest {

    @Test
    public void shouldBeVerifiedBySignature()
            throws IOException, InvalidCipherTextException, SignatureException {
        TransactionHusk transactionHusk = getTransactionHusk();
        Wallet wallet = new Wallet();
        Assertions.assertThat(transactionHusk.verify()).isFalse();

        transactionHusk.sign(wallet);
        Assertions.assertThat(transactionHusk.verify()).isTrue();
    }

    @Test
    public void shouldBeSignedTransaction() throws IOException, InvalidCipherTextException {
        TransactionHusk transactionHusk = getTransactionHusk();
        Assertions.assertThat(transactionHusk.isSigned()).isFalse();

        Wallet wallet = new Wallet();
        transactionHusk.sign(wallet);

        Assertions.assertThat(transactionHusk.isSigned()).isTrue();
        Assertions.assertThat(transactionHusk.verify());
    }

    @Test
    public void shouldBeCreatedNonSingedTransaction() {
        /* 외부에서 받는 정보
           - target - 블록체인 ID - String
           - from - 보내는 주소 - String
           - body - JSON - String
         */
        TransactionHusk transactionHusk = getTransactionHusk();
        Assertions.assertThat(transactionHusk).isNotNull();
    }

    private TransactionHusk getTransactionHusk() {
        JsonObject body = new JsonObject();
        body.addProperty("func", "transfer");
        JsonObject params = new JsonObject();
        params.addProperty("to", "0x407d73d8a49eeb85d32cf465507dd71d507100c1");
        params.addProperty("value", "1000");
        body.add("params", params);
        return new TransactionHusk(body);
    }

}