package gestaoprojectos

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import seguranca.KeyMaterial

import java.security.KeyStore
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicReference

@CompileStatic
class IdpKeyMaterialService {

    GrailsApplication grailsApplication

    private final AtomicReference<KeyMaterial> cached = new AtomicReference<>(null)
    private volatile long loadedAtMillis = 0L

    private int cacheSeconds() {
        return grailsApplication.config.getProperty('idp.jwks.cacheSeconds', Integer, 300)
    }

    KeyMaterial getKeyMaterial() {   // ✅ ESTE GETTER É O QUE CRIA a "property" keyMaterial
        long now = System.currentTimeMillis()
        KeyMaterial current = cached.get()

        if (current != null && (now - loadedAtMillis) < cacheSeconds() * 1000L) {
            return current
        }

        synchronized (this) {
            current = cached.get()
            if (current != null && (now - loadedAtMillis) < cacheSeconds() * 1000L) {
                return current
            }

            KeyMaterial fresh = loadFromPkcs12()
            cached.set(fresh)
            loadedAtMillis = now
            return fresh
        }
    }

    private KeyMaterial loadFromPkcs12() {

        String path      = grailsApplication.config.getProperty('idp.keystore.path', String)
        String storePass = grailsApplication.config.getProperty('idp.keystore.storePass', String)
        String alias     = grailsApplication.config.getProperty('idp.keystore.keyAlias', String)
        String keyPass   = grailsApplication.config.getProperty('idp.keystore.keyPass', String, storePass)

        if (!path || !storePass || !alias) {
            throw new IllegalStateException("Config incompleta: idp.keystore.path/storePass/keyAlias")
        }

        KeyStore ks = KeyStore.getInstance("PKCS12")
        new FileInputStream(path).withCloseable { fis ->
            ks.load(fis, storePass.toCharArray())
        }

        def keyObj = ks.getKey(alias, keyPass.toCharArray())
        if (!(keyObj instanceof RSAPrivateKey)) {
            throw new IllegalStateException("Alias '${alias}' não contém RSA private key")
        }

        def certObj = ks.getCertificate(alias)
        if (!(certObj instanceof X509Certificate)) {
            throw new IllegalStateException("Alias '${alias}' não contém X509Certificate")
        }

        RSAPrivateKey privateKey = (RSAPrivateKey) keyObj
        X509Certificate cert = (X509Certificate) certObj
        RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey()

/*        byte[] sha = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())
        String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(sha).substring(0, 22)*/
        byte[] sha = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())
        String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(sha)  // ✅ sem substring


        return new KeyMaterial(privateKey, publicKey, cert, kid)
    }
}
