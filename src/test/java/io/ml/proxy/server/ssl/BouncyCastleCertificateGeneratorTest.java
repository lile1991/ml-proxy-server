package io.ml.proxy.server.ssl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BouncyCastleCertificateGeneratorTest {

    public static void main(String[] args) throws Exception {
        //生成ca证书和私钥
        KeyPair keyPair = BouncyCastleCertificateGenerator.genKeyPair();
        File caCertFile = new File("./http-proxy/src/main/resources/ssl/ca.crt");
        if (caCertFile.exists()) {
            caCertFile.delete();
        }

        Files.write(Paths.get(caCertFile.toURI()),
                BouncyCastleCertificateGenerator.generateCaCert(
                        "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=HWProxy",
                        new Date(),
                        new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650)),
                        keyPair)
                        .getEncoded());

        File caPriKeyFile = new File("./http-proxy/src/main/resources/ssl/ca_private.der");
        if (caPriKeyFile.exists()) {
            caPriKeyFile.delete();
        }

        Files.write(caPriKeyFile.toPath(),
                new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded()).getEncoded());
    }

}
