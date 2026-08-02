package gestaoprojectos

import grails.core.GrailsApplication
import groovy.transform.CompileStatic

import javax.naming.Context
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext

@CompileStatic
class LdapAuthService {

    GrailsApplication grailsApplication

    boolean authenticate(String username, String password) {
        if (!username || !password) return false

        String ldapUrl = grailsApplication.config.getProperty('ldap.url', String)
        String domainPrefix = grailsApplication.config.getProperty('ldap.domainPrefix', String, "")
        Integer connectTimeout = grailsApplication.config.getProperty('ldap.connectTimeoutMs', Integer, 3000)
        Integer readTimeout = grailsApplication.config.getProperty('ldap.readTimeoutMs', Integer, 3000)

        if (!ldapUrl) throw new IllegalStateException("Config obrigatória: ldap.url")

        String principalName = domainPrefix + username


        Hashtable<String, Object> env = new Hashtable<>()
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        env.put(Context.PROVIDER_URL, ldapUrl)
        env.put(Context.SECURITY_AUTHENTICATION, "simple")
        env.put(Context.SECURITY_PRINCIPAL, principalName)
        env.put(Context.SECURITY_CREDENTIALS, password)

        // timeouts JNDI (propriedades standard)
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(connectTimeout))
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(readTimeout))

        DirContext ctx = null
        try {
            ctx = new InitialDirContext(env)
            return true
        } catch (Exception e) {
            return false
        } finally {
            try { ctx?.close() } catch (Exception ignore) {}
        }
    }
}
