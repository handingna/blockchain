package data;

import utils.SecurityUtil;

import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * 对交易的抽象
 */
public class Transaction {

    private final UTXO[] inUtxos;
    private final UTXO[] outUtxos;

    private final byte[] sendSign;
    private final PublicKey sendPublicKey;
    private final long timestamp;
    public Transaction(UTXO[] inUtxos, UTXO[] outUtxos, byte[] sendSign, PublicKey sendPublicKey, long timestamp) {
        this.inUtxos = inUtxos;
        this.outUtxos = outUtxos;
        this.sendSign = sendSign;
        this.sendPublicKey = sendPublicKey;
        this.timestamp = timestamp;
    }
    public  UTXO[] getInUtxos(){return  inUtxos;}
    public  UTXO[] getOutUtxos(){return  outUtxos;}

    public byte[] getSendSign() {
        return sendSign;
    }

    public PublicKey getSendPublicKey() {
        return sendPublicKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toString(){
        return "\nTransaction{" +
                "\ninUtxos="+ Arrays.toString(inUtxos)+
                ", \noutUtxos="+Arrays.toString(outUtxos) +
                ", \nsendSign="+ SecurityUtil.bytes2HexString(sendSign)+
                ", \nsendPublicKey="+SecurityUtil.bytes2HexString(sendPublicKey.getEncoded())+
                ", \ntimestamp="+timestamp +
                "}";
    }



}
