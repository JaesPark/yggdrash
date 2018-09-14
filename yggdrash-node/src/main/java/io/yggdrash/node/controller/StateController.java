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

package io.yggdrash.node.controller;

import com.google.gson.JsonObject;
import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.BranchId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("branches/{branchId}")
public class StateController {

    private final BranchGroup branchGroup;

    @Autowired
    public StateController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @GetMapping("/states")
    public ResponseEntity getAll(@PathVariable(name = "branchId") String branchId) {
        Map<String, ?> state = branchGroup.getStateStore(BranchId.of(branchId)).getState();
        ArrayList<String> result = new ArrayList<>();
        for (Map.Entry entry : state.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof JsonObject) {
                ((JsonObject) value).addProperty("id", entry.getKey().toString());
                result.add(value.toString());
            } else {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", entry.getKey().toString());
                jsonObject.addProperty("value", "" + entry.getValue());
                result.add(jsonObject.toString());
            }
        }
        return ResponseEntity.ok(result);
    }
}
