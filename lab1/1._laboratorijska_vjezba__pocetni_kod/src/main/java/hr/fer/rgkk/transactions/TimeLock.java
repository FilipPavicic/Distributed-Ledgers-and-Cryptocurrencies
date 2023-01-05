package hr.fer.rgkk.transactions;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.time.Instant;

import static org.bitcoinj.script.ScriptOpCodes.*;

public class TimeLock extends ScriptTransaction {

    private final ECKey aliceSecretKey = new ECKey();
    private final ECKey bobSecretKey = new ECKey();
    private final ECKey eveSecretKey = new ECKey();

    public enum ScriptSigType {
        ALICE_AND_EVE, BOB_AND_EVE, ALICE_AND_BOB
    }

    ScriptSigType scriptSigType;

    public TimeLock(WalletKit walletKit, NetworkParameters parameters, ScriptSigType scriptSigType) {
        super(walletKit, parameters);
        this.scriptSigType = scriptSigType;
    }

    @Override
    public Script createLockingScript() {
    	long lockedUntil = Instant.parse("2014-10-01T00:00:00Z").getEpochSecond();
    	byte[] hash160PubKeyEve = Utils.sha256hash160(eveSecretKey.getPubKey());
    	return new ScriptBuilder()            
    											// Stack = script that runs only if the number isn't zero
    											// Stack = | 1, evePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
    		.op(OP_IF) 							// Stack = | evePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
    		.number(lockedUntil)				// Stack = | lockedUntil,evePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
    		.op(OP_CHECKLOCKTIMEVERIFY)			// Stack = | lockedUntil, EvePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
    		.op(OP_DROP)						// Stack = | EvePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
    		.op(OP_DUP)   						// Stack = | EvePubKey, evePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
    		.op(OP_HASH160)   					// Stack = | hash160(pubKeyOnStack), evePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
    		.data(hash160PubKeyEve)        		// Stack = | hash160PubKeyEve, hash160(pubKeyOnStack), evePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
            .op(OP_EQUALVERIFY)  				// Stack = | evePubKey, signedBy_Eve, signedBy_Alice/Bob, 0 | [DNO]
            .op(OP_CHECKSIGVERIFY)  			// Stack = | signedBy_Alice/Bob, 0 | [DNO]
            .smallNum(1)						// Stack = | 1, signedBy_Alice/Bob, 0 | [DNO]
    		.data(aliceSecretKey.getPubKey())	// Stack = | AlicePubKey, 1, signedBy_Alice/Bob, 0 | [DNO]
    		.data(bobSecretKey.getPubKey())  	// Stack = | BobPubKey, AlicePubKey, 1, signedBy_Alice/Bob, 0 | [DNO]
    		.smallNum(2)					 	// Stack = | 2, BobPubKey, AlicePubKey, 1, signedBy_Alice/Bob, 0 | [DNO]
    		.op(OP_CHECKMULTISIG)				// Stack = | 1 [DNO]
    											// Stack = | 0, signedBy_Bob, signedBy_Alice, 0 | [DNO]
    		.op(OP_ELSE)				  		// Stack = | signedBy_Bob, signedBy_Alice, 0 | [DNO]
    		.smallNum(2)				  		// Stack = | 2, signedBy_Bob, signedBy_Alice, 0 | [DNO]
    		.data(aliceSecretKey.getPubKey())	// Stack = | AlicePubKey, 2, signedBy_Bob, signedBy_Alice, 0 | [DNO]
    		.data(bobSecretKey.getPubKey())  	// Stack = | BobPubKey, AlicePubKey, 2, signedBy_Bob, signedBy_Alice, 0 | [DNO]
    		.smallNum(2)					  	// Stack = | 2, BobPubKey, AlicePubKey, 2, signedBy_Bob, signedBy_Alice, 0 | [DNO]
    		.op(OP_CHECKMULTISIG)				// Stack = | 1 | [DNO]
    		.op(OP_ENDIF)						// Stack = | [DNO]
    		.build();

    }

    @Override
    public Script createUnlockingScript(Transaction unsignedScript) {
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        switch (this.scriptSigType) {
            case ALICE_AND_BOB:
                scriptBuilder
                        .smallNum(0)
                        .data(sign(unsignedScript, aliceSecretKey).encodeToBitcoin())
                        .data(sign(unsignedScript, bobSecretKey).encodeToBitcoin())
                        .smallNum(0);
                break;
            case ALICE_AND_EVE:
                scriptBuilder
                        .smallNum(0)
                        .data(sign(unsignedScript, aliceSecretKey).encodeToBitcoin())
                        .data(sign(unsignedScript, eveSecretKey).encodeToBitcoin())
                        .data(eveSecretKey.getPubKey())
                        .smallNum(1);
                break;
            case BOB_AND_EVE:
                scriptBuilder
                        .smallNum(0)
                        .data(sign(unsignedScript, bobSecretKey).encodeToBitcoin())
                        .data(sign(unsignedScript, eveSecretKey).encodeToBitcoin())
                        .data(eveSecretKey.getPubKey())
                        .smallNum(1);
        }
        return scriptBuilder.build();
    }
}
