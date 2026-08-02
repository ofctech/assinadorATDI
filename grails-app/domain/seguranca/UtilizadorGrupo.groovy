package seguranca

import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria
import groovy.transform.ToString
import org.codehaus.groovy.util.HashCodeHelper

@GrailsCompileStatic
@ToString(cache=true, includeNames=true, includePackage=false)
class UtilizadorGrupo implements Serializable {

	private static final long serialVersionUID = 1

    Utilizador utilizador
    Grupo grupo

	@Override
	boolean equals(other) {
		if (other instanceof UtilizadorGrupo) {
			other.utilizadorId == utilizador?.id && other.grupoId == grupo?.id
		}
	}

    @Override
	int hashCode() {
	    int hashCode = HashCodeHelper.initHash()
        if (utilizador) {
            hashCode = HashCodeHelper.updateHash(hashCode, utilizador.id)
		}
		if (grupo) {
		    hashCode = HashCodeHelper.updateHash(hashCode, grupo.id)
		}
		hashCode
	}

	static UtilizadorGrupo get(long utilizadorId, long grupoId) {
		criteriaFor(utilizadorId, grupoId).get()
	}

	static boolean exists(long utilizadorId, long grupoId) {
		criteriaFor(utilizadorId, grupoId).count()
	}

	private static DetachedCriteria criteriaFor(long utilizadorId, long grupoId) {
		UtilizadorGrupo.where {
			utilizador == Utilizador.load(utilizadorId) &&
			grupo == Grupo.load(grupoId)
		}
	}

	static UtilizadorGrupo create(Utilizador utilizador, Grupo grupo, boolean flush = false) {
		def instance = new UtilizadorGrupo(utilizador: utilizador, grupo: grupo)
		instance.save(flush: flush)
		instance
	}

	static boolean remove(Utilizador u, Grupo r) {
		if (u != null && r != null) {
			UtilizadorGrupo.where { utilizador == u && grupo == r }.deleteAll()
		}
	}

	static int removeAll(Utilizador u) {
		u == null ? 0 : UtilizadorGrupo.where { utilizador == u }.deleteAll() as int
	}

	static int removeAll(Grupo r) {
		r == null ? 0 : UtilizadorGrupo.where { grupo == r }.deleteAll() as int
	}

	static constraints = {
	    utilizador nullable: false
		grupo nullable: false, validator: { Grupo r, UtilizadorGrupo ur ->
			if (ur.utilizador?.id) {
				if (UtilizadorGrupo.exists(ur.utilizador.id, r.id)) {
				    return ['userRole.exists']
				}
			}
		}
	}

	static mapping = {
		id composite: ['utilizador', 'grupo']
		version false
	}
}
