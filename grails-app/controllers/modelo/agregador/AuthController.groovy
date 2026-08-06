package modelo.agregador

import grails.core.GrailsApplication

class AuthController {

    GrailsApplication grailsApplication
    def ldapAuthService
    def jwtIssuerService
    def loginOtpService

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

        def challenge = loginOtpService.createChallenge(username)

        render(contentType: "application/json") {
            [
                    otp_required: true,
                    challenge_id: challenge.challengeId,
                    expires_in: challenge.expiresIn,
                    email: maskEmail(challenge.email as String)
            ]
        }
    }

    def verifyOtp() {
        String challengeId = params.challenge_id
        String otp = params.otp

        if (!challengeId || !otp) {
            response.status = 400
            render(contentType: "application/json") { [error: "missing_otp"] }
            return
        }

        try {
            String username = loginOtpService.verify(challengeId, otp)
            def roles = ["user"]
            String token = jwtIssuerService.issueAccessToken(username, roles)
            Integer ttl = grailsApplication.config.getProperty('idp.tokenTtlSeconds', Integer, 900)

            render(contentType: "application/json") {
                [access_token: token, token_type: "Bearer", expires_in: ttl]
            }
        } catch (gestaoprojectos.OtpVerificationException e) {
            response.status = 401
            render(contentType: "application/json") { [error: e.message] }
        }
    }

    private String maskEmail(String email) {
        if (!email || !email.contains('@')) {
            return email
        }

        def parts = email.split('@', 2)
        String name = parts[0]
        String domain = parts[1]
        String maskedName = name.length() <= 2 ? "${name.take(1)}*" : "${name.take(2)}***"
        "${maskedName}@${domain}"
    }
}
