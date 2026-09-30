package modelo.agregador

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugin.springsecurity.annotation.Secured

class AuthController {

    GrailsApplication grailsApplication
    def ldapAuthService
    def jwtIssuerService
    def loginOtpService


    @Secured('ROLE_ADMIN')
    def generateToken() {
        String username = params.username

        if (!username) {
            response.status = 400
            render([res: false, success: false, error: 'missing_username'] as JSON)
            return
        }

        def roles = ['IS_AUTHENTICATED_ANONYMOUSLY']
        String token = jwtIssuerService.issueAccessToken(username, roles)

        response.status = 201
        render([res: true, success: true, status: 201, access_token: token, message: 'Logged IN'] as JSON)
    }

}
