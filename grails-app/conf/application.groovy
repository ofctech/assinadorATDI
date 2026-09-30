grails.gorm.default.mapping = {
    id generator: 'org.hibernate.id.enhanced.SequenceStyleGenerator', params: [prefer_sequence_per_entity: true]
}

grails.controllers.upload.maxFileSize = 3 * 1024 * 1024
grails.controllers.upload.maxRequestSize = 4 * 1024 * 1024
spring.servlet.multipart.maxFileSize = '3MB'
spring.servlet.multipart.maxRequestSize = '4MB'

grails.plugin.springsecurity.userLookup.userDomainClassName = 'seguranca.Utilizador'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'seguranca.UtilizadorGrupo'
grails.plugin.springsecurity.authority.className = 'seguranca.Grupo'
grails.plugin.springsecurity.securityConfigType = 'InterceptUrlMap'
grails.plugin.springsecurity.interceptUrlMap = [
        [pattern: '/',                         access: ['permitAll']],
        [pattern: '/error',                    access: ['permitAll']],
        [pattern: '/assets/**',                access: ['permitAll']],
        [pattern: '/**/assets/**',             access: ['permitAll']],
        [pattern: '/**/favicon.ico',           access: ['permitAll']],
        [pattern: '/api/auth/token',            access: ['ROLE_ADMIN']],
        [pattern: '/api/auth/**',              access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
        [pattern: '/api/signer/**',            access: ['ROLE_ADMIN']],
        [pattern: '/api/statistics/**',        access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
        [pattern: '/api/ldapLogin/**',         access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
        [pattern: '/api/jwks/**',              access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
        [pattern: '/api/.well-known/**',       access: ['IS_AUTHENTICATED_ANONYMOUSLY']],
        [pattern: '/**',                       access: ['isFullyAuthenticated()']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
        [
                pattern: '/**',
                filters: 'JOINED_FILTERS,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-rememberMeAuthenticationFilter'
        ],
        [
                pattern: '/stateful/**',
                filters: 'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter'
        ]
]

grails.plugin.springsecurity.rest.logout.endpointUrl = '/api/logout'
grails.plugin.springsecurity.rest.token.validation.useBearerToken = false
grails.plugin.springsecurity.rest.token.validation.headerName = 'X-Auth-Token'
grails.plugin.springsecurity.rest.token.storage.useGorm = true
grails.plugin.springsecurity.rest.token.storage.gorm.tokenDomainClassName = 'seguranca.AuthenticationToken'
grails.plugin.springsecurity.rest.token.storage.gorm.tokenValuePropertyName = 'tokenValue'
grails.plugin.springsecurity.rest.token.storage.gorm.usernamePropertyName = 'username'
grails.plugin.springsecurity.rest.token.generation.useSecureRandom = true
grails.plugin.springsecurity.rest.token.generation.useUUID = false
grails.plugin.springsecurity.rest.login.active = true
grails.plugin.springsecurity.rest.login.endpointUrl = '/api/login'
grails.plugin.springsecurity.rest.login.failureStatusCode = 401
grails.plugin.springsecurity.rest.login.useJsonCredentials = true
grails.plugin.springsecurity.rest.login.usernamePropertyName = 'username'
grails.plugin.springsecurity.rest.login.passwordPropertyName = 'password'
grails.plugin.springsecurity.rejectIfNoRule = true
grails.plugin.springsecurity.fii.rejectPublicInvocations = false
grails.plugin.springsecurity.password.algorithm = 'bcrypt'

security.bootstrap.admin.username = System.getenv('SIGNER_ADMIN_USERNAME') ?: 'admin@atdi.gov.mz'
security.bootstrap.admin.password = System.getenv('SIGNER_ADMIN_PASSWORD')
security.bootstrap.admin.name = System.getenv('SIGNER_ADMIN_NAME') ?: 'Administrador'
