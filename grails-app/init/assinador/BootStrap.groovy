package assinador

import grails.core.GrailsApplication
import seguranca.Grupo
import seguranca.Utilizador
import seguranca.UtilizadorGrupo

class BootStrap {

    GrailsApplication grailsApplication
    def springSecurityService

    def init = { servletContext ->
        Utilizador.withTransaction {
            createDefaultAdministrator()
        }
    }

    def destroy = {
    }

    private void createDefaultAdministrator() {
        Grupo adminGroup = Grupo.findByAuthority('ROLE_ADMIN')
        if (!adminGroup) {
            adminGroup = new Grupo(
                    authority: 'ROLE_ADMIN',
                    descricao: 'Administradores'
            ).save(flush: true, failOnError: true)
        }

        String username = grailsApplication.config.getProperty(
                'security.bootstrap.admin.username',
                String,
                'admin@atdi.gov.mz'
        )
        String password = grailsApplication.config.getProperty(
                'security.bootstrap.admin.password',
                String,
                null
        )
        String name = grailsApplication.config.getProperty(
                'security.bootstrap.admin.name',
                String,
                'Administrador'
        )

        if (!password) {
            log.warn('SIGNER_ADMIN_PASSWORD is not configured; default administrator creation was skipped')
            return
        }

        Utilizador admin = Utilizador.findByUsername(username)
        if (!admin) {
            admin = new Utilizador(
                    username: username,
                    password: springSecurityService.encodePassword(password),
                    nome: name,
                    enabled: true,
                    grupoPrincipal: adminGroup
            ).save(flush: true, failOnError: true)
        } else if (!admin.grupoPrincipal) {
            admin.grupoPrincipal = adminGroup
            admin.save(flush: true, failOnError: true)
        }

        if (!UtilizadorGrupo.exists(admin.id, adminGroup.id)) {
            new UtilizadorGrupo(
                    utilizador: admin,
                    grupo: adminGroup
            ).save(flush: true, failOnError: true)
        }
    }
}
