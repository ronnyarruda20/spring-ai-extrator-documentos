package io.github.ronnyarruda20.extrator.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.ronnyarruda20.extrator.extracao.Extracao.Boleto;
import io.github.ronnyarruda20.extrator.extracao.Extracao.Item;
import io.github.ronnyarruda20.extrator.extracao.Extracao.NotaFiscal;
import io.github.ronnyarruda20.extrator.extracao.Extracao.Parte;
import io.github.ronnyarruda20.extrator.receita.ConsultaCnpj;
import io.github.ronnyarruda20.extrator.validacao.Verificacao.Status;

class ValidadoresTest {

    static final Parte EMITENTE = new Parte("Papelaria Exemplo Ltda", "90770487000184");
    static final Parte DESTINATARIO = new Parte("Empresa Exemplo Ltda", "90216739000128");
    static final Parte BENEFICIARIO = new Parte("Escola de Idiomas Exemplo Ltda", "90126225000181");

    /** Receita falsa: todo CNPJ existe, está ativo e pertence a quem diz o documento. */
    static final ConsultaCnpj RECEITA_OK = cnpj -> new ConsultaCnpj.Encontrado(
            cnpj.equals(EMITENTE.documento()) ? "PAPELARIA EXEMPLO LTDA" : "ESCOLA DE IDIOMAS EXEMPLO LTDA", "ATIVA");

    static NotaFiscal nota(BigDecimal total, List<Item> itens, String cnpjEmitente) {
        return new NotaFiscal(DigitosTest.CHAVE, "1234", "1", LocalDate.of(2026, 9, 15),
                new Parte("Papelaria Exemplo Ltda", cnpjEmitente), DESTINATARIO, itens, new BigDecimal("1007.50"),
                new BigDecimal("35.00"), new BigDecimal("42.50"), total);
    }

    static final List<Item> ITENS = List.of(
            new Item("Caderno", new BigDecimal("20"), new BigDecimal("24.90"), new BigDecimal("498.00")),
            new Item("Caneta", new BigDecimal("4"), new BigDecimal("45.50"), new BigDecimal("182.00")),
            new Item("Papel A4", new BigDecimal("10"), new BigDecimal("32.75"), new BigDecimal("327.50")));

    static Boleto boleto(String linha) {
        return new Boleto(linha, "001", "Banco do Brasil", BENEFICIARIO, DESTINATARIO, LocalDate.of(2026, 10, 10),
                new BigDecimal("189.00"), "00012345-6");
    }

    @Test
    void notaCoerenteFicaTodaOk() {
        var v = new ValidadorNotaFiscal(RECEITA_OK).validar(nota(new BigDecimal("1000.00"), ITENS, EMITENTE.documento()));

        assertThat(v).extracting(Verificacao::status).containsOnly(Status.OK);
    }

    @Test
    void totalImpressoErradoEPego() {
        var v = new ValidadorNotaFiscal(RECEITA_OK).validar(nota(new BigDecimal("1100.00"), ITENS, EMITENTE.documento()));

        assertThat(v).filteredOn(x -> x.status() == Status.ERRO).singleElement()
                .satisfies(x -> assertThat(x.mensagem()).contains("R$ 1.000,00").contains("R$ 1.100,00"));
    }

    @Test
    void cnpjDoEmitenteDiferenteDaChaveEPego() {
        var v = new ValidadorNotaFiscal(RECEITA_OK).validar(nota(new BigDecimal("1000.00"), ITENS, "90216739000128"));

        assertThat(v).filteredOn(x -> x.status() == Status.ERRO).extracting(Verificacao::mensagem)
                .anyMatch(m -> m.contains("não é o que está na chave"));
    }

    @Test
    void itemComContaErradaViraAlerta() {
        var itens = List.of(new Item("Caderno", new BigDecimal("20"), new BigDecimal("24.90"), new BigDecimal("500.00")));

        var v = new ValidadorNotaFiscal(RECEITA_OK).validar(nota(new BigDecimal("1000.00"), itens, EMITENTE.documento()));

        assertThat(v).filteredOn(x -> x.status() == Status.ALERTA).extracting(Verificacao::campo).contains("itens[0]");
    }

    @Test
    void receitaNaoEncontradaEmOutroNomeOuForaDoAr() {
        var nota = nota(new BigDecimal("1000.00"), ITENS, EMITENTE.documento());

        assertThat(new ValidadorNotaFiscal(c -> new ConsultaCnpj.NaoEncontrado()).validar(nota))
                .anyMatch(x -> x.status() == Status.ERRO && x.mensagem().contains("não consta na base da Receita"));
        assertThat(new ValidadorNotaFiscal(c -> new ConsultaCnpj.Encontrado("MERCADINHO QUALQUER SA", "ATIVA")).validar(nota))
                .anyMatch(x -> x.status() == Status.ALERTA && x.mensagem().contains("pertence a MERCADINHO QUALQUER SA"));
        assertThat(new ValidadorNotaFiscal(c -> new ConsultaCnpj.Indisponivel("fora do ar")).validar(nota))
                .anyMatch(x -> x.status() == Status.ALERTA && x.mensagem().contains("Não foi possível consultar"));
    }

    @Test
    void boletoCoerenteFicaTodoOk() {
        var v = new ValidadorBoleto(RECEITA_OK).validar(boleto(DigitosTest.LINHA));

        assertThat(v).extracting(Verificacao::status).containsOnly(Status.OK);
    }

    @Test
    void boletoAdulteradoGeraDoisErrosComAvisoDeGolpe() {
        var v = new ValidadorBoleto(RECEITA_OK).validar(boleto(DigitosTest.LINHA_ADULTERADA));

        assertThat(v).filteredOn(x -> x.status() == Status.ERRO).extracting(Verificacao::campo)
                .containsExactly("bancoImpresso", "valorDocumento");
        assertThat(v).filteredOn(x -> x.status() == Status.ERRO)
                .allSatisfy(x -> assertThat(x.mensagem()).contains("boleto adulterado"));
    }

    @Test
    void nomesParecidosIgnoramAcentosESufixos() {
        assertThat(Receita.parecidos("Papelaria Exemplo Ltda", "PAPELARIA EXEMPLO LTDA - ME")).isTrue();
        assertThat(Receita.parecidos("Escola de Idiomas Exemplo", "ESCOLA DE IDIOMAS EXEMPLO LTDA")).isTrue();
        assertThat(Receita.parecidos("Papelaria Exemplo Ltda", "Mercadinho Qualquer SA")).isFalse();
    }
}
