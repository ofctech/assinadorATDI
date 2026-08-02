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

    def configuration() {
        String issuer = grailsApplication.config.getProperty('idp.issuer', String, 'https://apiagregador.incm.gov.mz')

        render(contentType: "application/json", text: JsonOutput.toJson([
                issuer: issuer,
                jwks_uri: "${issuer}/api/jwks/idp".toString(),
                id_token_signing_alg_values_supported: ["RS256"],
                subject_types_supported: ["public"],
                response_types_supported: ["token"]
        ]))
    }
}
