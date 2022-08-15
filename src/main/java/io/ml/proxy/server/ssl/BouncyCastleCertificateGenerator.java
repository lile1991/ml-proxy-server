package io.ml.proxy.server.ssl;

import io.ml.proxy.utils.io.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class BouncyCastleCertificateGenerator {

    public static KeyFactory keyFactory;

    public static PrivateKey privateKey;
    public static X509Certificate caCert;


    public static PublicKey serverPubKey;
    public static PrivateKey serverPriKey;
    static {
        Security.addProvider(new BouncyCastleProvider());

        try {
            keyFactory = KeyFactory.getInstance("RSA");

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            caCert = (X509Certificate) cf.generateCertificate(BouncyCastleCertificateGenerator.class.getResourceAsStream("/ssl/ca.crt"));
            EncodedKeySpec caPriKey = new PKCS8EncodedKeySpec(IOUtils.toByteArray(BouncyCastleCertificateGenerator.class.getResourceAsStream("/ssl/ca_private.der")));
            privateKey = BouncyCastleCertificateGenerator.keyFactory.generatePrivate(caPriKey);

            KeyPair keyPair = genKeyPair();
            serverPubKey = keyPair.getPublic();
            serverPriKey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException | CertificateException e) {
            // Unexpected anomalies
            log.error("Certificate init error", e);
        }
    }

    /**
     * 读取ssl证书使用者信息
     */
    public static String getSubject(X509Certificate certificate) {
        //读出来顺序是反的需要反转下
        List<String> tempList = Arrays.asList(certificate.getIssuerDN().toString().split(", "));
        return IntStream.rangeClosed(0, tempList.size() - 1)
                .mapToObj(i -> tempList.get(tempList.size() - i - 1)).collect(Collectors.joining(", "));
    }

    /**
     * 生成RSA公私密钥对,长度为2048
     */
    public static KeyPair genKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048, new SecureRandom());
        return keyPairGen.genKeyPair();
    }

    private static final Map<String, X509Certificate> hostServerCertCache = new WeakHashMap<>();

    public static X509Certificate generateServerCert(String host) throws CertIOException, CertificateException, OperatorCreationException {
        X509Certificate x509Certificate = hostServerCertCache.get(host);
        if(x509Certificate != null) {
            return x509Certificate;
        }

        /* String issuer = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=ProxyeeRoot";
        String subject = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + host;*/
        //根据CA证书subject来动态生成目标服务器证书的issuer和subject
        // 使用者信息
        String issuer = getSubject(caCert);
        String subject = Stream.of(issuer.split(", ")).map(item -> {
            String[] arr = item.split("=");
            if ("CN".equals(arr[0])) {
                return "CN=" + host;
            } else {
                return item;
            }
        }).collect(Collectors.joining(", "));

        Date caNotBefore = caCert.getNotBefore();
        Date caNotAfter = caCert.getNotAfter();

        //doc from https://www.cryptoworkshop.com/guide/
        JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(new X500Name(issuer),
                //issue#3 修复ElementaryOS上证书不安全问题(serialNumber为1时证书会提示不安全)，避免serialNumber冲突，采用时间戳+4位随机数生成
                BigInteger.valueOf(System.currentTimeMillis() + (long) (Math.random() * 10000) + 1000),
                caNotBefore,
                caNotAfter,
                new X500Name(subject),
                serverPubKey);
        //SAN扩展证书支持的域名，否则浏览器提示证书不安全
        GeneralName[] generalNames = new GeneralName[1];
        generalNames[0] = new GeneralName(GeneralName.dNSName, host);

        GeneralNames subjectAltName = new GeneralNames(generalNames);
        jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
        //SHA256 用SHA1浏览器可能会提示证书不安全
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(privateKey);
        x509Certificate = new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
        hostServerCertCache.put(host, x509Certificate);
        return x509Certificate;
    }

    public static X509Certificate generateCaCert(String subject, Date caNotBefore, Date caNotAfter,
                                          KeyPair keyPair) throws CertIOException, OperatorCreationException, CertificateException {
        JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(new X500Name(subject),
                BigInteger.valueOf(System.currentTimeMillis() + (long) (Math.random() * 10000) + 1000),
                caNotBefore,
                caNotAfter,
                new X500Name(subject),
                keyPair.getPublic());
        jv3Builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
    }
}