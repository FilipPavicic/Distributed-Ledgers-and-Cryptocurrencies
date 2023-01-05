package hr.fer.rgkk.transactions;

import org.bitcoinj.core.*;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import static org.bitcoinj.script.ScriptOpCodes.*;

public class PayToPubKeyHash extends ScriptTransaction {
	
	private DeterministicKey key;

    public PayToPubKeyHash(WalletKit walletKit, NetworkParameters parameters) {
        super(walletKit, parameters);
        key = getWallet().freshReceiveKey();
    }

    @Override
    public Script createLockingScript() {
    	byte[] hash160PubKey = Utils.sha256hash160(key.getPubKey());
        return new ScriptBuilder()            	// Stack = | pubKeyOnStack, signature| [DNO] (after executing unlocking script)
        		.op(OP_DUP)   					// Stack = | pubKeyOnStack, pubKeyOnStack, signature| [DNO]
        		.op(OP_HASH160)   				// Stack = | hash160(pubKeyOnStack), pubKeyOnStack, signature| [DNO]
        		.data(hash160PubKey)        	// Stack = | hash160PubKey,hash160(pubKeyOnStack), pubKey, signature| [DNO]
                .op(OP_EQUALVERIFY)  			// Stack = | pubKey, signature| [DNO]
                .op(OP_CHECKSIG)  				// Stack = | TRUE| [DNO]
                .build();
    }

    @Override
    public Script createUnlockingScript(Transaction unsignedTransaction) {
        byte[] signature = sign(unsignedTransaction, key).encodeToBitcoin();
        return new ScriptBuilder()				// Stack = | [DNO]
                .data(signature)       			// Stack = | signature | [DNO]
                .data(key.getPubKey())			// Stack = | pubKey, signature| [DNO]
                .build();
    }
}
