package hr.fer.rgkk.transactions;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.security.SecureRandom;

import static org.bitcoinj.script.ScriptOpCodes.*;

public class CoinToss extends ScriptTransaction {

    // Alice's private key
    private final ECKey aliceKey;
    // Alice's nonce
    private final byte[] aliceNonce;
    // Bob's private key
    private final ECKey bobKey;
    // Bob's nonce
    private final byte[] bobNonce;
    // Key used in unlocking script to select winning player.
    private final ECKey winningPlayerKey;

    private CoinToss(
            WalletKit walletKit, NetworkParameters parameters,
            ECKey aliceKey, byte[] aliceNonce,
            ECKey bobKey, byte[] bobNonce,
            ECKey winningPlayerKey
    ) {
        super(walletKit, parameters);
        this.aliceKey = aliceKey;
        this.aliceNonce = aliceNonce;
        this.bobKey = bobKey;
        this.bobNonce = bobNonce;
        this.winningPlayerKey = winningPlayerKey;
    }

    @Override
    public Script createLockingScript() {
        byte[] commitmentAlice = Utils.sha256hash160(aliceNonce);
        byte[] commitmentBob = Utils.sha256hash160(bobNonce);
        return new ScriptBuilder()            	// Stack = | R_A, R_B, winnerSignature| [DNO] (after executing unlocking script)
        		.op(OP_2DUP)   					// Stack = | R_A, R_B, R_A, R_B, winnerSignature| [DNO]
        		.op(OP_HASH160)   				// Stack = | hash160(R_A), R_B, R_A, R_B, winnerSignature| [DNO]
        		.data(commitmentAlice)        	// Stack = | comitmentAlice, hash160(R_A), R_B, R_A, R_B, winnerSignature| [DNO]
                .op(OP_EQUALVERIFY)  			// Stack = | R_B, R_A, R_B, winnerSignature| [DNO]
                .op(OP_HASH160)   				// Stack = | hash160(R_B), R_A, R_B, winnerSignature| [DNO]
        		.data(commitmentBob)        	// Stack = | comitmentBob, hash160(R_B), R_A, R_B, winnerSignature| [DNO]
                .op(OP_EQUALVERIFY)  			// Stack = | R_A, R_B, winnerSignature| [DNO]	
                .op(OP_SIZE)					// Stack = | N_A, R_A, R_B, winnerSignature| [DNO]
                .number(17)						// Stack = | 17, N_A, R_A, R_B, winnerSignature| [DNO]
                .op(OP_EQUAL)					// Stack = | 0/1, R_A, R_B, winnerSignature| [DNO]
                .op(OP_SWAP)					// Stack = | R_A, 0/1, R_B, winnerSignature| [DNO]
                .op(OP_DROP)					// Stack = | 0/1, R_B, winnerSignature| [DNO]
                .op(OP_SWAP)					// Stack = | R_B, 0/1, winnerSignature| [DNO]
                .op(OP_SIZE)					// Stack = | N_B, R_B, 0/1, winnerSignature| [DNO]
                .number(17)						// Stack = | 17, N_B, R_B, 0/1, winnerSignature| [DNO]
                .op(OP_EQUAL)					// Stack = | 0/1, R_B, 0/1, winnerSignature| [DNO]
                .op(OP_SWAP)					// Stack = | R_B, 0/1, 0/1, winnerSignature| [DNO]
                .op(OP_DROP)					// Stack = | 0/1, 0/1, winnerSignature| [DNO]
                .op(OP_ADD)						// Stack = | 0/1, winnerSignature| [DNO]
                .op(OP_IF)						// Stack = | winnerSignature| [DNO]
                .data(bobKey.getPubKey())		// Stack = | bobPubKey, winnerSignature| [DNO]
                .op(OP_CHECKSIG)				// Stack = | 1| [DNO]
                .op(OP_ELSE)					// Stack = | winnerSignature| [DNO]
                .data(aliceKey.getPubKey())		// Stack = | alicePubKey, winnerSignature| [DNO]
                .op(OP_CHECKSIG)				// Stack = | 1| [DNO]
                .op(OP_ENDIF)					// Stack = | 1| [DNO]
                .build();
    }

    @Override
    public Script createUnlockingScript(Transaction unsignedTransaction) {
        TransactionSignature signature = sign(unsignedTransaction, winningPlayerKey);
        return new ScriptBuilder()
                .data(signature.encodeToBitcoin())
                .data(bobNonce)
                .data(aliceNonce)
                .build();
    }

    public static CoinToss of(
            WalletKit walletKit, NetworkParameters parameters,
            CoinTossChoice aliceChoice, CoinTossChoice bobChoice,
            WinningPlayer winningPlayer
    ) {
        byte[] aliceNonce = randomBytes(16 + aliceChoice.value);
        byte[] bobNonce = randomBytes(16 + bobChoice.value);

        ECKey aliceKey = randKey();
        ECKey bobKey = randKey();

        // Alice is TAIL, bob is HEAD
        ECKey winningPlayerKey = WinningPlayer.TAIL == winningPlayer ? aliceKey : bobKey;

        return new CoinToss(
                walletKit, parameters,
                aliceKey, aliceNonce,
                bobKey, bobNonce,
                winningPlayerKey
        );
    }

    private static byte[] randomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public enum WinningPlayer {
        TAIL, HEAD
    }

    public enum CoinTossChoice {

        ZERO(0),
        ONE(1);

        public final int value;

        CoinTossChoice(int value) {
            this.value = value;
        }
    }
}

