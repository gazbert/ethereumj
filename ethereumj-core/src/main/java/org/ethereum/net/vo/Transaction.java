package org.ethereum.net.vo;

import org.ethereum.crypto.ECKey.ECDSASignature;
import org.ethereum.crypto.HashUtil;
import org.ethereum.net.rlp.RLPItem;
import org.ethereum.net.rlp.RLPList;
import org.ethereum.util.Utils;

/**
 * www.ethereumJ.com
 * User: Roman Mandeleil
 * Created on: 21/04/14 09:19
 */
public class Transaction {

    private RLPList rawData;
    private boolean parsed = false;

// creation contract tx or simple send tx
// [ nonce, value, receiveAddress, gasPrice, gasDeposit, data, signatureV, signatureR, signatureS ]
// or
// [ nonce, endowment, 0, gasPrice, gasDeposit (for init), body, init, signatureV, signatureR, signatureS ]

    private byte[] hash;
    private byte[] nonce;
    private byte[] value;

    // In creation transaction the receive address is - 0
    private byte[] receiveAddress;
    private byte[] gasPrice;
    private byte[] gas;

    // Contract creation [data] will hold the contract
    // for other transaction [data] can hold data
    private byte[] data;
    private byte[] init;

    // Signature
    private ECDSASignature signature;

    public Transaction(RLPList rawData) {
        this.rawData = rawData;
        parsed = false;
    }

    public Transaction(byte[] nonce, byte[] value, byte[] recieveAddress, byte[] gasPrice, byte[] gas, byte[] data, byte v, byte[] r, byte[] s) {
        this.nonce = nonce;
        this.value = value;
        this.receiveAddress = recieveAddress;
        this.gasPrice = gasPrice;
        this.gas = gas;
        this.data = data;
        this.signature = ECDSASignature.fromComponents(r, s, v);
        parsed = true;
    }

    public void rlpParse(){

        this.hash = HashUtil.sha3(rawData.getRLPData());
        this.nonce =          ((RLPItem) rawData.getElement(0)).getData();
        this.value =          ((RLPItem) rawData.getElement(1)).getData();
        this.receiveAddress = ((RLPItem) rawData.getElement(2)).getData();
        this.gasPrice =       ((RLPItem) rawData.getElement(3)).getData();
        this.gas =            ((RLPItem) rawData.getElement(4)).getData();
        this.data =           ((RLPItem) rawData.getElement(5)).getData();

        if (rawData.size() == 9){  // Simple transaction
        	byte v =     ((RLPItem) rawData.getElement(6)).getData()[0];
            byte[] r =     ((RLPItem) rawData.getElement(7)).getData();
            byte[] s =     ((RLPItem) rawData.getElement(8)).getData();
            this.signature = ECDSASignature.fromComponents(r, s, v);
        } else if (rawData.size() == 10){ // Contract creation transaction
            this.init =           ((RLPItem) rawData.getElement(6)).getData();
            byte v =     ((RLPItem) rawData.getElement(7)).getData()[0];
            byte[] r =     ((RLPItem) rawData.getElement(8)).getData();
            byte[] s =     ((RLPItem) rawData.getElement(9)).getData();
            this.signature = ECDSASignature.fromComponents(r, s, v);
        } else throw new Error("Wrong tx data element list size");

        this.parsed = true;
    }

    public RLPList getRawData() {
        return rawData;
    }

    public boolean isParsed() {
        return parsed;
    }

    public byte[] getHash() {
        if (!parsed) rlpParse();
        return hash;
    }

    public byte[] getNonce() {
        if (!parsed) rlpParse();
        return nonce;
    }

    public byte[] getValue() {
        if (!parsed) rlpParse();
        return value;
    }

    public byte[] getReceiveAddress() {
        if (!parsed) rlpParse();
        return receiveAddress;
    }

    public byte[] getGasPrice() {
        if (!parsed) rlpParse();
        return gasPrice;
    }

    public byte[] getGas() {
        if (!parsed) rlpParse();
        return gas;
    }

    public byte[] getData() {
        if (!parsed) rlpParse();
        return data;
    }

    public byte[] getInit() {
        if (!parsed) rlpParse();
        return init;
    }

    public ECDSASignature getSignature() {
        if (!parsed) rlpParse();
        return signature;
    }

    @Override
    public String toString() {
        if (!parsed) rlpParse();
        return "TransactionData [" +  " hash=" + Utils.toHexString(hash) +
                "  nonce=" + Utils.toHexString(nonce) +
                ", value=" + Utils.toHexString(value) +
                ", receiveAddress=" + Utils.toHexString(receiveAddress) +
                ", gasPrice=" + Utils.toHexString(gasPrice) +
                ", gas=" + Utils.toHexString(gas) +
                ", data=" + Utils.toHexString(data) +
                ", init=" + Utils.toHexString(init) +
                ", signatureV=" + signature.v +
                ", signatureR=" + Utils.toHexString(signature.r.toByteArray()) +
                ", signatureS=" + Utils.toHexString(signature.s.toByteArray()) +
                ']';
    }
}