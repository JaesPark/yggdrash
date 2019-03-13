package io.yggdrash.validator.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@GRpcService
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "ebft")
public class NodeGrpcService extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(NodeGrpcService.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final EbftBlockChain ebftBlockChain;

    @Autowired
    public NodeGrpcService(EbftBlockChain ebftBlockChain) {
        this.ebftBlockChain = ebftBlockChain;
    }

    @Override
    public void syncBlock(NetProto.SyncLimit syncLimit,
                                StreamObserver<Proto.BlockList> responseObserver) {
        log.debug("NodeService syncBlock");
        long offset = syncLimit.getOffset();
        long limit = syncLimit.getLimit();
        log.debug("syncBlock() request offset={}, limit={}", offset, limit);

        Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
        if (Arrays.equals(syncLimit.getBranch().toByteArray(), ebftBlockChain.getChain())
                && offset >= 0
                && offset <= ebftBlockChain.getLastConfirmedBlock().getIndex()) {
            List<EbftBlock> ebftBlockList = ebftBlockChain.getEbftBlockList(offset, limit);
            for (EbftBlock ebftBlock : ebftBlockList) {
                builder.addBlocks(ebftBlock.getBlock().toProtoBlock());
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void syncTx(NetProto.SyncLimit syncLimit,
                                      StreamObserver<Proto.TransactionList> responseObserver) {
        log.debug("NodeService syncTransaction");
        long offset = syncLimit.getOffset();
        long limit = syncLimit.getLimit();
        log.debug("syncTransaction() request offset={}, limit={}", offset, limit);

        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        if (Arrays.equals(syncLimit.getBranch().toByteArray(), ebftBlockChain.getChain())) {
            //todo: check memory leak
            for (TransactionHusk husk :
                    new ArrayList<>(ebftBlockChain.getTransactionStore().getUnconfirmedTxs())) {
                builder.addTransactions(husk.getInstance());
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Proto.Block> broadcastBlock(StreamObserver<NetProto.Empty> responseObserver) {
        return new StreamObserver<Proto.Block>() {
            @Override
            public void onNext(Proto.Block value) {
                log.warn("ignored broadcast block");
            }

            @Override
            public void onError(Throwable t) {
                log.warn(t.getMessage());
            }

            @Override
            public void onCompleted() {
                // Validator do not need to receive blocks from general node
                log.debug("NodeService broadcastBlock");
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Proto.Transaction> broadcastTx(StreamObserver<NetProto.Empty> responseObserver) {
        return new StreamObserver<Proto.Transaction>() {
            @Override
            public void onNext(Proto.Transaction value) {
                log.debug("NodeService broadcastTransaction");
                log.debug("Received transaction: {}", value);
                TransactionHusk tx = new TransactionHusk(value);
                if (Arrays.equals(tx.getBranchId().getBytes(), ebftBlockChain.getChain())) {
                    ebftBlockChain.getTransactionStore().put(tx.getHash(), tx);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn(t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
            }
        };
    }
}
