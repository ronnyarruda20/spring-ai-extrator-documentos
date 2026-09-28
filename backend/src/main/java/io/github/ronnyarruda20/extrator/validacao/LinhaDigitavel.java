package io.github.ronnyarruda20.extrator.validacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Linha digitável de boleto bancário (47 dígitos, padrão FEBRABAN).
 *
 * <p>Ela carrega o banco, o vencimento e o valor. É por isso que dá para pegar o golpe do
 * boleto adulterado: o golpista troca a linha para receber o dinheiro, mas o cabeçalho e o
 * valor impressos continuam os do boleto original.
 *
 * <pre>
 * campo 1: banco(3) moeda(1) campo livre[0,5)  DV   → 10
 * campo 2: campo livre[5,15)                    DV   → 11
 * campo 3: campo livre[15,25)                   DV   → 11
 * campo 4: DV geral do código de barras               →  1
 * campo 5: fator de vencimento(4) valor(10)           → 14
 * </pre>
 */
public record LinhaDigitavel(String digitos) {

    /** Base do fator de vencimento. O fator chegou a 9999 em 21/02/2025 e recomeçou em 1000 no dia seguinte. */
    static final LocalDate BASE_1997 = LocalDate.of(1997, 10, 7);
    static final LocalDate BASE_2025 = LocalDate.of(2025, 2, 22);

    public static LinhaDigitavel de(String entrada) {
        return new LinhaDigitavel(Digitos.somenteDigitos(entrada));
    }

    public boolean tamanhoValido() {
        return digitos.length() == 47;
    }

    /** Campos 1 a 3 cujo dígito (módulo 10) não confere. */
    public List<Integer> camposComDvErrado() {
        List<Integer> errados = new ArrayList<>();
        int[][] campos = {{0, 9}, {10, 20}, {21, 31}};
        for (int i = 0; i < campos.length; i++) {
            String corpo = digitos.substring(campos[i][0], campos[i][1]);
            int dv = digitos.charAt(campos[i][1]) - '0';
            if (Digitos.mod10(corpo) != dv) {
                errados.add(i + 1);
            }
        }
        return errados;
    }

    /** Código de barras (44 dígitos) reconstruído a partir da linha. */
    public String codigoDeBarras() {
        String campoLivre = digitos.substring(4, 9) + digitos.substring(10, 20) + digitos.substring(21, 31);
        return digitos.substring(0, 4) + digitos.charAt(32) + digitos.substring(33, 47) + campoLivre;
    }

    public boolean dvGeralValido() {
        String cb = codigoDeBarras();
        String semDv = cb.substring(0, 4) + cb.substring(5);
        return Digitos.mod11Boleto(semDv) == cb.charAt(4) - '0';
    }

    public String banco() {
        return digitos.substring(0, 3);
    }

    public int fatorVencimento() {
        return Integer.parseInt(digitos.substring(33, 37));
    }

    /** Valor em reais; zero quando o boleto não informa valor na linha. */
    public BigDecimal valor() {
        return new BigDecimal(digitos.substring(37, 47)).movePointLeft(2);
    }

    /** O mesmo fator vale para duas datas (ciclo de 1997 e ciclo de 2025); devolve as duas. */
    public List<LocalDate> vencimentosPossiveis() {
        int fator = fatorVencimento();
        if (fator == 0) {
            return List.of();
        }
        List<LocalDate> datas = new ArrayList<>();
        datas.add(BASE_1997.plusDays(fator));
        if (fator >= 1000) {
            datas.add(BASE_2025.plusDays(fator - 1000));
        }
        return datas;
    }
}
