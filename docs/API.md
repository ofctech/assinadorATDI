# APIs consumidas pelo frontend

Esta documentacao descreve o contrato entre o frontend Angular deste projecto e o backend Grails do Assinador.

O objectivo aqui e documentar as chamadas feitas pelo frontend para este backend: autenticacao, assinatura de PDF e validacao de PDF.

## Base URL usada pelo frontend

O frontend usa `environment.apiBaseUrl` como prefixo para todas as chamadas.

Ambiente local:

```ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8091',
};
```

Ambiente de producao:

```ts
export const environment = {
  production: true,
  apiBaseUrl: 'https://assinadorapi.incm.gov.mz',
};
```

Exemplo de URL final em desenvolvimento:

```text
http://localhost:8091/api/ldapLogin/login
```

## Fluxo do frontend

1. O utilizador informa `username` e `password`.
2. O frontend chama `POST /api/ldapLogin/login`.
3. O backend valida as credenciais no LDAP, cria um desafio OTP valido por 2 minutos e envia o codigo por email.
4. O frontend mostra o campo OTP e chama `POST /api/ldapLogin/verify-otp`.
5. O backend valida o OTP e devolve `access_token`.
6. O frontend guarda o token em `sessionStorage` com a chave `assinador_token`.
7. Para assinar ou validar PDF, o frontend envia o header `Authorization: Bearer <access_token>`.
8. O backend valida a presenca do header, valida o ficheiro PDF e encaminha a operacao para o servico de assinatura/validacao configurado no backend.

## Headers comuns

### Autenticacao

```http
Content-Type: application/json
```

### Assinatura e validacao

```http
Authorization: Bearer <access_token>
```

Ao enviar `FormData`, o frontend nao deve definir manualmente o header `Content-Type`; o browser define automaticamente `multipart/form-data` com o `boundary` correcto.

## 1. Autenticacao

### POST /api/ldapLogin/login

Endpoint usado actualmente pelo frontend para validar as credenciais LDAP e iniciar o desafio OTP.

Autentica o utilizador no LDAP, envia o OTP por email e devolve o identificador do desafio. Este endpoint ja nao devolve JWT no fluxo normal.

### Chamada feita pelo frontend

```ts
const body = { username: this.username, password: this.password };

this.http.post<LoginResponse>(
  `${this.apiBaseUrl}/api/ldapLogin/login`,
  body,
  {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
  },
);
```

### Requisicao

```http
POST /api/ldapLogin/login
Content-Type: application/json
```

```json
{
  "username": "utilizador",
  "password": "palavra-passe"
}
```

### Resposta de sucesso

HTTP `200 OK`

```json
{
  "res": true,
  "success": true,
  "status": 202,
  "otp_required": true,
  "challenge_id": "0f859a81-9e90-4fb4-a9cb-c7246f36e137",
  "expires_in": 120,
  "email": "ut***@incm.gov.mz",
  "message": "OTP sent"
}
```

### Tratamento no frontend

O frontend mostra a etapa de OTP quando `otp_required` e `true` e `challenge_id` existe na resposta.

```ts
this.otpChallengeId.set(response.challenge_id);
this.otpExpiresIn.set(response.expires_in ?? 120);
```

### Respostas de erro

Credenciais ausentes:

```json
{
  "res": false,
  "success": false,
  "status": "E0",
  "message": "missing_credentials."
}
```

Credenciais invalidas:

```json
{
  "res": false,
  "success": false,
  "status": "E0",
  "message": "Invalid Credentials."
}
```

Observacao importante: este endpoint renderiza os erros acima sem definir explicitamente `response.status`. Na pratica, o frontend pode receber HTTP `200 OK` mesmo quando `success` e `false`. Por isso, para este endpoint, o frontend deve validar `otp_required` e `challenge_id` antes de avancar para a etapa do OTP.

### POST /api/ldapLogin/login-without-twofactor

Endpoint para aplicacoes de terceiros que precisam autenticar no LDAP e receber o JWT sem desafio OTP.

Este endpoint valida `username` e `password` no LDAP e, em caso de sucesso, devolve directamente `access_token`.

### Requisicao

```http
POST /api/ldapLogin/login-without-twofactor
Content-Type: application/json
```

```json
{
  "username": "utilizador",
  "password": "palavra-passe"
}
```

### Resposta de sucesso

HTTP `200 OK`

```json
{
  "res": true,
  "success": true,
  "status": 201,
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "message": "Logged IN"
}
```

### Respostas de erro

Credenciais ausentes:

```json
{
  "res": false,
  "success": false,
  "status": "E0",
  "message": "missing_credentials."
}
```

Credenciais invalidas:

```json
{
  "res": false,
  "success": false,
  "status": "E0",
  "message": "Invalid Credentials."
}
```

### POST /api/ldapLogin/verify-otp

Endpoint usado pelo frontend para confirmar o OTP enviado por email. So depois desta confirmacao o backend emite o JWT.

### Chamada feita pelo frontend

```ts
const body = { challenge_id: this.otpChallengeId(), otp: this.otp };

this.http.post<LoginResponse>(
  `${this.apiBaseUrl}/api/ldapLogin/verify-otp`,
  body,
  {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
  },
);
```

### Requisicao

```http
POST /api/ldapLogin/verify-otp
Content-Type: application/json
```

```json
{
  "challenge_id": "0f859a81-9e90-4fb4-a9cb-c7246f36e137",
  "otp": "123456"
}
```

### Resposta de sucesso

HTTP `200 OK`

```json
{
  "res": true,
  "success": true,
  "status": 201,
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "message": "Logged IN"
}
```

### Tratamento no frontend

O frontend considera a sessao iniciada quando `access_token` existe nesta resposta.

```ts
this.token.set(response.access_token);
sessionStorage.setItem('assinador_token', response.access_token);
```

### Respostas de erro

OTP ausente:

```json
{
  "res": false,
  "success": false,
  "status": "E0",
  "message": "missing_otp."
}
```

OTP invalido, expirado, consumido ou com tentativas excedidas:

HTTP `401 Unauthorized`

```json
{
  "res": false,
  "success": false,
  "status": "E0",
  "error": "invalid_otp",
  "message": "Codigo invalido."
}
```

Valores possiveis em `error`:

| Erro | Significado |
| --- | --- |
| `invalid_challenge` | O desafio nao existe ou ja foi consumido. |
| `otp_expired` | O codigo expirou. |
| `too_many_attempts` | O limite de tentativas foi excedido. |
| `invalid_otp` | O codigo informado nao corresponde ao desafio. |

## 2. Assinatura de PDF

### POST /api/signer/sign

Endpoint usado pelo frontend para enviar um PDF ao backend e receber o documento assinado ou uma resposta JSON do servico de assinatura.

### Chamada feita pelo frontend

```ts
const formData = new FormData();
formData.append('file', file);
formData.append('send_email', String(this.sendEmail));
formData.append('attach_signed_document', String(this.attachSignedDocument));

this.http.post(
  `${this.apiBaseUrl}/api/signer/sign`,
  formData,
  {
    headers: new HttpHeaders({ Authorization: `Bearer ${this.token()}` }),
    observe: 'response',
    responseType: 'blob',
  },
);
```

### Requisicao

```http
POST /api/signer/sign
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

Campos enviados pelo frontend:

| Campo | Tipo | Obrigatorio | Descricao |
| --- | --- | --- | --- |
| `file` | `File` | Sim | PDF a assinar. |
| `send_email` | `string` | Nao | Valor textual `true` ou `false`. |
| `attach_signed_document` | `string` | Nao | Valor textual `true` ou `false`; actualmente enviado pelo frontend. |

Exemplo com `curl`:

```bash
curl -X POST "http://localhost:8091/api/signer/sign" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@documento.pdf;type=application/pdf" \
  -F "send_email=true" \
  -F "attach_signed_document=true" \
  --output documento-assinado.pdf
```

### Validacoes feitas antes do envio

O frontend valida:

| Validacao | Regra |
| --- | --- |
| Sessao | Deve existir token em memoria ou em `sessionStorage`. |
| Tipo de ficheiro | `file.type === 'application/pdf'` ou nome terminado em `.pdf`. |
| Tamanho maximo | `3MB`. |

O backend tambem valida:

| Validacao | Erro |
| --- | --- |
| Header `Authorization` ausente | `missing_authorization` |
| Ficheiro ausente | `missing_file` |
| Ficheiro nao PDF | `invalid_file_type` |
| Ficheiro acima de `3MB` | `file_too_large` |

### Resposta de sucesso

O frontend espera a resposta como `Blob`.

Se o `Content-Type` incluir `application/pdf`, o frontend descarrega o ficheiro automaticamente.

Nome de ficheiro:

1. Primeiro tenta usar `Content-Disposition`.
2. Se nao existir, usa `documento-assinado.pdf`.

Quando o `Content-Type` nao e PDF, o frontend converte o `Blob` para texto e tenta formatar como JSON.

### Erros devolvidos pelo backend

Header `Authorization` ausente:

HTTP `400 Bad Request`

```json
{
  "success": false,
  "error": "missing_authorization"
}
```

Ficheiro ausente:

HTTP `400 Bad Request`

```json
{
  "success": false,
  "error": "missing_file"
}
```

Ficheiro nao PDF:

HTTP `400 Bad Request`

```json
{
  "success": false,
  "error": "invalid_file_type",
  "message": "O anexo deve ser um ficheiro PDF."
}
```

Ficheiro acima do limite:

HTTP `413 Payload Too Large`

```json
{
  "success": false,
  "error": "file_too_large",
  "message": "O anexo nao pode exceder 3MB.",
  "maxFileSizeBytes": 3145728,
  "fileSizeBytes": 5242880
}
```

Servico de assinatura indisponivel:

HTTP `502 Bad Gateway`

```json
{
  "success": false,
  "error": "signer_unavailable",
  "message": "mensagem do erro"
}
```

## 3. Validacao de PDF

### POST /api/signer/verify

Endpoint usado pelo frontend para validar assinaturas de um PDF.

### Chamada feita pelo frontend

```ts
const formData = new FormData();
formData.append('file', file);

this.http.post(
  `${this.apiBaseUrl}/api/signer/verify`,
  formData,
  {
    headers: new HttpHeaders({ Authorization: `Bearer ${this.token()}` }),
    observe: 'response',
    responseType: 'blob',
  },
);
```

### Requisicao

```http
POST /api/signer/verify
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

Campos enviados pelo frontend:

| Campo | Tipo | Obrigatorio | Descricao |
| --- | --- | --- | --- |
| `file` | `File` | Sim | PDF a validar. |

Exemplo com `curl`:

```bash
curl -X POST "http://localhost:8091/api/signer/verify" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@documento-assinado.pdf;type=application/pdf"
```

### Validacoes feitas antes do envio

As validacoes sao as mesmas da assinatura:

| Validacao | Regra |
| --- | --- |
| Sessao | Deve existir token em memoria ou em `sessionStorage`. |
| Tipo de ficheiro | `file.type === 'application/pdf'` ou nome terminado em `.pdf`. |
| Tamanho maximo | `3MB`. |

### Resposta de sucesso esperada pelo frontend

O frontend recebe a resposta como `Blob`, converte para texto e tenta interpretar como JSON.

O frontend suporta dois formatos:

1. Um objecto JSON com uma assinatura.
2. Um array JSON com varias assinaturas.

Formato esperado para exibir o resumo de validacao:

```json
[
  {
    "signature": {
      "validity": {
        "is_valid": true,
        "invalid_cause": ""
      }
    },
    "certificate": {
      "subject": {
        "common_name": "Nome do titular",
        "email_address": "titular@exemplo.co.mz",
        "nuit": "123456789"
      },
      "issuer": {
        "common_name": "Entidade emissora"
      }
    }
  }
]
```

Campos usados pelo frontend:

| Campo | Uso |
| --- | --- |
| `signature.validity.is_valid` | Define se a assinatura e apresentada como valida ou invalida. |
| `signature.validity.invalid_cause` | Motivo exibido quando a assinatura e invalida. |
| `certificate.subject` | Dados do titular do certificado. |
| `certificate.issuer` | Dados da entidade emissora. |

Se a resposta nao for JSON valido, o frontend mostra o texto bruto formatado.

### Erros devolvidos pelo backend

| Situacao | HTTP | Corpo |
| --- | --- | --- |
| Header `Authorization` ausente | `400` | `{"success":false,"error":"missing_authorization"}` |
| Ficheiro ausente | `400` | `{"success":false,"error":"missing_file"}` |
| Ficheiro nao PDF | `400` | `{"success":false,"error":"invalid_file_type","message":"O anexo deve ser um ficheiro PDF."}` |
| Ficheiro acima de `3MB` | `413` | `{"success":false,"error":"file_too_large",...}` |
| Servico de validacao indisponivel | `502` | `{"success":false,"error":"verifier_unavailable","message":"..."}` |

## Contratos TypeScript recomendados

```ts
interface LoginResponse {
  res?: boolean;
  success?: boolean;
  status?: number | string;
  access_token?: string;
  token_type?: string;
  expires_in?: number;
  otp_required?: boolean;
  challenge_id?: string;
  email?: string;
  message?: string;
  error?: string;
}

interface DistinguishedName {
  common_name?: string;
  country_name?: string;
  email_address?: string;
  nuit?: string;
  organization_name?: string;
  organizational_unit_name?: string;
  state_or_province_name?: string;
}

interface VerificationApiItem {
  signature?: {
    validity?: {
      is_valid?: boolean;
      invalid_cause?: string;
    };
  };
  certificate?: {
    subject?: DistinguishedName;
    issuer?: DistinguishedName;
  };
}
```

## Limites de upload

Os limites estao alinhados entre frontend e backend:

| Camada | Limite |
| --- | --- |
| Frontend | `3 * 1024 * 1024` bytes |
| Backend `signer.maxFileSizeBytes` | `3145728` bytes |
| Backend `grails.controllers.upload.maxFileSize` | `3MB` |
| Backend `spring.servlet.multipart.max-file-size` | `3MB` |
| Backend `max-request-size` | `4MB` |

## Configuracao de OTP por email

O OTP tem validade de `120` segundos e, por defeito, usa 6 digitos.

| Chave | Descricao |
| --- | --- |
| `otp.ttlSeconds` | Validade do OTP em segundos. |
| `otp.digits` | Quantidade de digitos do codigo. |
| `otp.maxAttempts` | Numero maximo de tentativas por desafio. |
| `otp.hashSecret` | Segredo usado para guardar o hash do OTP. |
| `otp.email.enabled` | Activa envio real de email. Quando `false`, o codigo e escrito no log do backend. |
| `otp.email.host` | Servidor SMTP. |
| `otp.email.port` | Porta SMTP. |
| `otp.email.from` | Remetente do email. |
| `otp.email.username` | Utilizador SMTP, quando houver autenticacao. |
| `otp.email.password` | Palavra-passe SMTP, quando houver autenticacao. |
| `otp.email.startTls` | Activa STARTTLS. |
| `otp.email.ssl` | Usa conexao SMTP sobre SSL desde o inicio. |
| `otp.email.defaultDomain` | Dominio usado quando o username nao contem email. |

## Observacoes importantes para o frontend

- O frontend deve chamar apenas o backend deste projecto, usando `environment.apiBaseUrl`.
- O frontend nao deve chamar directamente `https://assinadoravancado.gov.mz`; essa integracao e responsabilidade do backend.
- O token so e guardado em `sessionStorage` com a chave `assinador_token` depois da confirmacao do OTP.
- O backend exige `Authorization` em assinatura e validacao, mas actualmente verifica apenas a presenca do header antes de encaminhar a requisicao.
- Em assinatura e validacao, usar sempre `responseType: 'blob'`, porque a resposta pode ser PDF ou JSON.
- Para `FormData`, nao definir manualmente `Content-Type`.
- O campo `attach_signed_document` e enviado pelo frontend, mas o backend actualmente encaminha `attach_signed_document` como `true` para o servico externo.
