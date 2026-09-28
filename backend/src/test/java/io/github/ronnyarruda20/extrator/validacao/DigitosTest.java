package io.github.ronnyarruda20.extrator.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DigitosTest {

    /** Valores gerados por exemplos/gerar_exemplos.py (documentos fictícios). */
    static final String CHAVE = "51260990770487000184550010000012341123456789";
    static final String LINHA = "00190000090123456789701234567178315950000018900";
    static final String LINHA_ADULTERADA = "23793381286007000000901234567897115950000189000";

    @Test
    void cnpjNumericoEAlfanumerico() {
        assertThat(Digitos.cnpjValido("00.000.000/0001-91")).isTrue();
        assertThat(Digitos.cnpjValido("12.ABC.345/01DE-35")).as("exemplo oficial do CNPJ alfanumérico").isTrue();
        assertThat(Digitos.cnpjValido("00.000.000/0001-92")).isFalse();
        assertThat(Digitos.cnpjValido("11111111111111")).isFalse();
    }

    @Test
    void cpf() {
        assertThat(Digitos.cpfValido("529.982.247-25")).isTrue();
        assertThat(Digitos.cpfValido("529.982.247-26")).isFalse();
        assertThat(Digitos.cpfValido("111.111.111-11")).isFalse();
    }

    @Test
    void chaveDeAcessoDaNfe() {
        var chave = ChaveNfe.de(CHAVE);

        assertThat(chave.dvValido()).isTrue();
        assertThat(chave.anoMes()).isEqualTo("2609");
        assertThat(chave.cnpjEmitente()).isEqualTo("90770487000184");
        assertThat(chave.modelo()).isEqualTo("55");
        assertThat(chave.numero()).isEqualTo(1234);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 25, 43})
    void qualquerDigitoTrocadoNaChaveEPego(int posicao) {
        char[] c = CHAVE.toCharArray();
        c[posicao] = c[posicao] == '9' ? '0' : (char) (c[posicao] + 1);

        assertThat(ChaveNfe.de(new String(c)).dvValido()).isFalse();
    }

    @Test
    void linhaDigitavelDecodificaBancoValorEVencimento() {
        var l = LinhaDigitavel.de("00190.00009 01234.567897 01234.567178 3 15950000018900");

        assertThat(l.tamanhoValido()).isTrue();
        assertThat(l.camposComDvErrado()).isEmpty();
        assertThat(l.dvGeralValido()).isTrue();
        assertThat(l.banco()).isEqualTo("001");
        assertThat(l.valor()).isEqualByComparingTo(new BigDecimal("189.00"));
        assertThat(l.vencimentosPossiveis()).contains(LocalDate.of(2026, 10, 10));
    }

    @Test
    void fatorDeVencimentoConsideraORecomecoDe2025() {
        // Fator 1000 vale 03/07/2000 no ciclo antigo e 22/02/2025 no novo.
        var l = LinhaDigitavel.de(LINHA.substring(0, 33) + "1000" + "0000018900");

        assertThat(l.vencimentosPossiveis()).containsExactly(LocalDate.of(2000, 7, 3), LocalDate.of(2025, 2, 22));
    }

    @Test
    void digitoTrocadoNaLinhaEPegoNoCampoCerto() {
        String errada = LINHA.substring(0, 12) + (LINHA.charAt(12) == '9' ? '0' : (char) (LINHA.charAt(12) + 1)) + LINHA.substring(13);

        assertThat(LinhaDigitavel.de(errada).camposComDvErrado()).containsExactly(2);
    }

    @Test
    void boletoAdulteradoTemLinhaValidaMasDeOutroBancoEValor() {
        var l = LinhaDigitavel.de(LINHA_ADULTERADA);

        assertThat(l.camposComDvErrado()).isEmpty();
        assertThat(l.dvGeralValido()).as("o golpista gera uma linha válida").isTrue();
        assertThat(l.banco()).isEqualTo("237");
        assertThat(l.valor()).isEqualByComparingTo(new BigDecimal("1890.00"));
    }
}
