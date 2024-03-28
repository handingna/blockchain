package data;

import java.util.*;

/**
 * 交易池
 */
public class TransactionPool {

    private final List<Transaction> transactions;
    private final int capacity;

    private final Set<UTXO> utxoSet;

    public TransactionPool(int capacity) {
        this.transactions = new ArrayList<>();
        this.capacity = capacity;
        utxoSet=new HashSet<>();
    }

    public void put(Transaction transaction) {
        for(UTXO utxo:transaction.getInUtxos()){
            if(utxoSet.contains(utxo)){
                return;
            }
        }
        utxoSet.addAll(Arrays.asList(transaction.getInUtxos()));
        transactions.add(transaction);
    }

    public Transaction[] getAll() {
        Transaction[] ret = new Transaction[capacity];
        transactions.toArray(ret);
        transactions.clear();
        return ret;
    }

    public boolean isFull() {
        return transactions.size() >= capacity;
    }

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    public int getCapacity() {
        return capacity;
    }
}


