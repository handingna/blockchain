package network;

import config.MiniChainConfig;
import consensus.MinerPeer;
import consensus.TransactionProducer;
import data.*;
import spv.SpvPeer;
import utils.SecurityUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * 该类模拟一个网络环境，在该网络中主要有区块链和矿工，另外地，出于工程实现的角度，还有一个交易池和一个生成随机交易的线程
 *
 */
public class NetWork {

//    private final Account[] accounts;
    private final List<Account> accounts=new ArrayList<>();

    private final TransactionPool transactionPool;
//    private final SpvPeer[] spvPeers;
//
//    private final TransactionProducer transactionProducer;
    private final SpvPeer spvPeer;

    private final BlockChain blockChain ;
    private final MinerPeer minerPeer;

    /**
     * 系统中几个主要成员的初始化
     */
    public NetWork() {

        System.out.println("\naccounta and spvPeers config...");
//        accounts=new Account[MiniChainConfig.ACCOUNT_NUM];
//        spvPeers=new SpvPeer[MiniChainConfig.ACCOUNT_NUM];
        for (int i = 0; i < MiniChainConfig.ACCOUNT_NUM; ++i) {
//            accounts[i]=new Account();
//            System.out.println("network register new account:"+accounts[i]);
//            spvPeers[i]=new SpvPeer(accounts[i], this);
            Account a =new Account();
            accounts.add(a);
            System.out.println("network register new account: "+accounts.get(i));
        }

        System.out.println("\ntransactionPool config...");
        transactionPool=new TransactionPool(MiniChainConfig.MAX_TRANSACTION_COUNT);

//        System.out.println("\ntransactionProducer config...");
//        transactionProducer = new TransactionProducer(this);
        spvPeer=new SpvPeer(null, this);

        System.out.println("\nblockChain config...");
        blockChain=new BlockChain(this);

        System.out.println("\nminerPeer config...");
        minerPeer =new MinerPeer(blockChain, this);

        System.out.println("\nnetwork start!\n");

        minerPeer.boardcast(blockChain.getNewestBlock());

        theyHaveDayDream();
    }

    public SpvPeer getSpvPeers() {
        return spvPeer;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public BlockChain getBlockChain() {
        return blockChain;
    }

    public MinerPeer getMinerPeer() {
        return minerPeer;
    }

    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

//    public TransactionProducer getTransactionProducer() {
//        return transactionProducer;
//    }


    public void theyHaveDayDream(){
        UTXO[] outUtxos =new UTXO[accounts.size()];
        for (int i = 0; i < accounts.size(); ++i) {
            outUtxos[i]=new UTXO(accounts.get(i).getWalletAddress(), MiniChainConfig.INIT_AMOUNT, accounts.get(i).getPublicKey());
        }
        KeyPair dayDreamKeyPair = SecurityUtil.secp256k1Generate();
        PublicKey dayDreamPublicKey= dayDreamKeyPair.getPublic();
        PrivateKey dayDreamPrivateKey = dayDreamKeyPair.getPrivate();
        byte[] sign =SecurityUtil.signature("Everything in the dream!".getBytes(StandardCharsets.UTF_8), dayDreamPrivateKey);

        Transaction transaction=new Transaction(new UTXO[]{}, outUtxos, sign, dayDreamPublicKey, System.currentTimeMillis());
        Transaction[] transactions={transaction};
        String preBlockHash=SecurityUtil.sha256Digest(blockChain.getNewestBlock().toString());
        String merkleRootHash=SecurityUtil.sha256Digest(transaction.toString());
        BlockHeader blockHeader=new BlockHeader(preBlockHash, merkleRootHash, Math.abs(new Random().nextLong()));
        BlockBody blockBody=new BlockBody(merkleRootHash, transactions);
        Block block=new Block(blockHeader, blockBody);

        blockChain.addNewBlock(block);
        minerPeer.boardcast(block);
    }

    /**
     * 启动挖矿线程和生成随机交易的线程
     */
    public void start() {
        spvPeer.start();
        minerPeer.start();
    }

    public List<Transaction> getTransactionInLastBlock(String walletAddress){
        List<Transaction> list=new ArrayList<>();
        Block block=blockChain.getNewestBlock();
        for(Transaction transaction:block.getBlockBody().getTransactions()){
            boolean have =false;
            for(UTXO utxo:transaction.getInUtxos()){
                if(utxo.getWalletAddress().equals(walletAddress)){
                    list.add(transaction);
                    have=true;
                    break;
                }
            }
            if(have){
                continue;
            }
            for(UTXO utxo:transaction.getOutUtxos()){
                if(utxo.getWalletAddress().equals(walletAddress)){
                    list.add(transaction);
                    break;
                }
            }
        }
        return list;
    }

    public Account create_account(){
        Account account=new Account();
        accounts.add(account);
        UTXO[] outUtxos=new UTXO[1];
        outUtxos[0]=new UTXO(account.getWalletAddress(), MiniChainConfig.INIT_AMOUNT, account.getPublicKey());
        KeyPair dayDreamKeyPair = SecurityUtil.secp256k1Generate();
        PublicKey dayDreamPublicKey= dayDreamKeyPair.getPublic();
        PrivateKey dayDreamPrivateKey = dayDreamKeyPair.getPrivate();
        byte[] sign =SecurityUtil.signature("Everything in the dream!".getBytes(StandardCharsets.UTF_8), dayDreamPrivateKey);

        Transaction transaction=new Transaction(new UTXO[]{}, outUtxos, sign, dayDreamPublicKey, System.currentTimeMillis());
        Transaction[] transactions={transaction};
        String preBlockHash=SecurityUtil.sha256Digest(blockChain.getNewestBlock().toString());
        String merkleRootHash=SecurityUtil.sha256Digest(transaction.toString());
        BlockHeader blockHeader=new BlockHeader(preBlockHash, merkleRootHash, Math.abs(new Random().nextLong()));
        BlockBody blockBody=new BlockBody(merkleRootHash, transactions);
        Block block=new Block(blockHeader, blockBody);
        blockChain.addNewBlock(block);
        minerPeer.boardcast(block);
        return account;
    }


}
