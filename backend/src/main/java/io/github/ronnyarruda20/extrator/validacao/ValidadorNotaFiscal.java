package io.github.ronnyarruda20.extrator.validacao;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.ronnyarruda20.extrator.extracao.Extracao.Item;
import io.github.ronnyarruda20.extrator.extracao.Extracao.NotaFiscal;
import io.github.ronnyarruda20.extrator.extracao.Extracao.Parte;
import io.github.ronnyarruda20.extrator.receita.ConsultaCnpj;

/** Regras da NF-e: chave de acesso, coerência entre chave e campos impressos, documentos e contas. */
public class ValidadorNotaFiscal {

    private final ConsultaCnpj receita;

    public ValidadorNotaFiscal(ConsultaCnpj receita) {
        this.receita = receita;
    }

    public List<Verificacao> validar(NotaFiscal nf) {
        List<Verificacao> v = new ArrayList<>();
        String cnpjEmitente = nf.emitente() == null ? "" : Digitos.somenteDigitos(nf.emitente().documento());

        ChaveNfe chave = ChaveNfe.de(nf.chaveAcesso());
        if (!chave.tamanhoValido()) {
            v.add(Verificacao.erro("chaveAcesso", "A chave de acesso não tem 44 dígitos."));
        } else if (!chave.dvValido()) {
            v.add(Verificacao.erro("chaveAcesso",
                    "O dígito verificador da chave de acesso não confere: a chave foi lida ou digitada errada, ou foi alterada."));
        } else {
            v.add(Verificacao.ok("chaveAcesso", "Chave de acesso com dígito verificador válido."));
            conferir(v, "chaveAcesso", "55".equals(chave.modelo()), "Modelo 55 (NF-e).",
                    "A chave indica modelo " + chave.modelo() + ", não 55 (NF-e).");
            conferir(v, "emitente", chave.cnpjEmitente().equals(cnpjEmitente),
                    "O CNPJ do emitente é o mesmo que está na chave.",
                    "O CNPJ do emitente impresso não é o que está na chave de acesso.");
            conferir(v, "numero", numeroIgual(nf.numero(), chave.numero()),
                    "O número da nota é o mesmo que está na chave.",
                    "O número impresso não é o que está na chave (" + chave.numero() + ").");
            if (nf.dataEmissao() != null) {
                String aamm = nf.dataEmissao().format(DateTimeFormatter.ofPattern("yyMM"));
                conferir(v, "dataEmissao", aamm.equals(chave.anoMes()),
                        "O mês de emissão bate com a chave.",
                        "A data de emissão não bate com o ano e mês da chave (" + chave.anoMes() + ").");
            }
        }

        documento(v, "emitente", nf.emitente());
        documento(v, "destinatario", nf.destinatario());
        contas(v, nf);
        receita(v, nf.emitente());
        return v;
    }

    private void contas(List<Verificacao> v, NotaFiscal nf) {
        List<Item> itens = nf.itens() == null ? List.of() : nf.itens();
        for (int i = 0; i < itens.size(); i++) {
            Item it = itens.get(i);
            if (it.quantidade() != null && it.valorUnitario() != null && it.valorTotal() != null
                    && !igual(it.quantidade().multiply(it.valorUnitario()), it.valorTotal())) {
                v.add(Verificacao.alerta("itens[" + i + "]", "Quantidade vezes valor unitário não dá o total do item \""
                        + it.descricao() + "\"."));
            }
        }
        BigDecimal somaItens = itens.stream().map(Item::valorTotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (nf.valorProdutos() != null && !itens.isEmpty()) {
            conferir(v, "valorProdutos", igual(somaItens, nf.valorProdutos()),
                    "A soma dos itens bate com o total dos produtos.",
                    "A soma dos itens (R$ " + ValidadorBoleto.formatar(somaItens) + ") não bate com o total dos produtos (R$ "
                            + ValidadorBoleto.formatar(nf.valorProdutos()) + ").");
        }
        if (nf.valorTotal() != null && nf.valorProdutos() != null) {
            BigDecimal esperado = nf.valorProdutos().add(zero(nf.valorFrete())).subtract(zero(nf.valorDesconto()));
            conferir(v, "valorTotal", igual(esperado, nf.valorTotal()),
                    "Produtos mais frete menos desconto dá o total da nota.",
                    "Produtos mais frete menos desconto dá R$ " + ValidadorBoleto.formatar(esperado)
                            + ", mas o total impresso é R$ " + ValidadorBoleto.formatar(nf.valorTotal()) + ".");
        }
    }

    private void documento(List<Verificacao> v, String campo, Parte parte) {
        if (parte == null || parte.documento() == null || parte.documento().isBlank()) {
            v.add(Verificacao.alerta(campo, "Documento (CNPJ/CPF) não encontrado."));
            return;
        }
        String d = parte.documento().replaceAll("[.\\-/\\s]", "");
        boolean valido = d.length() == 11 ? Digitos.cpfValido(d) : Digitos.cnpjValido(d);
        conferir(v, campo, valido, (d.length() == 11 ? "CPF" : "CNPJ") + " com dígito verificador válido.",
                "O " + (d.length() == 11 ? "CPF" : "CNPJ") + " " + parte.documento() + " tem dígito verificador inválido.");
    }

    private void receita(List<Verificacao> v, Parte emitente) {
        if (emitente == null || !Digitos.cnpjValido(Objects.requireNonNullElse(emitente.documento(), ""))) {
            return;
        }
        Receita.avaliar(v, "emitente", emitente, receita.consultar(Digitos.somenteDigitos(emitente.documento())));
    }

    static void conferir(List<Verificacao> v, String campo, boolean certo, String seCerto, String seErrado) {
        v.add(certo ? Verificacao.ok(campo, seCerto) : Verificacao.erro(campo, seErrado));
    }

    private static boolean numeroIgual(String impresso, long daChave) {
        String d = Digitos.somenteDigitos(impresso);
        return !d.isEmpty() && Long.parseLong(d) == daChave;
    }

    static boolean igual(BigDecimal a, BigDecimal b) {
        return a.subtract(b).abs().compareTo(new BigDecimal("0.01")) <= 0;
    }

    private static BigDecimal zero(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }
}
