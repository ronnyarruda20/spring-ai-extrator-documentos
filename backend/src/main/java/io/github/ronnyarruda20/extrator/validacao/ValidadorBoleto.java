package io.github.ronnyarruda20.extrator.validacao;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.ronnyarruda20.extrator.extracao.Extracao.Boleto;
import io.github.ronnyarruda20.extrator.receita.ConsultaCnpj;

/**
 * Regras do boleto. As mais importantes comparam o que está embutido na linha digitável
 * (banco, valor, vencimento) com o que está impresso: divergência é o sinal clássico do
 * golpe do boleto adulterado.
 */
public class ValidadorBoleto {

    static final Map<String, String> BANCOS = Map.ofEntries(
            Map.entry("001", "Banco do Brasil"), Map.entry("033", "Santander"), Map.entry("077", "Inter"),
            Map.entry("104", "Caixa Econômica Federal"), Map.entry("208", "BTG Pactual"), Map.entry("237", "Bradesco"),
            Map.entry("260", "Nubank"), Map.entry("336", "C6 Bank"), Map.entry("341", "Itaú"),
            Map.entry("422", "Safra"), Map.entry("748", "Sicredi"), Map.entry("756", "Sicoob"));
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String GOLPE = " Sinal de boleto adulterado: não pague sem confirmar com quem emitiu.";

    private final ConsultaCnpj receita;

    public ValidadorBoleto(ConsultaCnpj receita) {
        this.receita = receita;
    }

    public List<Verificacao> validar(Boleto b) {
        List<Verificacao> v = new ArrayList<>();
        LinhaDigitavel linha = LinhaDigitavel.de(b.linhaDigitavel());
        if (!linha.tamanhoValido()) {
            v.add(Verificacao.erro("linhaDigitavel", "A linha digitável não tem 47 dígitos."));
        } else {
            List<Integer> errados = linha.camposComDvErrado();
            ValidadorNotaFiscal.conferir(v, "linhaDigitavel", errados.isEmpty() && linha.dvGeralValido(),
                    "Dígitos verificadores da linha digitável conferem.",
                    errados.isEmpty() ? "O dígito verificador geral da linha digitável não confere."
                            : "O dígito verificador do campo " + errados + " da linha digitável não confere.");
            banco(v, b, linha);
            valor(v, b, linha);
            vencimento(v, b, linha);
        }
        beneficiario(v, b);
        return v;
    }

    private void banco(List<Verificacao> v, Boleto b, LinhaDigitavel linha) {
        String impresso = Digitos.somenteDigitos(b.bancoImpresso());
        if (impresso.isEmpty()) {
            v.add(Verificacao.alerta("bancoImpresso", "Não encontrei o código do banco no cabeçalho do boleto."));
            return;
        }
        String nomeLinha = BANCOS.getOrDefault(linha.banco(), "banco " + linha.banco());
        ValidadorNotaFiscal.conferir(v, "bancoImpresso", impresso.equals(linha.banco()),
                "O banco da linha digitável (" + nomeLinha + ") é o mesmo do cabeçalho.",
                "A linha digitável é do " + nomeLinha + " (" + linha.banco() + "), mas o cabeçalho diz "
                        + Objects.requireNonNullElse(b.nomeBancoImpresso(), impresso) + " (" + impresso + ")." + GOLPE);
    }

    private void valor(List<Verificacao> v, Boleto b, LinhaDigitavel linha) {
        if (linha.valor().signum() == 0) {
            v.add(Verificacao.alerta("valorDocumento", "A linha digitável não traz valor; confira o valor antes de pagar."));
            return;
        }
        if (b.valorDocumento() == null) {
            v.add(Verificacao.alerta("valorDocumento", "Não encontrei o valor impresso do documento."));
            return;
        }
        ValidadorNotaFiscal.conferir(v, "valorDocumento", ValidadorNotaFiscal.igual(linha.valor(), b.valorDocumento()),
                "O valor da linha digitável é o mesmo do documento (R$ " + formatar(b.valorDocumento()) + ").",
                "A linha digitável cobra R$ " + formatar(linha.valor()) + ", mas o valor impresso do documento é R$ "
                        + formatar(b.valorDocumento()) + "." + GOLPE);
    }

    private void vencimento(List<Verificacao> v, Boleto b, LinhaDigitavel linha) {
        var possiveis = linha.vencimentosPossiveis();
        if (possiveis.isEmpty() || b.vencimento() == null) {
            v.add(Verificacao.alerta("vencimento", "Não foi possível conferir o vencimento pela linha digitável."));
            return;
        }
        if (possiveis.contains(b.vencimento())) {
            v.add(Verificacao.ok("vencimento", "O vencimento impresso bate com o da linha digitável."));
        } else {
            v.add(Verificacao.alerta("vencimento", "O vencimento impresso (" + DATA.format(b.vencimento())
                    + ") não bate com o da linha digitável (" + DATA.format(possiveis.getLast()) + ")."));
        }
    }

    private void beneficiario(List<Verificacao> v, Boleto b) {
        if (b.beneficiario() == null || b.beneficiario().documento() == null) {
            v.add(Verificacao.alerta("beneficiario", "Documento do beneficiário não encontrado."));
            return;
        }
        String d = b.beneficiario().documento().replaceAll("[.\\-/\\s]", "");
        if (d.length() == 11) {
            ValidadorNotaFiscal.conferir(v, "beneficiario", Digitos.cpfValido(d), "CPF do beneficiário válido.",
                    "O CPF do beneficiário tem dígito verificador inválido.");
            return;
        }
        if (!Digitos.cnpjValido(d)) {
            v.add(Verificacao.erro("beneficiario", "O CNPJ do beneficiário tem dígito verificador inválido."));
            return;
        }
        v.add(Verificacao.ok("beneficiario", "CNPJ do beneficiário com dígito verificador válido."));
        Receita.avaliar(v, "beneficiario", b.beneficiario(), receita.consultar(d));
    }

    static String formatar(BigDecimal valor) {
        return String.format(java.util.Locale.of("pt", "BR"), "%,.2f", valor);
    }
}
