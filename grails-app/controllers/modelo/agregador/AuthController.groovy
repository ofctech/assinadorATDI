package modelo.agregador

import grails.core.GrailsApplication

class AuthController {

    GrailsApplication grailsApplication
    def ldapAuthService
    def jwtIssuerService

    def login() {
        String username = params.username
        String password = params.password

        if (!username || !password) {
            response.status = 400
            render(contentType: "application/json") { [error: "missing_credentials"] }
            return
        }

        boolean ok = ldapAuthService.authenticate(username, password)
        if (!ok) {
            response.status = 401
            render(contentType: "application/json") { [error: "invalid_credentials"] }
            return
        }

        // TODO produção: mapear grupos LDAP → roles (memberOf)
        def roles = ["user"]

        String token = jwtIssuerService.issueAccessToken(username, roles)
        Integer ttl = grailsApplication.config.getProperty('idp.tokenTtlSeconds', Integer, 900)

        render(contentType: "application/json") {
            [access_token: token, token_type: "Bearer", expires_in: ttl]
        }
    }
}
