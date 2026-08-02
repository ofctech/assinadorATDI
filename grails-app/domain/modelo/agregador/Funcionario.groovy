package modelo.agregador

class Funcionario {

    Integer numero
    String nome
    String email
    String dataNascimento
    Long nuit
    String celular

    static constraints = {
        numero nullable: true
        nome nullable: true
        email nullable: true
        dataNascimento nullable: true
        nuit nullable: true
        celular nullable: true
    }
}
