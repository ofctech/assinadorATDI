package modelo.agregador

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartFile

import java.nio.charset.StandardCharsets
import java.util.UUID

@Secured('ROLE_ADMIN')
class SignerController {

    private static final Long DEFAULT_MAX_FILE_SIZE_BYTES = 3L * 1024L * 1024L

    GrailsApplication grailsApplication
    def operacaoEstatisticaService

    def sign() {

        String certificateId = params.certificateId ?: '1'
        String authorization = request.getHeader('Authorization')

        MultipartFile file
        try {
            file = request.getFile('file')
        } catch (MaxUploadSizeExceededException ignored) {
            renderFileTooLargeResponse(DEFAULT_MAX_FILE_SIZE_BYTES, null)
            return
        } catch (IllegalStateException ignored) {
            renderFileTooLargeResponse(DEFAULT_MAX_FILE_SIZE_BYTES, null)
            return
        }

        if (!authorization) {
            response.status = 400
            render([success: false, error: 'missing_authorization'] as JSON)
            return
        }

        if (!file || file.empty) {
            response.status = 400
            render([success: false, error: 'missing_file'] as JSON)
            return
        }

        if (!isPdf(file)) {
            response.status = 400
            render([
                    success: false,
                    error: 'invalid_file_type',
                    message: 'O anexo deve ser um ficheiro PDF.'
            ] as JSON)
            return
        }

        Long fileSizeLimit = getMaxFileSizeBytes()
        if (file.size > fileSizeLimit) {
            renderFileTooLargeResponse(fileSizeLimit, file.size)
            return
        }

        HttpURLConnection connection
        try {
            String boundary = "----AssinadorBoundary${System.currentTimeMillis()}"
            connection = openConnection(certificateId, authorization, boundary, 'signer', 'sign')
            writeSignMultipartRequest(connection, boundary, file)

            int statusCode = connection.responseCode
            byte[] responseBody = readResponse(connection, statusCode)

            response.status = statusCode
            String responseContentType = connection.getHeaderField('Content-Type')
            if (responseContentType) {
                response.contentType = responseContentType
            }
            recordSuccessfulOperation(statusCode, 'signatures')
            response.outputStream.write(responseBody)
            response.outputStream.flush()

        } catch (Exception e) {
            log.error('Erro ao assinar documento', e)
            response.status = 502
            render([success: false, error: 'signer_unavailable', message: e.message] as JSON)
        } finally {
            connection?.disconnect()
        }
    }

    def verify() {
        String certificateId = params.certificateId ?: '1'
        String authorization = request.getHeader('Authorization')
        MultipartFile file
        try {
            file = request.getFile('file')
        } catch (MaxUploadSizeExceededException ignored) {
            renderFileTooLargeResponse(DEFAULT_MAX_FILE_SIZE_BYTES, null)
            return
        } catch (IllegalStateException ignored) {
            renderFileTooLargeResponse(DEFAULT_MAX_FILE_SIZE_BYTES, null)
            return
        }

        if (!authorization) {
            response.status = 400
            render([success: false, error: 'missing_authorization'] as JSON)
            return
        }

        if (!file || file.empty) {
            response.status = 400
            render([success: false, error: 'missing_file'] as JSON)
            return
        }

        if (!isPdf(file)) {
            response.status = 400
            render([
                    success: false,
                    error: 'invalid_file_type',
                    message: 'O anexo deve ser um ficheiro PDF.'
            ] as JSON)
            return
        }

        Long fileSizeLimit = getMaxFileSizeBytes()
        if (file.size > fileSizeLimit) {
            renderFileTooLargeResponse(fileSizeLimit, file.size)
            return
        }

        HttpURLConnection connection
        try {
            String boundary = "----AssinadorBoundary${System.currentTimeMillis()}"
            connection = openConnection(certificateId, authorization, boundary, 'verifier', 'verify')
            writeFileOnlyMultipartRequest(connection, boundary, file)

            int statusCode = connection.responseCode
            byte[] responseBody = readResponse(connection, statusCode)

            response.status = statusCode
            String responseContentType = connection.getHeaderField('Content-Type')
            if (responseContentType) {
                response.contentType = responseContentType
            }
            recordSuccessfulOperation(statusCode, 'validations')
            response.outputStream.write(responseBody)
            response.outputStream.flush()
        } catch (Exception e) {
            log.error('Erro ao validar documento', e)
            response.status = 502
            render([success: false, error: 'verifier_unavailable', message: e.message] as JSON)
        } finally {
            connection?.disconnect()
        }
    }

    private HttpURLConnection openConnection(
            String certificateId,
            String authorization,
            String boundary,
            String resource,
            String action
    ) {
        String baseUrl = grailsApplication.config.getProperty(
                'signer.baseUrl',
                String,
                'https://assinadoravancado.gov.mz'
        )
        Integer connectTimeoutMs = grailsApplication.config.getProperty('signer.connectTimeoutMs', Integer, 10000)
        Integer readTimeoutMs = grailsApplication.config.getProperty('signer.readTimeoutMs', Integer, 60000)
        URL url = new URL("${baseUrl}/api/${resource}/pdf/1/${action}")
        HttpURLConnection connection = (HttpURLConnection) url.openConnection()
        connection.requestMethod = 'POST'
        connection.doOutput = true
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty('Authorization', authorization)
        connection.setRequestProperty('Accept', 'application/json, application/pdf, */*')
        connection.setRequestProperty('Content-Type', "multipart/form-data; boundary=${boundary}")
        connection
    }

    private void writeSignMultipartRequest(
            HttpURLConnection connection,
            String boundary,
            MultipartFile file
    ) {
        OutputStream output = connection.outputStream
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)

        [
                stamp_enable: true,
                stamp_QR_code_enable: true,
                field_name: UUID.randomUUID().toString(),
                send_email: params.send_email ?: false,
                attach_signed_document: true
        ].each { String name, Object value ->
            if (value != null) {
                writeFormField(writer, boundary, name, value.toString())
            }
        }

        writeFileField(writer, output, boundary, 'file', file)

        writer.append("--${boundary}--").append('\r\n')
        writer.flush()
        writer.close()
    }

    private void writeFileOnlyMultipartRequest(
            HttpURLConnection connection,
            String boundary,
            MultipartFile file
    ) {
        OutputStream output = connection.outputStream
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)

        writeFileField(writer, output, boundary, 'file', file)

        writer.append("--${boundary}--").append('\r\n')
        writer.flush()
        writer.close()
    }

    private void writeFormField(PrintWriter writer, String boundary, String name, String value) {
        writer.append("--${boundary}").append('\r\n')
        writer.append("Content-Disposition: form-data; name=\"${name}\"").append('\r\n')
        writer.append('Content-Type: text/plain; charset=UTF-8').append('\r\n')
        writer.append('\r\n')
        writer.append(value).append('\r\n')
        writer.flush()
    }

    private void writeFileField(
            PrintWriter writer,
            OutputStream output,
            String boundary,
            String name,
            MultipartFile multipartFile
    ) {
        String fileName = multipartFile.originalFilename ?: name
        String contentType = multipartFile.contentType ?: 'application/octet-stream'

        writer.append("--${boundary}").append('\r\n')
        writer.append("Content-Disposition: form-data; name=\"${name}\"; filename=\"${fileName}\"").append('\r\n')
        writer.append("Content-Type: ${contentType}").append('\r\n')
        writer.append('\r\n')
        writer.flush()

        multipartFile.inputStream.withStream { InputStream input ->
            byte[] buffer = new byte[8192]
            int read
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read)
            }
            output.flush()
        }

        writer.append('\r\n')
        writer.flush()
    }

    private byte[] readResponse(HttpURLConnection connection, int statusCode) {
        InputStream input = statusCode >= 400 ? connection.errorStream : connection.inputStream
        if (!input) {
            return new byte[0]
        }
        input.bytes
    }

    private Long getMaxFileSizeBytes() {
        grailsApplication.config.getProperty('signer.maxFileSizeBytes', Long, DEFAULT_MAX_FILE_SIZE_BYTES)
    }

    private String formatBytes(Long bytes) {
        Long megabytes = bytes.intdiv(1024L).intdiv(1024L)
        "${megabytes}MB"
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.contentType?.toLowerCase()
        String fileName = file.originalFilename?.toLowerCase()

        contentType == 'application/pdf' || fileName?.endsWith('.pdf')
    }

    private void recordSuccessfulOperation(int statusCode, String tipo) {
        if (statusCode < 200 || statusCode >= 300) {
            return
        }

        try {
            operacaoEstatisticaService.incrementar(tipo)
        } catch (Exception e) {
            log.error("Erro ao gravar estatistica da operacao ${tipo}", e)
        }
    }

    private void renderFileTooLargeResponse(Long fileSizeLimit, Long fileSize) {
        response.status = 413
        render([
                success: false,
                error: 'file_too_large',
                message: "O anexo nao pode exceder ${formatBytes(fileSizeLimit)}.",
                maxFileSizeBytes: fileSizeLimit,
                fileSizeBytes: fileSize
        ] as JSON)
    }
}
