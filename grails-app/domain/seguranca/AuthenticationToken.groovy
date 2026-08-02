package seguranca

class AuthenticationToken implements Serializable {

    private static final long serialVersionUID = 20140401

    String tokenValue
    String username
    Date dateCreated
    Date lastUpdated

    Date refreshed = new Date()

    static mapping = {
        tokenValue unique: true, index: 'token_idx'
        username length: 64
        version false
    }


    boolean equals(other) {
        if (!( other instanceof AuthenticationToken )) {
            return false
        }

        other.tokenValue == tokenValue && other.username == username
    }

}
