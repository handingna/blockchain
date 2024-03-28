//package unit;
//
//import config.MiniChainConfig;
//import consensus.MinerNode;
//import consensus.TransactionProducer;
//import data.*;
//import org.junit.Assert;
//import org.junit.Before;
//import utils.SecurityUtil;
//import static org.junit.Assert.assertTrue;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//public class UtxoTest {
//    private final BlockChain blockChain = new BlockChain();
//    @org.junit.Test
//    public void Test() throws InterruptedException {
//        Transaction transaction=getAt();
//
//        TransactionPool transactionPool = new TransactionPool(1);
//        transactionPool.put(transaction);
//
//        Thread minerThread = new Thread(() -> {
//            MinerNode minerNode = new MinerNode(transactionPool, blockChain);
//            minerNode.run();
//        });
//        minerThread.start();
//
//        Thread.sleep(5000);
//
//        minerThread.interrupt();
//
//    }
//    private Transaction getAt(){
//        Account[] accounts = blockChain.getAccounts();
//
//        Account accountA = accounts[1];
//        Account accountB = accounts[2];
//
//        String aWalletAddress = accountA.getWalletAddress();
//        String bWalletAddress = accountB.getWalletAddress();
//
//        UTXO[] aTrueUtxos = blockChain.getTrueUtxos(aWalletAddress);
//
//        int totalAmount = 0;
//        List<UTXO> inUtxoList = new ArrayList<>();
//        for (UTXO utxo : aTrueUtxos) {
//            if (totalAmount >= 1000) {
//                break;
//            }
//            totalAmount += utxo.getAmount();
//            inUtxoList.add(utxo);
//        }
//
//        assertTrue(totalAmount >= 1000);
//
//        List<UTXO> outUtxoList = new ArrayList<>();
//        outUtxoList.add(new UTXO(bWalletAddress, 1000, accountB.getPublicKey()));
//
//        if (totalAmount > 1000) {
//            outUtxoList.add(new UTXO(aWalletAddress, totalAmount - 1000, accountA.getPublicKey()));
//        }
//
//        UTXO[] inUtxos = inUtxoList.toArray(new UTXO[0]);
//        UTXO[] outUtxos = outUtxoList.toArray(new UTXO[0]);
//
//        byte[] data = SecurityUtil.utxos2Bytes(inUtxos, outUtxos);
//
//        byte[] sign = SecurityUtil.signature(data, accountA.getPrivateKey());
//
//        long timestamp = System.currentTimeMillis();
//        Transaction transaction = new Transaction(inUtxos, outUtxos, sign, accountA.getPublicKey(), timestamp);
//
//        return transaction;
//
//
//    }
//
//
//
//}
