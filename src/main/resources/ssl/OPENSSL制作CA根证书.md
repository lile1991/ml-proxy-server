### openssl制作CA根证书
生成java支持的私钥

```
openssl genrsa -des3 -out ca.key 2048
openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.pem
```

#key的转换，转换成netty支持私钥编码格式
```openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.der```

再通过CA私钥生成CA证书
```
openssl req -sha256 -new -x509 -days 365 -key ca.key -out HWProxyRoot.crt -subj "/C=CN/ST=GD/L=SZ/O=lee/OU=study/CN=HWProxyRoot"
```
上面我们就制作了一个自己的CA证书，打开HWProxyRoot.crt来看一看


参考链接：https://www.jianshu.com/p/ef4a71d2404d

### java动态签发ssl证书
JAVA自带的SSL以及X509库只能使用SSL证书，不能生成SSL证书。因此我们使用“Bouncy Castle”这个算法库来实现SSL证书的生成。
```
//maven
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.49</version>
</dependency>
```
``` 
//注册bouncycastle
Security.addProvider(new BouncyCastleProvider());
//生成ssl证书公钥和私钥
KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
caKeyPairGen.initialize(2048, new SecureRandom());
PrivateKey serverPriKey = keyPair.getPrivate();
PublicKey  serverPubKey = keyPair.getPublic();
//通过CA私钥动态签发ssl证书
public static X509Certificate genCert(String issuer, PublicKey serverPubKey, PrivateKey caPriKey, String host) throws Exception {
        X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
        String issuer = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=ProxyeeRoot";
        String subject = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + host;
        v3CertGen.reset();
        v3CertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        v3CertGen.setIssuerDN(new X509Principal(issuer));
        v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 10 * ONE_DAY));
        v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + 3650 * ONE_DAY));
        v3CertGen.setSubjectDN(new X509Principal(subject));
        v3CertGen.setPublicKey(serverPubKey);
        //SHA256 Chrome需要此哈希算法否则会出现不安全提示
        v3CertGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        //SAN扩展 Chrome需要此扩展否则会出现不安全提示
        GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, host));
        v3CertGen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);
        X509Certificate cert = v3CertGen.generateX509Certificate(caPriKey);
        return cert;
    }
```
至此我们最重要的功能已经实现了，接着就是拿着ssl证书返回给客户端即可捕获明文了。