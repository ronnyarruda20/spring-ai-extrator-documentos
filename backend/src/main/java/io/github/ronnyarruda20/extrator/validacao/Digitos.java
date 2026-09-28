package io.github.ronnyarruda20.extrator.validacao;

import java.util.Locale;

/**
 * Dígitos verificadores dos documentos brasileiros. Tudo aqui é aritmética pura:
 * se o modelo leu um dígito errado, ou se alguém adulterou o número, a conta não fecha.
 */
public final class Digitos {

    private static final int[] PESOS_CNPJ_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Digitos() {
    }

    public static String somenteDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /** Aceita o CNPJ alfanumérico (a partir de julho de 2026): letras valem o código ASCII menos 48. */
    public static boolean cnpjValido(String entrada) {
        String c = entrada == null ? "" : entrada.replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);
        if (!c.matches("[0-9A-Z]{12}\\d{2}") || c.chars().distinct().count() == 1) {
            return false;
        }
        char dv1 = dvCnpj(c.substring(0, 12), PESOS_CNPJ_1);
        char dv2 = dvCnpj(c.substring(0, 12) + dv1, PESOS_CNPJ_2);
        return c.charAt(12) == dv1 && c.charAt(13) == dv2;
    }

    private static char dvCnpj(String s, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (s.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return (char) ('0' + (resto < 2 ? 0 : 11 - resto));
    }

    public static boolean cpfValido(String entrada) {
        String c = somenteDigitos(entrada);
        if (c.length() != 11 || c.chars().distinct().count() == 1) {
            return false;
        }
        for (int n = 9; n <= 10; n++) {
            int soma = 0;
            for (int i = 0; i < n; i++) {
                soma += (c.charAt(i) - '0') * (n + 1 - i);
            }
            int dv = (soma * 10) % 11 % 10;
            if (dv != c.charAt(n) - '0') {
                return false;
            }
        }
        return true;
    }

    /** Módulo 11 da chave da NF-e: pesos 2 a 9 da direita para a esquerda; resto 0 ou 1 dá dígito 0. */
    public static int mod11Nfe(String numero) {
        int soma = 0;
        int peso = 2;
        for (int i = numero.length() - 1; i >= 0; i--) {
            soma += (numero.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Módulo 10 dos campos da linha digitável (FEBRABAN). */
    public static int mod10(String numero) {
        int soma = 0;
        int peso = 2;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int p = (numero.charAt(i) - '0') * peso;
            soma += p / 10 + p % 10;
            peso = peso == 2 ? 1 : 2;
        }
        return (10 - soma % 10) % 10;
    }

    /** Dígito geral do código de barras do boleto: módulo 11, e 0, 10 ou 11 viram 1. */
    public static int mod11Boleto(String numero43) {
        int soma = 0;
        int peso = 2;
        for (int i = numero43.length() - 1; i >= 0; i--) {
            soma += (numero43.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int dv = 11 - soma % 11;
        return dv == 0 || dv == 10 || dv == 11 ? 1 : dv;
    }
}
