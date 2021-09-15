import cn.com.tass.jce.castle.core.cert.X509CertificateHolder;
import cn.com.tass.jce.castle.core.cert.X509v3CertificateBuilder;
import cn.com.tass.jce.castle.core.jcajce.provider.asymmetric.ec.TAECPrivateKey;
import cn.com.tass.jce.castle.core.jcajce.provider.asymmetric.ec.TAECPublicKey;
import cn.com.tass.jce.castle.core.jce.provider.TassProvider;
import cn.com.tass.jce.castle.core.jce.spec.ECNamedCurveGenParameterSpec;
import cn.com.tass.jce.castle.core.operator.jcajce.JcaContentSignerBuilder;
import cn.com.tass.jce.castle.tc.asn1.x500.X500Name;
import cn.com.tass.jce.castle.tc.asn1.x509.SubjectPublicKeyInfo;
import conflux.web3j.types.AddressType;
import conflux.web3j.types.RawTransaction;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.StandardDSAEncoding;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.*;

import static org.web3j.crypto.Sign.recoverFromSignature;

public class SDKManager {

    static KeyStore keyStore;
    static {
        Security.addProvider(new TassProvider());
        Security.addProvider(new BouncyCastleProvider());
        try {
            keyStore = KeyStore.getInstance("TAKS", "TASS");
            keyStore.load(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SDKManager(){
    }
    private static class SingletonContainer{
        private static SDKManager instance = new SDKManager();
    }
    public static SDKManager getInstance(){
        return SingletonContainer.instance;
    }

    /**
     * 列出密钥列表
     */
    public void listKeys() {
        /* get an enumeration of all the key names (aliases), print each one */
        try {
            for (Enumeration enumKeys = keyStore.aliases(); enumKeys.hasMoreElements();)
            {
                System.out.println(enumKeys.nextElement().toString());
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }
    public String getAddress(String alias) throws KeyStoreException, IOException {
        Certificate certificate = keyStore.getCertificate(alias);
        ECPublicKeyParameters ecPubKeyParameters = (ECPublicKeyParameters) PublicKeyFactory.createKey(certificate.getPublicKey().getEncoded());
        byte[] publicKeyBytes = ecPubKeyParameters.getQ().getEncoded(false);
        BigInteger publicKeyValue = new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));
        String address = Numeric.prependHexPrefix(Keys.getAddress(publicKeyValue));
        return AddressType.User.normalize(address);
    }

    public String generateECKeyPair() throws Exception {
        // 生成基于SECP256K1曲线的秘钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "TASS");
        ECNamedCurveGenParameterSpec parameterSpec = new ECNamedCurveGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(parameterSpec);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String alias = getNonce();
        // 存放密钥
        Certificate certificate =  getECCCert(keyPair.getPublic(),keyPair.getPrivate());
        keyStore.setCertificateEntry(alias, certificate);
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), null, new Certificate[]{certificate});
        keyStore.store(null, null);
        System.out.println("success");
        keyStore.load(null,null);
        return alias;
    }

    public String sign(RawTransaction rawTx, String alias) throws Exception {
        TAECPrivateKey privateKey = (TAECPrivateKey) keyStore.getKey(alias,null);
        TAECPublicKey publicKey = (TAECPublicKey) keyStore.getCertificate(alias).getPublicKey();
        RlpType rlpTx = rawTx.toRlp();
        byte[] encoded = RlpEncoder.encode(rlpTx);
        Sign.SignatureData signature = signMessage(encoded, privateKey,publicKey,false);

        int v = signature.getV()[0] - 27;
        byte[] r = Bytes.trimLeadingZeroes(signature.getR());
        byte[] s = Bytes.trimLeadingZeroes(signature.getS());

        byte[] signedTx = RlpEncoder.encode(new RlpList(
                rlpTx,
                RlpString.create(v),
                RlpString.create(r),
                RlpString.create(s)));

        return Numeric.toHexString(signedTx);
    }

    private Sign.SignatureData signMessage(byte[] message, TAECPrivateKey privateKey,TAECPublicKey publicKey, boolean needToHash) throws Exception {
        byte[] publicKeyBytes = publicKey.getEncoded();
        BigInteger publicKeyValue = new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));
        byte[] messageHash;
        if (needToHash) {
            messageHash = Hash.sha3(message);
        } else {
            messageHash = message;
        }
        X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");

        Signature sign = Signature.getInstance("SHA256withECDSA", "TASS");
        sign.initSign(privateKey);
        sign.update(messageHash);
        byte[] resultSign = sign.sign();
        BigInteger[] result = StandardDSAEncoding.INSTANCE.decode(CURVE_PARAMS.getN(),resultSign);
        ECDSASignature sig = new ECDSASignature(result[0],result[1]).toCanonicalised();

        // Now we have to work backwards to figure out the recId needed to recover the signature.
        int recId = -1;
        for (int i = 0; i < 4; i++) {
            BigInteger k = recoverFromSignature(i, sig, Hash.sha3(messageHash));
            if (k != null && k.equals(publicKeyValue)) {
                recId = i;
                break;
            }
        }
        if (recId == -1) {
            throw new Exception(
                    "Could not construct a recoverable key. Are your credentials valid?");
        }

        int headerByte = recId + 27;

        // 1 header + 32 bytes for R + 32 bytes for S
        byte[] v = new byte[] {(byte) headerByte};
        byte[] r = Numeric.toBytesPadded(sig.r, 32);
        byte[] s = Numeric.toBytesPadded(sig.s, 32);

        return new Sign.SignatureData(v, r, s);
    }

    public String getNonce() {
        //定义一个字符串（A-Z，a-z，0-9）即62位；
        String str="zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
        //由Random生成随机数
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        //长度为几就循环几次
        for(int i=0; i<7; ++i){
            //产生0-61的数字
            int number=random.nextInt(62);
            //将产生的数字通过length次承载到sb中
            sb.append(str.charAt(number));
        }
        //将承载的字符转换成字符串
        sb.insert(0,"ECC");
        return sb.toString();

    }

    //TODO CN需要动态修改
    private Certificate getECCCert(PublicKey publicKey, PrivateKey privateKey) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 1);
        SubjectPublicKeyInfo bcPk = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(
                new X500Name("CN=TASS PUBLIC KEY CERT,C=CN"),
                BigInteger.ONE,
                new Date(),
                cal.getTime(),
                new X500Name("CN=TASS PUBLIC KEY CERT,C=CN"),
                bcPk
        );
        X509CertificateHolder certHolder = certGen
                .build(new JcaContentSignerBuilder("SHA256withECDSA").build(privateKey));
        CertificateFactory cFact = CertificateFactory.getInstance("X.509", "TASS");
        ByteArrayInputStream bIn = new ByteArrayInputStream(certHolder.getEncoded());
        return cFact.generateCertificate(bIn);
    }

}
