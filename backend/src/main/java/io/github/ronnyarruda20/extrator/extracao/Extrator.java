package io.github.ronnyarruda20.extrator.extracao;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

/**
 * Lê o documento com um modelo multimodal: o PDF ou a imagem vão direto para o modelo,
 * sem OCR à parte, e a resposta volta no formato de {@link Extracao}.
 */
@Component
public class Extrator {

    static final String INSTRUCOES = """
            Você extrai dados de documentos fiscais e financeiros brasileiros.
            Leia o documento anexado e preencha os campos.
            - Copie o que está impresso, sem calcular nem corrigir nada: a conferência é feita depois, \
            por regras. Se um total impresso estiver errado, copie o total errado.
            - CNPJ, CPF, chave de acesso e linha digitável: só os caracteres, sem pontos, barras, \
            hífens ou espaços.
            - Valores em reais como número decimal com ponto (1234.56). Datas no formato AAAA-MM-DD.
            - Campo que não existe no documento fica nulo. Não invente.
            - Preencha só notaFiscal ou só boleto, de acordo com o tipo.
            """;

    private final ChatClient chat;

    public Extrator(ChatClient.Builder builder) {
        this.chat = builder.defaultSystem(INSTRUCOES).build();
    }

    public Extracao extrair(byte[] conteudo, MimeType tipo) {
        return chat.prompt()
                .user(u -> u.text("Extraia os dados deste documento.").media(tipo, new ByteArrayResource(conteudo)))
                .call()
                .entity(Extracao.class);
    }
}
