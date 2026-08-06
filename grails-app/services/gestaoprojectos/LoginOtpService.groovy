package gestaoprojectos

import grails.core.GrailsApplication
import modelo.agregador.Funcionario
import seguranca.LoginOtpChallenge

import java.security.MessageDigest
import java.security.SecureRandom

class LoginOtpService {

    private static final SecureRandom RANDOM = new SecureRandom()

    GrailsApplication grailsApplication
    OtpEmailService otpEmailService

    Map<String, Object> createChallenge(String username) {
        Integer ttlSeconds = grailsApplication.config.getProperty('otp.ttlSeconds', Integer, 120)
        Integer digits = grailsApplication.config.getProperty('otp.digits', Integer, 6)
        String otp = generateOtp(digits)
        String email = resolveEmail(username)
        String challengeId = UUID.randomUUID().toString()

        new LoginOtpChallenge(
                challengeId: challengeId,
                username: username,
                email: email,
                otpHash: hashOtp(challengeId, otp),
                expiresAt: new Date(System.currentTimeMillis() + (ttlSeconds as long) * 1000L),
                consumed: false,
                attempts: 0
        ).save(failOnError: true, flush: true)

        otpEmailService.sendOtp(email, otp, ttlSeconds)

        [
                challengeId: challengeId,
                expiresIn: ttlSeconds,
                email: email
        ] as Map<String, Object>
    }

    String verify(String challengeId, String otp) {
        Integer maxAttempts = grailsApplication.config.getProperty('otp.maxAttempts', Integer, 5)
        LoginOtpChallenge challenge = LoginOtpChallenge.findByChallengeId(challengeId)

        if (!challenge || challenge.consumed) {
            throw new OtpVerificationException('invalid_challenge')
        }

        if (challenge.expiresAt.before(new Date())) {
            throw new OtpVerificationException('otp_expired')
        }

        if ((challenge.attempts ?: 0) >= maxAttempts) {
            throw new OtpVerificationException('too_many_attempts')
        }

        challenge.attempts = (challenge.attempts ?: 0) + 1

        if (challenge.otpHash != hashOtp(challenge.challengeId, otp ?: '')) {
            challenge.save(failOnError: true, flush: true)
            throw new OtpVerificationException('invalid_otp')
        }

        challenge.consumed = true
        challenge.save(failOnError: true, flush: true)

        challenge.username
    }

    private String resolveEmail(String username) {
        Funcionario funcionario = Funcionario.findWhere(email: username?.concat('@incm.gov.mz'))
        if (funcionario?.email) {
            return funcionario.email
        }

        if (username?.contains('@')) {
            return username
        }

        String defaultDomain = grailsApplication.config.getProperty('otp.email.defaultDomain', String, 'incm.gov.mz')
        "${username}@${defaultDomain}"
    }

    private String generateOtp(Integer digits) {
        Integer max = Math.pow(10, digits) as Integer
        Integer min = Math.pow(10, digits - 1) as Integer
        String.valueOf(min + RANDOM.nextInt(max - min))
    }

    private String hashOtp(String challengeId, String otp) {
        String secret = grailsApplication.config.getProperty('otp.hashSecret', String, 'assinador-otp')
        byte[] digest = MessageDigest.getInstance('SHA-256')
                .digest("${challengeId}:${otp}:${secret}".getBytes('UTF-8'))
        digest.encodeHex().toString()
    }
}

class OtpVerificationException extends RuntimeException {
    OtpVerificationException(String message) {
        super(message)
    }
}
