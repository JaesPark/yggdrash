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

package io.yggdrash.gateway.controller;

import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.gateway.dto.BranchDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("branches")
public class BranchController {
    private final BranchGroup branchGroup;

    @Autowired
    public BranchController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @GetMapping
    public ResponseEntity<Map<String, BranchDto>> getBranches() {
        Map<String, BranchDto> branchMap = new HashMap<>();
        branchGroup.getAllBranch().forEach(blockChain ->
                branchMap.put(blockChain.getBranchId().toString(),
                        BranchDto.of(blockChain.getBranch().getJson())));
        return ResponseEntity.ok(branchMap);
    }

}
