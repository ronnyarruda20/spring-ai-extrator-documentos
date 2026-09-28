package io.github.ronnyarruda20.extrator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import io.github.ronnyarruda20.extrator.receita.ConsultaCnpj;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ExtracaoIT.Config.class)
@ActiveProfiles("test")
class ExtracaoIT {

    /** O que um modelo devolveria ao ler exemplos/nota-fiscal-exemplo.pdf. */
    static final String NOTA_JSON = """
            {"tipo":"NOTA_FISCAL","notaFiscal":{
              "chaveAcesso":"51260990770487000184550010000012341123456789","numero":"1234","serie":"001",
              "dataEmissao":"2026-09-15",
              "emitente":{"nome":"Papelaria Exemplo Ltda","documento":"90770487000184"},
              "destinatario":{"nome":"Empresa Exemplo Ltda","documento":"90216739000128"},
              "itens":[{"descricao":"Caderno","quantidade":20,"valorUnitario":24.90,"valorTotal":498.00},
                       {"descricao":"Caneta","quantidade":4,"valorUnitario":45.50,"valorTotal":182.00},
                       {"descricao":"Papel A4","quantidade":10,"valorUnitario":32.75,"valorTotal":327.50}],
              "valorProdutos":1007.50,"valorFrete":35.00,"valorDesconto":42.50,"valorTotal":1000.00},
             "boleto":null,"observacoes":[]}""";

    static class ModeloFalso implements ChatModel {
        final AtomicReference<Prompt> ultimo = new AtomicReference<>();
        volatile String resposta = NOTA_JSON;
        volatile boolean falhar;

        @Override
        public ChatResponse call(Prompt prompt) {
            ultimo.set(prompt);
            if (falhar) {
                throw new IllegalStateException("provedor fora do ar");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(resposta))));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {
        @Bean
        ModeloFalso chatModel() {
            return new ModeloFalso();
        }

        @Bean
        @Primary
        ConsultaCnpj receitaDeTeste() {
            return cnpj -> new ConsultaCnpj.Encontrado("PAPELARIA EXEMPLO LTDA", "ATIVA");
        }
    }

    @LocalServerPort
    int porta;

    @Autowired
    ModeloFalso modelo;

    RestClient http;

    @BeforeEach
    void preparar() {
        http = RestClient.builder().baseUrl("http://localhost:" + porta).defaultStatusHandler(s -> true, (q, r) -> { }).build();
        modelo.resposta = NOTA_JSON;
        modelo.falhar = false;
    }

    @Test
    void mandaOPdfAoModeloEConfereOQueEleExtraiu() throws Exception {
        var r = enviar("nota-fiscal-exemplo.pdf", Files.readAllBytes(Path.of("../exemplos/nota-fiscal-exemplo.pdf")));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).containsEntry("tipo", "NOTA_FISCAL").containsEntry("situacao", "CONFERIDO");

        UserMessage usuario = (UserMessage) modelo.ultimo.get().getInstructions().getLast();
        assertThat(usuario.getMedia()).singleElement()
                .satisfies(m -> assertThat(m.getMimeType().toString()).isEqualTo("application/pdf"));
        assertThat(modelo.ultimo.get().getInstructions().getFirst().getText()).contains("sem calcular nem corrigir");
    }

    @Test
    void totalQueNaoFechaDeixaAExtracaoInconsistente() {
        modelo.resposta = NOTA_JSON.replace("\"valorTotal\":1000.00", "\"valorTotal\":1100.00");

        var r = enviar("nota.pdf", new byte[] {1, 2, 3});

        assertThat(r.getBody()).containsEntry("situacao", "INCONSISTENTE");
    }

    @Test
    void recusaFormatoNaoSuportado() {
        var r = enviar("planilha.xlsx", new byte[] {1});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("detail", "Envie um PDF, PNG ou JPG.");
    }

    @Test
    void falhaDoProvedorVira502ComMensagemClara() {
        modelo.falhar = true;

        var r = enviar("nota.pdf", new byte[] {1});

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat((String) r.getBody().get("detail")).contains("Tente de novo");
    }

    private ResponseEntity<Map<String, Object>> enviar(String nome, byte[] conteudo) {
        var partes = new LinkedMultiValueMap<String, Object>();
        partes.add("arquivo", new ByteArrayResource(conteudo) {
            @Override
            public String getFilename() {
                return nome;
            }
        });
        return http.post().uri("/api/extracoes").contentType(MediaType.MULTIPART_FORM_DATA).body(partes)
                .retrieve().toEntity(new ParameterizedTypeReference<>() {
                });
    }
}
