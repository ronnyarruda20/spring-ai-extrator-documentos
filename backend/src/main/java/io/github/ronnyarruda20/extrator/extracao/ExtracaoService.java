package io.github.ronnyarruda20.extrator.extracao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import io.github.ronnyarruda20.extrator.receita.ConsultaCnpj;
import io.github.ronnyarruda20.extrator.validacao.ValidadorBoleto;
import io.github.ronnyarruda20.extrator.validacao.ValidadorNotaFiscal;
import io.github.ronnyarruda20.extrator.validacao.Verificacao;
import io.github.ronnyarruda20.extrator.validacao.Verificacao.Status;

/** Extrai com o modelo e confere com as regras. O modelo lê; o código decide se dá para confiar. */
@Service
public class ExtracaoService {

    /**
     * @param situacao CONFERIDO (tudo bate), REVISAR (algo não pôde ser conferido) ou INCONSISTENTE (algo não bate)
     */
    public record Resultado(Extracao.Tipo tipo, Situacao situacao, Extracao dados, List<Verificacao> verificacoes,
            long duracaoMs) {
    }

    public enum Situacao { CONFERIDO, REVISAR, INCONSISTENTE }

    private final Extrator extrator;
    private final ValidadorNotaFiscal notaFiscal;
    private final ValidadorBoleto boleto;

    public ExtracaoService(Extrator extrator, ConsultaCnpj receita) {
        this.extrator = extrator;
        this.notaFiscal = new ValidadorNotaFiscal(receita);
        this.boleto = new ValidadorBoleto(receita);
    }

    public Resultado processar(byte[] conteudo, MimeType tipo) {
        long inicio = System.currentTimeMillis();
        Extracao e = extrator.extrair(conteudo, tipo);
        List<Verificacao> v = switch (e.tipo() == null ? Extracao.Tipo.OUTRO : e.tipo()) {
            case NOTA_FISCAL -> e.notaFiscal() == null ? List.of(Verificacao.erro("notaFiscal", "O modelo não preencheu os dados da nota."))
                    : notaFiscal.validar(e.notaFiscal());
            case BOLETO -> e.boleto() == null ? List.of(Verificacao.erro("boleto", "O modelo não preencheu os dados do boleto."))
                    : boleto.validar(e.boleto());
            case OUTRO -> List.of(Verificacao.alerta("tipo", "O documento não parece ser uma nota fiscal nem um boleto."));
        };
        return new Resultado(e.tipo(), situacao(v), e, v, System.currentTimeMillis() - inicio);
    }

    static Situacao situacao(List<Verificacao> v) {
        if (v.stream().anyMatch(x -> x.status() == Status.ERRO)) {
            return Situacao.INCONSISTENTE;
        }
        if (v.stream().anyMatch(x -> x.status() == Status.ALERTA)) {
            return Situacao.REVISAR;
        }
        return Situacao.CONFERIDO;
    }
}
