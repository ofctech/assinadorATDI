package seguranca

import groovy.transform.CompileStatic
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.cert.X509Certificate

@CompileStatic
class KeyMaterial {
    final RSAPrivateKey privateKey
    final RSAPublicKey publicKey
    final X509Certificate cert
    final String kid

    KeyMaterial(RSAPrivateKey privateKey, RSAPublicKey publicKey, X509Certificate cert, String kid) {
        this.privateKey = privateKey
        this.publicKey = publicKey
        this.cert = cert
        this.kid = kid
    }
}
