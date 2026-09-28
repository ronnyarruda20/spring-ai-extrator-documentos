package io.github.ronnyarruda20.extrator.extracao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * O que o modelo devolve. As descrições viram parte do esquema JSON que o Spring AI manda
 * para o modelo, então funcionam como instrução campo a campo.
 */
public record Extracao(
        @JsonPropertyDescription("NOTA_FISCAL para DANFE/NF-e, BOLETO para boleto bancário, OUTRO para o resto")
        Tipo tipo,
        NotaFiscal notaFiscal,
        Boleto boleto,
        @JsonPropertyDescription("Algo ilegível ou ambíguo no documento; vazio se não houver")
        List<String> observacoes) {

    public enum Tipo { NOTA_FISCAL, BOLETO, OUTRO }

    public record Parte(
            @JsonPropertyDescription("Nome ou razão social") String nome,
            @JsonPropertyDescription("CNPJ ou CPF, só os caracteres, sem pontuação") String documento) {
    }

    public record Item(String descricao, BigDecimal quantidade,
            @JsonPropertyDescription("Valor unitário em reais, número com ponto decimal") BigDecimal valorUnitario,
            @JsonPropertyDescription("Valor total do item, como impresso") BigDecimal valorTotal) {
    }

    public record NotaFiscal(
            @JsonPropertyDescription("Chave de acesso com os 44 dígitos, sem espaços") String chaveAcesso,
            String numero,
            String serie,
            @JsonPropertyDescription("Data de emissão AAAA-MM-DD") LocalDate dataEmissao,
            Parte emitente,
            Parte destinatario,
            List<Item> itens,
            @JsonPropertyDescription("Valor total dos produtos, como impresso") BigDecimal valorProdutos,
            BigDecimal valorFrete,
            BigDecimal valorDesconto,
            @JsonPropertyDescription("Valor total da nota, como impresso") BigDecimal valorTotal) {
    }

    public record Boleto(
            @JsonPropertyDescription("Linha digitável com os 47 dígitos, sem pontos nem espaços") String linhaDigitavel,
            @JsonPropertyDescription("Código do banco impresso no cabeçalho, ao lado do nome do banco, 3 dígitos")
            String bancoImpresso,
            @JsonPropertyDescription("Nome do banco impresso no cabeçalho") String nomeBancoImpresso,
            Parte beneficiario,
            Parte pagador,
            @JsonPropertyDescription("Vencimento impresso, AAAA-MM-DD") LocalDate vencimento,
            @JsonPropertyDescription("Valor do documento impresso, em reais") BigDecimal valorDocumento,
            String nossoNumero) {
    }
}
