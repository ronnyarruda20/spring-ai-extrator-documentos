package io.github.ronnyarruda20.extrator.validacao;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.ronnyarruda20.extrator.extracao.Extracao.Parte;
import io.github.ronnyarruda20.extrator.receita.ConsultaCnpj;

/** Confronta o nome impresso com o cadastro da Receita. */
final class Receita {

    private static final Set<String> IGNORAR = Set.of("ltda", "sa", "me", "epp", "eireli", "de", "da", "do", "das", "dos", "e");

    private Receita() {
    }

    static void avaliar(List<Verificacao> v, String campo, Parte parte, ConsultaCnpj.Situacao s) {
        switch (s) {
            case ConsultaCnpj.NaoEncontrado n -> v.add(Verificacao.erro(campo,
                    "O CNPJ " + parte.documento() + " não consta na base da Receita Federal."));
            case ConsultaCnpj.Indisponivel i -> v.add(Verificacao.alerta(campo,
                    "Não foi possível consultar o CNPJ na Receita agora (" + i.motivo() + ")."));
            case ConsultaCnpj.Encontrado e -> {
                if (!"ATIVA".equalsIgnoreCase(e.situacaoCadastral())) {
                    v.add(Verificacao.alerta(campo, "Na Receita, o CNPJ está com situação " + e.situacaoCadastral() + "."));
                }
                if (parecidos(parte.nome(), e.razaoSocial())) {
                    v.add(Verificacao.ok(campo, "CNPJ ativo na Receita, em nome de " + e.razaoSocial() + "."));
                } else {
                    v.add(Verificacao.alerta(campo, "Na Receita, esse CNPJ pertence a " + e.razaoSocial()
                            + ", e não a " + parte.nome() + "."));
                }
            }
        }
    }

    /** Nomes parecidos: metade ou mais das palavras relevantes em comum. */
    static boolean parecidos(String a, String b) {
        Set<String> pa = palavras(a);
        Set<String> pb = palavras(b);
        if (pa.isEmpty() || pb.isEmpty()) {
            return false;
        }
        long comuns = pa.stream().filter(pb::contains).count();
        return comuns * 2 >= Math.min(pa.size(), pb.size());
    }

    private static Set<String> palavras(String s) {
        if (s == null) {
            return Set.of();
        }
        String limpo = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ");
        return Arrays.stream(limpo.split("\\s+")).filter(p -> p.length() > 1 && !IGNORAR.contains(p))
                .collect(Collectors.toSet());
    }
}
