package seguranca

import grails.compiler.GrailsCompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@GrailsCompileStatic
@EqualsAndHashCode(includes='username')
@ToString(includes='username', includeNames=true, includePackage=false)
class Utilizador implements Serializable {

    private static final long serialVersionUID = 1

    String username
    String password
    String email
    boolean enabled = true
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired
    String nome
    String idSiga //Id do estudante no siga
    Grupo grupoPrincipal
    String numEstudante
    String celular

    Set<Grupo> getAuthorities() {
        (UtilizadorGrupo.findAllByUtilizador(this) as List<UtilizadorGrupo>)*.grupo as Set<Grupo>
    }

    static constraints = {
        password nullable: false, blank: false, password: true, display: false
        username nullable: false, blank: false, unique: true, email: true
        nome nullable: false
        email nullable: true
        idSiga nullable: true, display : false
        grupoPrincipal nullable: true
        numEstudante nullable: true
        celular nullable: true
    }

    static mapping = {
	    password column: '`password`'
    }

    String toString(){
        return nome;
    }
}
