package seguranca

import grails.compiler.GrailsCompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@GrailsCompileStatic
@EqualsAndHashCode(includes='authority')
@ToString(includes='authority', includeNames=true, includePackage=false)
class Grupo implements Serializable {

	private static final long serialVersionUID = 1

	String authority
	String descricao
	Utilizador responsavel

	static constraints = {
		authority nullable: false, blank: false, unique: true
		responsavel nullable: true
		descricao nullable: true
	}

	static mapping = {
		cache true
	}
}
