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

package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchEventListener;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PeerHandlerGroup implements BranchEventListener {

    private static final Logger log = LoggerFactory.getLogger(PeerHandlerGroup.class);

    private final Map<PeerId, PeerHandler> handlerMap = new ConcurrentHashMap<>();

    private PeerHandlerFactory peerHandlerFactory;

    private PeerEventListener peerEventListener;

    public PeerHandlerGroup(PeerHandlerFactory peerHandlerFactory) {
        this.peerHandlerFactory = peerHandlerFactory;
    }

    public void setPeerEventListener(PeerEventListener peerEventListener) {
        this.peerEventListener = peerEventListener;
    }

    PeerHandlerFactory getPeerHandlerFactory() {
        return peerHandlerFactory;
    }

    public void destroy() {
        handlerMap.values().forEach(PeerHandler::stop);
    }

    public void healthCheck(Peer owner) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty to health check peer");
            return;
        }

        for (PeerHandler handler : new ArrayList<>(handlerMap.values())) {
            try {
                String pong = handler.ping("Ping", owner);
                if ("Pong".equals(pong)) {
                    continue;
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
            log.warn("Health check fail. peer=" + handler.getPeer().getYnodeUri());
            removeHandler(handler);
        }
    }

    @Override
    public void receivedTransaction(TransactionHusk tx) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty to broadcast transaction");
            return;
        }
        for (PeerHandler peerHandler : handlerMap.values()) {
            try {
                peerHandler.broadcastTransaction(tx);
            } catch (Exception e) {
                removeHandler(peerHandler);
            }
        }
    }

    @Override
    public void chainedBlock(BlockHusk block) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty to broadcast block");
            return;
        }
        for (PeerHandler peerHandler : handlerMap.values()) {
            try {
                peerHandler.broadcastBlock(block);
            } catch (Exception e) {
                removeHandler(peerHandler);
            }
        }
    }

    private void removeHandler(PeerHandler peerHandler) {
        peerHandler.stop();
        handlerMap.remove(peerHandler.getPeer().getPeerId());
        peerEventListener.peerDisconnected(peerHandler.getPeer());
        log.debug("Removed handler size={}", handlerMap.size());
    }

    int handlerCount() {
        return handlerMap.size();
    }

    void addHandler(Peer owner, Peer requestPeer) {
        PeerHandler peerHandler =  peerHandlerFactory.create(requestPeer);

        if (handlerMap.containsKey(requestPeer.getPeerId())) {
            return;
        }

        try {
            log.info("Connecting... peer {}:{}", requestPeer.getHost(), requestPeer.getPort());
            String pong = peerHandler.ping("Ping", owner);
            // TODO validation peer
            if ("Pong".equals(pong)) {
                handlerMap.put(requestPeer.getPeerId(), peerHandler);
                log.info("Added size={}, handler={}", handlerMap.size(), requestPeer.toAddress());
            } else {
                // 접속 실패 시 목록 및 버킷에서 제거
                peerEventListener.peerDisconnected(requestPeer);
            }
        } catch (Exception e) {
            log.warn("Fail to add to the peer handler err=" + e.getMessage());
        }
    }

    public List<String> getActivePeerList() {
        return handlerMap.values().stream()
                .map(handler -> handler.getPeer().toString())
                .collect(Collectors.toList());
    }

    public List<String> getActivePeerListOf() {
        return handlerMap
                .values()
                .stream()
                .map(c -> String.format("%s:%d", c.getPeer().getHost(), c.getPeer().getPort()))
                .collect(Collectors.toList());
    }

    /**
     * Sync block list.
     *
     * @param offset the offset
     * @return the block list
     */
    public List<BlockHusk> syncBlock(BranchId branchId, long offset) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty to sync block");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        PeerId key = (PeerId) handlerMap.keySet().toArray()[0];
        PeerHandler peerHandler = handlerMap.get(key);
        List<BlockHusk> blockList = peerHandler.syncBlock(branchId, offset);
        log.debug("Synchronize block offset={} receivedSize={}, from={}", offset, blockList.size(),
                peerHandler.getPeer());
        return blockList;
    }

    /**
     * Sync transaction list.
     *
     * @return the transaction list
     */
    public List<TransactionHusk> syncTransaction(BranchId branchId) {
        if (handlerMap.isEmpty()) {
            log.trace("Active peer is empty to sync transaction");
            return Collections.emptyList();
        }
        // TODO sync peer selection policy
        PeerId key = (PeerId) handlerMap.keySet().toArray()[0];
        PeerHandler peerHandler = handlerMap.get(key);
        List<TransactionHusk> txList = peerHandler.syncTransaction(branchId);
        log.info("Synchronize transaction receivedSize={}, from={}", txList.size(),
                peerHandler.getPeer());
        return txList;
    }
}
