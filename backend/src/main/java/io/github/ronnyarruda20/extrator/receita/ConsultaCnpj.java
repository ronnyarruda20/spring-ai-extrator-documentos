package io.github.ronnyarruda20.extrator.receita;

/** Consulta do CNPJ na base da Receita Federal. */
public interface ConsultaCnpj {

    Situacao consultar(String cnpj);

    sealed interface Situacao {
    }

    record Encontrado(String razaoSocial, String situacaoCadastral) implements Situacao {
    }

    record NaoEncontrado() implements Situacao {
    }

    /** Não deu para consultar agora (servidor fora do ar, sem configuração...). */
    record Indisponivel(String motivo) implements Situacao {
    }
}
