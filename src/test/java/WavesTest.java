import com.gamaxa.Data;
import com.wavesplatform.wavesj.Account;
import com.wavesplatform.wavesj.Node;
import com.wavesplatform.wavesj.PrivateKeyAccount;
import com.wavesplatform.wavesj.Transaction;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class WavesTest {

    private PrivateKeyAccount alice;
    private PrivateKeyAccount bob;
    private Node node;

    @Before
    public void setup() {
        String seed = "health lazy lens fix dwarf salad breeze myself silly december endless rent faculty report beyond";
        alice = PrivateKeyAccount.fromSeed(seed, 0, Account.TESTNET);
        bob = PrivateKeyAccount.fromSeed(PrivateKeyAccount.generateSeed(), 0, Account.TESTNET);
        node = new Node();
    }

    @Test
    public void canCreateAccount() throws IOException {
        assert alice != null;
        assert bob != null;
        assert node != null;
        assert node.getBalance(bob.getAddress(), Data.getAssetId(true)) == 0;

    }
}
