package gestaoprojectos

import grails.core.GrailsApplication

import javax.net.ssl.SSLSocketFactory
import java.nio.charset.StandardCharsets

class OtpEmailService {

    GrailsApplication grailsApplication

    void sendOtp(String recipient, String otp, Integer ttlSeconds) {
        Boolean enabled = grailsApplication.config.getProperty('otp.email.enabled', Boolean, false)
        if (!enabled) {
            log.warn("Envio de OTP por email desativado. Codigo OTP para ${recipient}: ${otp}")
            return
        }

        String host = grailsApplication.config.getProperty('otp.email.host', String)
        Integer port = grailsApplication.config.getProperty('otp.email.port', Integer, 25)
        String from = grailsApplication.config.getProperty('otp.email.from', String)
        String username = grailsApplication.config.getProperty('otp.email.username', String)
        String password = grailsApplication.config.getProperty('otp.email.password', String)
        Boolean startTls = grailsApplication.config.getProperty('otp.email.startTls', Boolean, false)
        Boolean ssl = grailsApplication.config.getProperty('otp.email.ssl', Boolean, false)
        Integer timeoutMs = grailsApplication.config.getProperty('otp.email.timeoutMs', Integer, 10000)

        if (!host) {
            throw new IllegalStateException('Config obrigatoria: otp.email.host')
        }
        if (!from) {
            throw new IllegalStateException('Config obrigatoria: otp.email.from')
        }

        Socket socket = null
        BufferedReader reader = null
        BufferedWriter writer = null

        try {
            socket = ssl ? SSLSocketFactory.default.createSocket(host, port) as Socket : new Socket(host, port)
            socket.soTimeout = timeoutMs
            reader = new BufferedReader(new InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
            writer = new BufferedWriter(new OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))

            expect(reader, 220)
            command(writer, reader, "EHLO ${localHostName()}", 250)

            if (startTls && !ssl) {
                command(writer, reader, 'STARTTLS', 220)
                Socket tlsSocket = SSLSocketFactory.default.createSocket(socket, host, port, true) as Socket
                tlsSocket.soTimeout = timeoutMs
                socket = tlsSocket
                reader = new BufferedReader(new InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
                writer = new BufferedWriter(new OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))
                command(writer, reader, "EHLO ${localHostName()}", 250)
            }

            if (username && password) {
                command(writer, reader, 'AUTH LOGIN', 334)
                command(writer, reader, username.bytes.encodeBase64().toString(), 334)
                command(writer, reader, password.bytes.encodeBase64().toString(), 235)
            }

            command(writer, reader, "MAIL FROM:<${from}>", 250)
            command(writer, reader, "RCPT TO:<${recipient}>", 250)
            command(writer, reader, 'DATA', 354)
            writer.write(message(from, recipient, otp, ttlSeconds))
            writer.write('\r\n.\r\n')
            writer.flush()
            expect(reader, 250)
            command(writer, reader, 'QUIT', 221)
        } finally {
            try { writer?.close() } catch (Exception ignored) {}
            try { reader?.close() } catch (Exception ignored) {}
            try { socket?.close() } catch (Exception ignored) {}
        }
    }

    private String message(String from, String recipient, String otp, Integer ttlSeconds) {
        Integer minutes = Math.max(1, Math.round(ttlSeconds / 60.0f) as Integer)
        String subject = grailsApplication.config.getProperty('otp.email.subject', String, 'Codigo OTP do Assinador')

        [
                "From: ${from}",
                "To: ${recipient}",
                "Subject: ${subject}",
                'MIME-Version: 1.0',
                'Content-Type: text/plain; charset=UTF-8',
                '',
                'Use o codigo abaixo para concluir o acesso ao Portal de Assinatura Digital.',
                '',
                "Codigo: ${otp}",
                "Validade: ${minutes} minuto(s).",
                '',
                'Se nao iniciou este acesso, ignore esta mensagem.'
        ].join('\r\n')
    }

    private void command(BufferedWriter writer, BufferedReader reader, String command, Integer expectedCode) {
        writer.write(command)
        writer.write('\r\n')
        writer.flush()
        expect(reader, expectedCode)
    }

    private void expect(BufferedReader reader, Integer expectedCode) {
        String line = reader.readLine()
        if (!line) {
            throw new IOException('Resposta SMTP vazia')
        }

        String response = line
        while (line.length() > 3 && line.charAt(3) == ('-' as char)) {
            line = reader.readLine()
            if (!line) break
            response += "\n${line}"
        }

        if (!response.startsWith(expectedCode.toString())) {
            throw new IOException("Resposta SMTP inesperada. Esperado ${expectedCode}, recebido: ${response}")
        }
    }

    private String localHostName() {
        try {
            return InetAddress.localHost.hostName ?: 'localhost'
        } catch (Exception ignored) {
            return 'localhost'
        }
    }
}
