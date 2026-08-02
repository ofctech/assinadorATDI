package assinador

class ApplicationController {
    static responseFormats = ['json']

    def index() {
        respond([application: 'assinador', status: 'UP'])
    }
}
