package consensus;

import config.MiniChainConfig;
import data.*;
import network.NetWork;
import spv.Proof;
import spv.SpvPeer;
import utils.MinerUtil;
import utils.SecurityUtil;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MinerPeer extends Thread{

    private final BlockChain blockChain;
    private final NetWork network;
    public MinerPeer(BlockChain blockChain, NetWork netWork){
        this.network=netWork;
        this.blockChain=blockChain;
    }



    @Override
    public void run() {
        while(true){
            synchronized (network.getTransactionPool()){
                TransactionPool transactionPool=network.getTransactionPool();

                while(!transactionPool.isFull()){
                    try{
                        transactionPool.wait();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                Transaction[] transactions= transactionPool.getAll();

                if(!check(transactions)){
                    System.out.println("transactions error!");
                    System.exit(-1);
                }

                BlockBody blockBody = getBlockBody(transactions);

                Block block= mine(blockBody);

                boardcast(block);

                System.out.println("the sum of all account amount: "+blockChain.getAllAccountAmount());

                transactionPool.notify();
            }


        }
    }
    private boolean check(Transaction[] transactions){
        for (int i = 0; i < transactions.length; ++i) {
            Transaction transaction=transactions[i];
            byte[] data=SecurityUtil.utxos2Bytes(transaction.getInUtxos(), transaction.getOutUtxos());
            byte[] sign=transaction.getSendSign();
            PublicKey publicKey=transaction.getSendPublicKey();
            if(!SecurityUtil.verify(data, sign, publicKey)){
                return false;
            }
        }
        return  true;
    }

    /**
     * 该方法根据传入的参数中的交易，构造并返回一个相应的区块体对象
     *
     * 查看BlockBody类中的字段以及构造方法你会发现，还需要根据这些交易计算Merkle树的根哈希值
     *
     * @param transactions 一批次的交易
     *
     * @return 根据参数中的交易构造出的区块体
     */
    public BlockBody getBlockBody(Transaction[] transactions) {
        assert transactions != null && transactions.length == MiniChainConfig.MAX_TRANSACTION_COUNT;
        //todo
        List<String> list =new ArrayList<>();
        for(Transaction transaction:transactions){
            String txHash =SecurityUtil.sha256Digest(transaction.toString());
            list.add(txHash);
        }
        while(list.size() != 1){
            List<String> newList=new ArrayList<>();
            for(int i=0;i<list.size();i+=2){
                String leftHash= list.get(i);
                String rightHash=(i + 1< list.size() ? list.get(i+1) : leftHash);
                String parentHash=SecurityUtil.sha256Digest(leftHash + rightHash);
                newList.add(parentHash);
            }
            list=newList;
        }

        BlockBody blockBody=new BlockBody(list.get(0), transactions);

        return blockBody;
    }


    /**
     * 该方法即在循环中完成"挖矿"操作，其实就是通过不断的变换区块中的nonce字段，直至区块的哈希值满足难度条件，
     * 即可将该区块加入区块链中
     *
     * @param blockBody 区块体
     * @return
     */
    private Block mine(BlockBody blockBody) {
        Block block = getBlock(blockBody);
        while (true) {
            String blockHash = SecurityUtil.sha256Digest(block.toString());
            if (blockHash.startsWith(MinerUtil.hashPrefixTarget())) {
                System.out.println("Mined a new Block! Detail of the new Block : ");
                System.out.println(block.toString());
                System.out.println("And the hash of this Block is : " + SecurityUtil.sha256Digest(block.toString()) +
                        ", you will see the hash value in next Block's preBlockHash field.");
                System.out.println();
                blockChain.addNewBlock(block);
                return block;

//                break;
            } else {
                //todo
                block.getBlockHeader().setNonce(block.getBlockHeader().getNonce() + 1);

            }
        }
    }

    /**
     * 该方法供mine方法调用，其功能为根据传入的区块体参数，构造一个区块对象返回，
     * 也就是说，你需要构造一个区块头对象，然后用一个区块对象组合区块头和区块体
     *
     * 建议查看BlockHeader类中的字段和注释，有助于你实现该方法
     *
     * @param blockBody 区块体
     *
     * @return 相应的区块对象
     */
    public Block getBlock(BlockBody blockBody) {
        //todo
        Random random = new Random();
        // 获取最新的区块
        Block latestBlock = blockChain.getNewestBlock();

        // 创建一个新的区块头对象
        BlockHeader blockHeader = new BlockHeader( //    public BlockHeader(String preBlockHash, String merkleRootHash, long nonce)
                SecurityUtil.sha256Digest(latestBlock.toString()),  // 使用最新区块的哈希作为前一个区块的哈希
                blockBody.getMerkleRootHash(),
                random.nextLong()
        );

        // 使用新的区块头和提供的区块体创建一个新的区块
        return new Block(blockHeader, blockBody);
    }

    public Proof getProof(String proofTxHash){
        Block proofBlock =null;
        int proofHeight =-1;

        for(Block block:blockChain.getChain()){
            ++proofHeight;
            for(Transaction transaction:block.getBlockBody().getTransactions()){
                String txHash = SecurityUtil.sha256Digest(transaction.toString());
                if(txHash.equals(proofTxHash)){
                    proofBlock=block;
                    break;
                }
            }
            if(proofBlock != null){
                break;
            }
        }

        if(proofBlock==null){
            return null;
        }

        List<Proof.Node> proofPath=new ArrayList<>();
        List<String> list=new ArrayList<>();
        String pathHash=proofTxHash;
        for(Transaction transaction:proofBlock.getBlockBody().getTransactions()){
            String txHash =SecurityUtil.sha256Digest(transaction.toString());
            list.add(txHash);
        }
        while(list.size()!=1){
            List<String> newList=new ArrayList<>();
            for (int i = 0; i < list.size(); i += 2) {
                String leftHash= list.get(i);
                String rightHash=(i + 1 < list.size() ? list.get(i + 1) : leftHash);
                String parentHash = SecurityUtil.sha256Digest(leftHash+rightHash);
                newList.add(parentHash);

                if(pathHash.equals(leftHash)){
                    Proof.Node proopNode = new Proof.Node(rightHash, Proof.Orientation.RIGHT);
                    proofPath.add(proopNode);
                    pathHash=parentHash;
                }else if(pathHash.equals(rightHash)){
                    Proof.Node proopNode = new Proof.Node(leftHash, Proof.Orientation.LEFT);
                    proofPath.add(proopNode);
                    pathHash=parentHash;
                }
            }
            list=newList;
        }
        String proofMerkleRootHash=list.get(0);
        return new Proof(proofTxHash, proofMerkleRootHash,proofHeight, proofPath);
    }

    public void boardcast(Block block){
        SpvPeer spvPeer= network.getSpvPeers();
        spvPeer.accept(block.getBlockHeader());

    }
}
