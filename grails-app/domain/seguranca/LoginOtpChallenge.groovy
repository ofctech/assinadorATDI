package seguranca

class LoginOtpChallenge {

    String challengeId
    String username
    String email
    String otpHash
    Date expiresAt
    Date dateCreated
    Date lastUpdated
    Boolean consumed = false
    Integer attempts = 0

    static constraints = {
        challengeId nullable: false, blank: false, unique: true
        username nullable: false, blank: false
        email nullable: false, blank: false, email: true
        otpHash nullable: false, blank: false
        expiresAt nullable: false
        consumed nullable: false
        attempts nullable: false
    }

    static mapping = {
        otpHash column: 'otp_hash'
    }
}
