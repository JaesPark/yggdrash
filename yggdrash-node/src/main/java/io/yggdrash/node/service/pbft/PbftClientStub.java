package io.yggdrash.node.service.pbft;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.yggdrash.core.blockchain.pbft.PbftBlock;
import io.yggdrash.core.blockchain.pbft.PbftStatus;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.CommonProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.proto.PbftServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PbftClientStub {

    private static final Logger log = LoggerFactory.getLogger(PbftClientStub.class);

    private boolean myclient;
    private String pubKey;
    private String host;
    private int port;
    private String id;
    private boolean isRunning;
    private PbftStatus pbftStatus;

    private ManagedChannel channel;
    private PbftServiceGrpc.PbftServiceBlockingStub blockingStub;

    public PbftClientStub(String pubKey, String host, int port) {
        this.pubKey = pubKey;
        this.host = host;
        this.port = port;
        this.id = this.pubKey + "@" + this.host + ":" + this.port;
        this.isRunning = false;

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = PbftServiceGrpc.newBlockingStub(channel);
    }

    public void multicastPbftMessage(PbftProto.PbftMessage pbftMessage) {
        blockingStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .multicastPbftMessage(pbftMessage);
    }

    public List<PbftBlock> getBlockList(long index) {
        PbftProto.PbftBlockList protoBlockList = blockingStub
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .getPbftBlockList(
                        CommonProto.Offset.newBuilder().setIndex(index).setCount(10L).build());

        if (Context.current().isCancelled()) {
            return null;
        }

        List<PbftBlock> newPbftBlockList = new ArrayList<>();
        for (PbftProto.PbftBlock protoPbftBlock : protoBlockList.getPbftBlockList()) {
            newPbftBlockList.add(new PbftBlock(protoPbftBlock));
        }

        return newPbftBlockList;
    }

    public long pingPongTime(long timestamp) {
        CommonProto.PingTime pingTime =
                CommonProto.PingTime.newBuilder().setTimestamp(timestamp).build();
        CommonProto.PongTime pongTime;
        try {
            pongTime = blockingStub
                    .withDeadlineAfter(1, TimeUnit.SECONDS)
                    .pingPongTime(pingTime);
        } catch (StatusRuntimeException e) {
            return 0L;
        }

        if (Context.current().isCancelled()) {
            return 0L;
        }

        return pongTime.getTimestamp();
    }

    public PbftStatus exchangePbftStatus(PbftProto.PbftStatus pbftStatus) {
        this.pbftStatus = new PbftStatus(blockingStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .exchangePbftStatus(pbftStatus));
        if (Context.current().isCancelled()) {
            return null;
        }
        return this.pbftStatus;
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public boolean isMyclient() {
        return myclient;
    }

    public void setMyclient(boolean myclient) {
        this.myclient = myclient;
    }

    public String getPubKey() {
        return pubKey;
    }

    public String getAddress() {
        return Hex.toHexString(Wallet.calculateAddress(Hex.decode(this.pubKey)));
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public PbftServiceGrpc.PbftServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    @Override
    public String toString() {
        return this.pubKey + "@" + this.host + ":" + this.port;
    }

}
