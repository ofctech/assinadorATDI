package modelo.agregador

import grails.converters.JSON

class LdapLoginController {

    def ldapAuthService
    def jwtIssuerService
    def loginOtpService

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

        def challenge = loginOtpService.createChallenge(username)

        render([
                res: true,
                success: true,
                status: 202,
                otp_required: true,
                challenge_id: challenge.challengeId,
                expires_in: challenge.expiresIn,
                email: maskEmail(challenge.email as String),
                message: 'OTP sent'
        ] as JSON)
    }

    def authenticate() {
        login()
    }

    def verifyOtp() {
        def verificationRequest = request.JSON

        String challengeId = verificationRequest.challenge_id
        String otp = verificationRequest.otp

        if (!challengeId || !otp) {
            render([res: false, success: false, status: "E0", message: 'missing_otp.'] as JSON)
            return
        }

        try {
            String username = loginOtpService.verify(challengeId, otp)
            def roles = ["IS_AUTHENTICATED_ANONYMOUSLY"]
            String token = jwtIssuerService.issueAccessToken(username, roles)

            render([res: true, success: true, status: 201, access_token: token, message: 'Logged IN'] as JSON)
        } catch (gestaoprojectos.OtpVerificationException e) {
            response.status = 401
            render([res: false, success: false, status: "E0", error: e.message, message: otpErrorMessage(e.message)] as JSON)
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

    private String otpErrorMessage(String error) {
        switch (error) {
            case 'otp_expired':
                return 'Codigo expirado.'
            case 'too_many_attempts':
                return 'Numero maximo de tentativas excedido.'
            case 'invalid_otp':
                return 'Codigo invalido.'
            default:
                return 'Desafio OTP invalido.'
        }
    }
}
