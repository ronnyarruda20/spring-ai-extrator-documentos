package io.github.ronnyarruda20.extrator.validacao;

/**
 * Chave de acesso da NF-e (44 dígitos). Ela embute UF, ano e mês de emissão, CNPJ do
 * emitente, modelo, série e número; por isso dá para conferir cada um com o que está impresso.
 *
 * <pre>
 * cUF(2) AAMM(4) CNPJ(14) modelo(2) série(3) número(9) tpEmis(1) código(8) DV(1)
 * </pre>
 */
public record ChaveNfe(String digitos) {

    public static ChaveNfe de(String entrada) {
        return new ChaveNfe(Digitos.somenteDigitos(entrada));
    }

    public boolean tamanhoValido() {
        return digitos.length() == 44;
    }

    public boolean dvValido() {
        return Digitos.mod11Nfe(digitos.substring(0, 43)) == digitos.charAt(43) - '0';
    }

    public String anoMes() {
        return digitos.substring(2, 6);
    }

    public String cnpjEmitente() {
        return digitos.substring(6, 20);
    }

    public String modelo() {
        return digitos.substring(20, 22);
    }

    public int serie() {
        return Integer.parseInt(digitos.substring(22, 25));
    }

    public long numero() {
        return Long.parseLong(digitos.substring(25, 34));
    }
}
