package spv;

import consensus.MinerPeer;
import data.*;
import network.NetWork;
import utils.SecurityUtil;

import java.util.*;

public class SpvPeer extends Thread{
    private final List<BlockHeader> headers=new ArrayList<>();
    private Account account;
    private final NetWork netWork;

    public SpvPeer(Account account, NetWork netWork){
        this.account=account;
        this.netWork=netWork;
    }

    public void accept(BlockHeader blockHeader){

        headers.add(blockHeader);
//        verifyLatest();
    }
    public void run() {
        while (true) {
            synchronized (netWork.getTransactionPool()) {
                TransactionPool transactionPool= netWork.getTransactionPool();
                while (transactionPool.isFull()) {
                    try {
                        transactionPool.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
//                Transaction randomOne = getOneTransaction();
//                transactionPool.put(randomOne);
//                if (transactionPool.isFull()) {
//                    transactionPool.notify();
//                }
                Scanner scan =new Scanner(System.in);
                if(account==null){
                    System.out.println("You should create a account! Please enter:create a account");
                    String str=scan.nextLine();
                    if(str.equals("create a account")){
                        account=netWork.create_account();
                        System.out.println("create successfully! account is "+account.getWalletAddress());
                    }else{
                        System.out.println("You enter wrong.");
                    }
                }else{
                    System.out.println("Now you can choose the following features");
                    System.out.println("create your own transaction(enter:create a transaction).");
                    System.out.println("query your balance(enter:query balance).");
                    System.out.println("query a transaction(enter:query transaction).");
                    System.out.println("create random transactions(enter:random transaction).");

                    String str=scan.nextLine();
                    System.out.println(str +":");
                    if(str.equals("create a transaction")){
                        System.out.println("please enter the index of the account you want to transfer and the number of money.");
                        int toaccount_index=scan.nextInt();
                        int amount = scan.nextInt();
                        Account toaccount=netWork.getAccounts().get(toaccount_index);
                        Transaction transaction=getOneTransaction(toaccount, amount);
                        if(transaction==null){
                            continue;
                        }
                        System.out.println("create a transaction, the txHash is "+SecurityUtil.sha256Digest(transaction.toString()));
                        transactionPool.put(transaction);
                        if(transactionPool.isFull()){
                            transactionPool.notify();
                        }
                    } else if (str.equals("query balance")) {
                        int amount =getbalance();
                        System.out.println("The balance of your account is "+amount);
                    } else if (str.equals("query transaction")) {
                        System.out.println("please enter a txHash of transaction.");
                        String txHash=scan.nextLine();
                        if(simplifiedPaymentVerify(txHash)){
                            System.out.println("transaction exist.");
                        }else{
                            System.out.println("transaction doesn't exist.");
                        }
                    }else if (str.equals("random transaction")) {
                        while(!transactionPool.isFull()){
                            Transaction transaction=getRandomTransaction();
                            System.out.println("create random transaction, the txHash is "+SecurityUtil.sha256Digest(transaction.toString()));
                            transactionPool.put(transaction);
                            if(transactionPool.isFull()){
                                transactionPool.notify();
                                break;
                            }
                        }

                    }
                    System.out.println();
                }
            }
        }
    }
//    private void verifyLatest() {
//        List<Transaction> transactions=netWork.getTransactionInLastBlock(account.getWalletAddress());
//        if(transactions.isEmpty()){
//            return;
//        }
//        System.out.println("Account["+account.getWalletAddress()+"] began to verify the transaction...");
//        for(Transaction transaction:transactions){
//            if(!simplifiedPaymentVerify(transaction)){
//                System.out.println("verification failed!");
//                System.exit(-1);
//            }
//        }
//        System.out.println("Account["+account.getWalletAddress()+"] verifies all transactions are successful!\n");
//    }

    public boolean simplifiedPaymentVerify(String  txHash){
//        String txHash = SecurityUtil.sha256Digest(transaction.toString());

        MinerPeer minerPeer=netWork.getMinerPeer();
        Proof proof=minerPeer.getProof(txHash);

        if(proof==null){
            return false;
        }

        String hash = proof.getTxHash();
        for(Proof.Node node:proof.getPath()){
            switch (node.getOrientation()){
                case LEFT:hash=SecurityUtil.sha256Digest(node.getTxHash() + hash);break;
                case RIGHT:hash=SecurityUtil.sha256Digest(hash + node.getTxHash());break;
                default:return false;
            }
        }
        int height=proof.getHeight();
        String localMerkleRootHash=headers.get(height).getMerkleRootHash();

        String remoteMerkleRootHash=proof.getMerkleRootHash();

        System.out.println("\n-------> verify hash:\t"+txHash);
        System.out.println("calMerkleRootHash:\t\t"+hash);
        System.out.println("localMerkleRootHash:\t"+localMerkleRootHash);
        System.out.println("remoteMerkleRootHash:\t"+remoteMerkleRootHash);
        System.out.println();
        return hash.equals(localMerkleRootHash) && hash.equals(remoteMerkleRootHash);

    }

    private Transaction getOneTransaction(Account toaccount, int amount) {
        Transaction transaction=null;
        List<Account> accounts=netWork.getAccounts();
        Account aAccount=account;
        Account bAccount=toaccount;

        if(aAccount == bAccount){
            System.out.println("You can't transfer to yourself.");
            return transaction;
        }
        String aWalletAddress= aAccount.getWalletAddress();
        String bWalletAddress= bAccount.getWalletAddress();

        UTXO[] aTrueUtxos =netWork.getBlockChain().getTrueUtxos(aWalletAddress);
        int aAmount = getbalance();

        int txAmount =aAmount;

        if(aAmount<amount){
            System.out.println("your balance can't afford this transaction.your balance is "+aAmount);
            return transaction;
        }

        List<UTXO> inUtxoList =new ArrayList<>();
        List<UTXO> outUtxoList =new ArrayList<>();

        byte[] aUnlockSign = SecurityUtil.signature(aAccount.getPublicKey().getEncoded(), aAccount.getPrivateKey());

        int inAmount=0;
        for(UTXO utxo:aTrueUtxos){
            if(utxo.unlockScript(aUnlockSign, aAccount.getPublicKey())){
                inAmount+= utxo.getAmount();
                inUtxoList.add(utxo);
                if(inAmount>=txAmount){break;}
            }
        }
        if(inAmount<txAmount){
            System.out.println("the unlocked utxos is not enough.");
            return transaction;
        }

        outUtxoList.add(new UTXO(bWalletAddress, txAmount, bAccount.getPublicKey()));
        if(inAmount>txAmount){
            outUtxoList.add(new UTXO(aWalletAddress, inAmount-txAmount, aAccount.getPublicKey()));
        }

        UTXO[] inUtxos = inUtxoList.toArray(new UTXO[0]);
        UTXO[] outUtxos = outUtxoList.toArray(new UTXO[0]);

        byte[] data =SecurityUtil.utxos2Bytes(inUtxos, outUtxos);

        byte[] sign = SecurityUtil.signature(data, aAccount.getPrivateKey());

        long timestamp =System.currentTimeMillis();

        transaction =new Transaction(inUtxos, outUtxos, sign, aAccount.getPublicKey(), timestamp);

        return transaction;
    }
    private int getbalance(){
        String walletAddress= account.getWalletAddress();
        UTXO[] aTrueUtxos=netWork.getBlockChain().getTrueUtxos(walletAddress);
        int amount=account.getAmount(aTrueUtxos);
        return  amount;
    }
    private Transaction getRandomTransaction() {
        Random random = new Random();
        Transaction transaction=null;
        Account[] accounts = netWork.getAccounts().toArray(new Account[0]);

        while(true){
            Account aAccount=accounts[random.nextInt(accounts.length)];
            Account bAccount=accounts[random.nextInt(accounts.length)];

            if(aAccount == bAccount){continue;}
            String aWalletAddress= aAccount.getWalletAddress();
            String bWalletAddress= bAccount.getWalletAddress();

            UTXO[] aTrueUtxos =netWork.getBlockChain().getTrueUtxos(aWalletAddress);
            int aAmount = aAccount.getAmount(aTrueUtxos);
            if(aAmount==0){continue;}

            int txAmount =random.nextInt(aAmount)+1;

            List<UTXO> inUtxoList =new ArrayList<>();
            List<UTXO> outUtxoList =new ArrayList<>();

            byte[] aUnlockSign = SecurityUtil.signature(aAccount.getPublicKey().getEncoded(), aAccount.getPrivateKey());

            int inAmount=0;
            for(UTXO utxo:aTrueUtxos){
                if(utxo.unlockScript(aUnlockSign, aAccount.getPublicKey())){
                    inAmount+= utxo.getAmount();
                    inUtxoList.add(utxo);
                    if(inAmount>=txAmount){break;}
                }
            }
            if(inAmount<txAmount){continue;}

            outUtxoList.add(new UTXO(bWalletAddress, txAmount, bAccount.getPublicKey()));
            if(inAmount>txAmount){
                outUtxoList.add(new UTXO(aWalletAddress, inAmount-txAmount, aAccount.getPublicKey()));
            }

            UTXO[] inUtxos = inUtxoList.toArray(new UTXO[0]);
            UTXO[] outUtxos = outUtxoList.toArray(new UTXO[0]);

            byte[] data =SecurityUtil.utxos2Bytes(inUtxos, outUtxos);

            byte[] sign = SecurityUtil.signature(data, aAccount.getPrivateKey());

            long timestamp =System.currentTimeMillis();

            transaction =new Transaction(inUtxos, outUtxos, sign, aAccount.getPublicKey(), timestamp);

            break;
        }

        return transaction;
    }


}
