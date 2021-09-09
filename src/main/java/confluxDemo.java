import cn.com.tass.jce.castle.core.jce.provider.TassProvider;
import cn.com.tass.jce.castle.core.jce.spec.ECNamedCurveGenParameterSpec;
import conflux.web3j.AccountManager;
import conflux.web3j.Cfx;
import conflux.web3j.types.Address;
import conflux.web3j.types.RawTransaction;
import conflux.web3j.types.TransactionBuilder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;

public class confluxDemo {
    public String confluxSk = "0x3B57A00547A05511B0339FFBD9AA7A0AD2F6FD3D0B980372C22252F77D14D03D";
    Address address = new Address("cfxtest:aam66h08kb8acc4ea25661xpg1xbf2e8xy1a95pnju");
    Address contractAddress = new Address("cfxtest:achm7rp1p42rvxh908up7c6a29r6nrt5f67xp4jm1g");
    AccountManager am = null;

    Cfx cfx = null;

    static KeyStore keyStore = null;

    static {
        Security.addProvider(new TassProvider());
        try {
            keyStore = KeyStore.getInstance("TAKS", "TASS");
            keyStore.load(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importAddress() {

        System.out.println(address.getAddress()); // "cfxtest:aak7fsws4u4yf38fk870218p1h3gxut3ku00u1k1da"
        System.out.println(address.getHexAddress()); // "0x13d2bA4eD43542e7c54fbB6c5fCCb9f269C1f94C"
        System.out.println(address.getVerboseAddress()); // "NET1921:TYPE.USER:AAR8JZYBZV0FHZREAV49SYXNZUT8S0JT1AT8UHK7M3"
        System.out.println(address.getNetworkId()); // 1
        System.out.println(address.getType()); // user
    }

    public void genAccount() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "TASS");
            ECNamedCurveGenParameterSpec parameterSpec = new ECNamedCurveGenParameterSpec("secp256k1");
            keyPairGenerator.initialize(parameterSpec);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            String hash = Hash.sha3(String.valueOf(keyPair.getPublic()));
            System.out.println(hash);
            String addressHex = "0x" + hash.substring(hash.length() - 40);
            System.out.println(addressHex); // yanggb
            addressHex = Keys.toChecksumAddress(addressHex);
            System.out.println(addressHex);
            Address address2 = new Address(addressHex, 1);
            System.out.println(address2.getAddress());
            System.out.println(address2.getHexAddress());

        } catch (Exception e) {

        }
    }

    public void importSk() {
        int testNetId = 1;
        // Initialize a accountManager
        try {
            am = new AccountManager(testNetId);
            // import private key
            am.imports(confluxSk, "112");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectNode() {
        cfx = Cfx.create("https://test.confluxrpc.org/v2", 3, 1000);
        BigInteger epoch = cfx.getEpochNumber().sendAndGet();
        System.out.println("Current epoch: " + epoch);
    }

    public void buildAndSendTx() {
        BigInteger value = new BigInteger("0", 16);
        TransactionBuilder txBuilder = new TransactionBuilder(address);
        txBuilder.withChainId(1);
        txBuilder.withTo(contractAddress);
        txBuilder.withValue(value);

        txBuilder.withGasPrice(BigInteger.valueOf(1)); // 没有似乎也没关系
        txBuilder.withGasLimit(BigInteger.valueOf(240904)); // 没有似乎也没关系

        String data = makeFunctionCallData();
        txBuilder.withData(data);

        connectNode();
        RawTransaction rawTx = txBuilder.build(cfx);

        // get account from accountManager, `account.send` will sign the tx and send it to blockchain
        try {
            String hexEncodedTx = am.signTransaction(rawTx, address, "123456"); // 不清楚为啥这里是123456
            String txHash = cfx.sendRawTransaction(hexEncodedTx).sendAndGet();
            System.out.printf("txHash is: %s", txHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // or you can manually sign and send
        // String hexEncodedTx = account.sign(rawTx);
        // String txHash = cfx.sendRawTransaction(hexEncodedTx).sendAndGet();

    }

    public String makeFunctionCallData() {
        //        [["zzz","111","ZHH","lately","asdasd","morden","122","12","33","11","12","11"], true ]
//        [["https://sad", "adasd"],["https://sad", "adasd"]]
//        12

        //[["zzz","111","ZHH","lately","asdasd","morden",
        // "122","12","33","11","12","11"],true]
        DynamicStruct basicInfoArg = new DynamicStruct(
                new Utf8String("zzz"),
                new Utf8String("111"),
                new Utf8String("ZHH"),
                new Utf8String("lately"),
                new Utf8String("asdasd"),
                new Utf8String("morden"),

                new Utf8String("122"),
                new Utf8String("12"),
                new Utf8String("33"),
                new Utf8String("11"),
                new Utf8String("12"),
                new Utf8String("11")
        );
        DynamicStruct firstArg = new DynamicStruct(basicInfoArg, new Bool(true));

        //[["https://sad","adasd"],["https://sad","adasd"]]
        DynamicStruct image1 = new DynamicStruct(
                new Utf8String("https://sad"),
                new Utf8String("adasd")
        );
        DynamicStruct image2 = new DynamicStruct(
                new Utf8String("https://sad"),
                new Utf8String("adasd")
        );
        DynamicArray secondArg =  new DynamicArray<DynamicStruct>(DynamicStruct.class, Arrays.asList(image1, image2));


        Function function = new Function(
                "mintArtWorksToken",  // 合约方法名
                Arrays.<Type>asList(firstArg,
                        secondArg,
                        new Uint256(22)),
                Collections.<TypeReference<?>>emptyList());
        String txData = FunctionEncoder.encode(function);
        System.out.println(txData);
        return txData;
    }

    public static void main(String args[]) {
        confluxDemo cfd = new confluxDemo();
//        cfd.importSk();
//        cfd.buildAndSendTx();
        cfd.genAccount();
    }
}
