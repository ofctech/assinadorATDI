package seguranca

class OperacaoEstatistica {

    String tipo
    Long total = 0L
    Date dateCreated
    Date lastUpdated

    static constraints = {
        tipo nullable: false, blank: false, unique: true, inList: ['signatures', 'validations']
        total nullable: false, min: 0L
    }

    static mapping = {
        table 'operacao_estatistica'
        tipo column: 'tipo'
        total column: 'total'
    }
}
