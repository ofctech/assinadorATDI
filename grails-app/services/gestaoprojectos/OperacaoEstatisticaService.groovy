package gestaoprojectos

import grails.gorm.transactions.Transactional
import seguranca.OperacaoEstatistica

@Transactional
class OperacaoEstatisticaService {

    Map resumo() {
        [
                signatures : totalPorTipo('signatures'),
                validations: totalPorTipo('validations')
        ]
    }

    synchronized Map incrementar(String tipo) {
        validarTipo(tipo)

        OperacaoEstatistica estatistica = OperacaoEstatistica.findByTipo(tipo)
        if (!estatistica) {
            estatistica = new OperacaoEstatistica(tipo: tipo, total: 0L)
        }

        estatistica.total = (estatistica.total ?: 0L) + 1L
        estatistica.save(failOnError: true, flush: true)

        resumo()
    }

    private Long totalPorTipo(String tipo) {
        OperacaoEstatistica.findByTipo(tipo)?.total ?: 0L
    }

    private void validarTipo(String tipo) {
        if (!(tipo in ['signatures', 'validations'])) {
            throw new IllegalArgumentException('invalid_metric_type')
        }
    }
}
