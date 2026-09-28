package io.github.ronnyarruda20.extrator.validacao;

/**
 * Resultado de uma regra aplicada ao que o modelo extraiu.
 *
 * @param campo    o que foi verificado, para a tela destacar
 * @param status   OK, ALERTA (conferir manualmente) ou ERRO (inconsistente)
 * @param mensagem explicação em português
 */
public record Verificacao(String campo, Status status, String mensagem) {

    public enum Status { OK, ALERTA, ERRO }

    public static Verificacao ok(String campo, String mensagem) {
        return new Verificacao(campo, Status.OK, mensagem);
    }

    public static Verificacao alerta(String campo, String mensagem) {
        return new Verificacao(campo, Status.ALERTA, mensagem);
    }

    public static Verificacao erro(String campo, String mensagem) {
        return new Verificacao(campo, Status.ERRO, mensagem);
    }
}
