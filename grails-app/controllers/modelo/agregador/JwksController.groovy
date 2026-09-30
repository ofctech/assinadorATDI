package modelo.agregador

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import gestaoprojectos.IdpKeyMaterialService
import grails.core.GrailsApplication
import groovy.json.JsonOutput

class JwksController {

    GrailsApplication grailsApplication
    IdpKeyMaterialService idpKeyMaterialService

    def chavePublica() {
        def km = idpKeyMaterialService.keyMaterial

        def jwk = new RSAKey.Builder(km.publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(km.kid)
                .build()

        def jwks = new JWKSet(jwk)

        render(contentType: "application/json",
                text: JsonOutput.toJson(jwks.toJSONObject()))
    }

}
