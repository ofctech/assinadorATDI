package modelo.agregador

import grails.converters.JSON

class StatisticsController {

    def operacaoEstatisticaService

    def summary() {
        render(operacaoEstatisticaService.resumo() as JSON)
    }

    def increment() {
        String tipo = request.JSON?.type ?: params.type

        try {
            render(operacaoEstatisticaService.incrementar(tipo) as JSON)
        } catch (IllegalArgumentException ignored) {
            response.status = 400
            render([success: false, error: 'invalid_metric_type'] as JSON)
        }
    }
}
