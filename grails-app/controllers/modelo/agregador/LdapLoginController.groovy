package modelo.agregador

import grails.converters.JSON

class LdapLoginController {

    def ldapAuthService
    def jwtIssuerService

    def login() {
        def authenticationRequest = request.JSON

        String username = authenticationRequest.username
        String password = authenticationRequest.password

        if (!username || !password) {
            render([res: false, success: false, status: "E0", message: 'missing_credentials.'] as JSON)
            return
        }

        boolean ok = ldapAuthService.authenticate(username, password)
        if (!ok) {
            render([res: false, success: false, status: "E0", message: 'Invalid Credentials.'] as JSON)
            return
        }

        def roles = ["IS_AUTHENTICATED_ANONYMOUSLY"]
        String token = jwtIssuerService.issueAccessToken(username, roles)

        render([res: true, success: true, status: 201, access_token: token, message: 'Logged IN'] as JSON)
    }

    def authenticate() {
        login()
    }
}
