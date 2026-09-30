package gestaoprojectos

import grails.core.GrailsApplication

class OtpEmailService {

    GrailsApplication grailsApplication
    def mailService

    void sendOtp(String recipient, String otp, Integer ttlSeconds) {
        String fromAddress = grailsApplication.config.getProperty('grails.mail.from_address', String) ?:
                grailsApplication.config.getProperty('mail.from_address', String)
        String fromName = grailsApplication.config.getProperty('grails.mail.from_name', String) ?:
                grailsApplication.config.getProperty('mail.from_name', String, 'Assinador Avancado INCM')
        String mailSubject = grailsApplication.config.getProperty('grails.mail.otp_subject', String) ?:
                grailsApplication.config.getProperty('mail.otp_subject', String, 'Codigo OTP do Assinador')

        if (!fromAddress) {
            throw new IllegalStateException('Config obrigatoria: mail.from_address')
        }

        mailService.sendMail {
            to recipient
            from mailbox(fromName, fromAddress)
            subject mailSubject
            body otpMessage(otp, ttlSeconds)
        }
    }

    private String otpMessage(String otp, Integer ttlSeconds) {
        Integer minutes = Math.max(1, Math.round(ttlSeconds / 60.0f) as Integer)

        [
                'Use o codigo abaixo para concluir o acesso ao Portal de Assinatura Digital.',
                '',
                "Codigo: ${otp}",
                "Validade: ${minutes} minuto(s).",
                '',
                'Se nao iniciou este acesso, ignore esta mensagem.'
        ].join('\n')
    }

    private String mailbox(String name, String address) {
        if (!name) {
            return address
        }

        "${name} <${address}>"
    }
}
