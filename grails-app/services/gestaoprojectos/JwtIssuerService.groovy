package gestaoprojectos

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import modelo.agregador.Funcionario
import seguranca.KeyMaterial

@CompileStatic
class JwtIssuerService {

    GrailsApplication grailsApplication
    IdpKeyMaterialService idpKeyMaterialService

    String issueAccessToken(String subject, List<String> roles = []) {
        String issuer   = grailsApplication.config.getProperty('idp.issuer', String, "https://assinadorapi.incm.gov.mz")
        String audience = grailsApplication.config.getProperty('idp.audience', String, "grails-api")
        Integer ttl     = grailsApplication.config.getProperty('idp.tokenTtlSeconds', Integer, 900)

        if (!issuer) throw new IllegalStateException("Config obrigatória: idp.issuer")
        KeyMaterial km = idpKeyMaterialService.keyMaterial
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(km.kid)
                .build()

        //KeyMaterial km = idpKeyMaterialService.getKeyMaterial() // ✅ agora é tipado

        Date now = new Date()
        Date exp = new Date(now.time + (ttl as long) * 1000L)

/*        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID("km.kid")
                .build()*/

        Funcionario funcionario = Funcionario.findWhere(email: subject?.concat("@incm.gov.mz"))

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(audience)
                .issueTime(now)
                /*.notBeforeTime(now)*/
                .expirationTime(exp)
                /*.jwtID(UUID.randomUUID().toString())*/
                //.claim("roles", roles ?: [])
                //.claim("preferred_username", funcionario?.nome)
                //.claim("given_name", funcionario?.nome)
                //.claim("family_name", funcionario?.nome)
                .claim("birthdate", funcionario?.dataNascimento?.toString()?.take(10))
                .claim("name", funcionario?.nome)
                .claim("email", funcionario?.email)
                .claim("nuit", funcionario?.nuit?.toString())
                /*.claim("celular", "${funcionario?.celular}")*/
                .build()

        SignedJWT jwt = new SignedJWT(header, claims)
        jwt.sign(new RSASSASigner(km.privateKey))

        return jwt.serialize()
    }
}
