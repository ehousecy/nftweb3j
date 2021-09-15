import cn.com.tass.jce.castle.core.jcajce.provider.asymmetric.ec.TAECPrivateKey;
import cn.com.tass.jce.castle.core.jce.provider.TassProvider;
import cn.com.tass.jce.castle.core.jce.spec.ECNamedCurveGenParameterSpec;
import conflux.web3j.Account;
import conflux.web3j.AccountManager;
import conflux.web3j.Cfx;
import conflux.web3j.types.Address;
import conflux.web3j.types.RawTransaction;
import conflux.web3j.types.TransactionBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
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


    public void importSk() {
        int testNetId = 1;
        // Initialize a accountManager
        try {
            am = new AccountManager(testNetId);
            // import private key
            am.imports(confluxSk, "112");
            Address a = am.create("232");
            System.out.println(a.getAddress());
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

        txBuilder.withGasPrice(BigInteger.valueOf(1)); // 没有的话，sdk会用默认值
        txBuilder.withGasLimit(BigInteger.valueOf(240904)); // 没有的话，sdk会用默认值

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
                        new Uint256(332)),
                Collections.<TypeReference<?>>emptyList());
        String txData = FunctionEncoder.encode(function);
        System.out.println(txData);
        return txData;
    }


    public void genKeyPairAndAddress() {
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("EC", "TASS");
            ECNamedCurveGenParameterSpec parameterSpec = new ECNamedCurveGenParameterSpec("secp256k1");
            keyPairGenerator.initialize(parameterSpec);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            String sk = Hex.toHexString(((TAECPrivateKey)keyPair.getPrivate()).getD().toByteArray());
            System.out.println("私钥： "+sk);
            System.out.println(sk.length());

            connectNode();
            Account account = Account.create(cfx, "0x"+sk);



            System.out.printf("conflux address %s\n", account.getAddress());
            System.out.printf("hex address %s\n", account.getHexAddress());

        } catch (Exception e) {
        }
    }

    public void sendTx() {
        String sk = "0x326a6467e643328f14fe83c1663ca3ffb57cac54b120c3e31164263b4a585ffd";
        int testNetId = 1;
        // Initialize a accountManager
        try {
            am = new AccountManager(testNetId);
            // import private key
            am.imports(sk, "222111");
        } catch (Exception e) {
            e.printStackTrace();
        }
        connectNode();

        BigInteger value = new BigInteger("8", 16);
        Address fromAdd = new Address("cfxtest:aajsgh1ya76x9gpvteybxumxfsbs4c95pph5e4jjab");
        TransactionBuilder txBuilder = new TransactionBuilder(fromAdd);
        txBuilder.withChainId(1);
        txBuilder.withTo(address);
        txBuilder.withValue(value);

        txBuilder.withGasPrice(BigInteger.valueOf(1)); // 没有的话，sdk会用默认值
        txBuilder.withGasLimit(BigInteger.valueOf(240904)); // 没有的话，sdk会用默认值


        connectNode();
        RawTransaction rawTx = txBuilder.build(cfx);

        // get account from accountManager, `account.send` will sign the tx and send it to blockchain
        try {
            SDKManager sdkManager = new SDKManager();
            // alias填写具体账户的alias
            String hexEncodedTx = sdkManager.sign(rawTx, "ECCHZEiuBp");
            // String hexEncodedTx = am.signTransaction(rawTx, fromAdd, "222111");
            String txHash = cfx.sendRawTransaction(hexEncodedTx).sendAndGet();
            System.out.printf("txHash is: %s", txHash);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public BigInteger getChainId() {
        connectNode();
        return cfx.getChainId();
    }

    public Address CreateWallet() {
        SDKManager sdkManager = new SDKManager();
        String alias;
        try {
            alias = sdkManager.generateECKeyPair();
            System.out.println(alias);
            String hexAddress = sdkManager.getAddress(alias);
            int chainId = getChainId().intValue();
            return new Address(hexAddress, chainId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    public static void main(String args[]) {
        confluxDemo cfd = new confluxDemo();
//        cfd.importSk();
//        cfd.buildAndSendTx();
//        cfd.genKeyPairAndAddress();
        cfd.sendTx();
        System.out.println(cfd.CreateWallet().getAddress());
    }


}
