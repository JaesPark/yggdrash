package io.yggdrash.validator.service.node;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class NodeServerStub extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(NodeServerStub.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final ConsensusBlockChain blockChain;

    public NodeServerStub(ConsensusBlockChain blockChain) {
        this.blockChain = blockChain;
    }

    @Override
    public void syncBlock(NetProto.SyncLimit syncLimit,
                                StreamObserver<Proto.BlockList> responseObserver) {
        log.debug("NodeService syncBlock");
        long offset = syncLimit.getOffset();
        long limit = syncLimit.getLimit();
        log.debug("syncBlock() request offset={}, limit={}", offset, limit);

        Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
        if (Arrays.equals(syncLimit.getBranch().toByteArray(), blockChain.getBranchId().getBytes())
                && offset >= 0
                && offset <= blockChain.getLastConfirmedBlock().getIndex()) {
            List blockList = blockChain.getBlockList(offset, limit);
            for (Object object : blockList) {
                ConsensusBlock consensusBlock = (ConsensusBlock) object;
                if (consensusBlock.getBlock() != null) {
                    builder.addBlocks(consensusBlock.getProtoBlock());
                }
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
        if (Arrays.equals(syncLimit.getBranch().toByteArray(), blockChain.getBranchId().getBytes())) {
            //todo: check memory leak
            for (Transaction tx : blockChain.getTransactionStore().getUnconfirmedTxs()) {
                builder.addTransactions(tx.getInstance());
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
                Transaction tx = new TransactionImpl(value);
                if (tx.getBranchId().equals(blockChain.getBranchId())) {
                    blockChain.getTransactionStore().put(tx.getHash(), tx);
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
