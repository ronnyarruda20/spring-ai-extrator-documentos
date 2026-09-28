package io.github.ronnyarruda20.extrator.receita;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consulta o CNPJ pela ferramenta {@code consultar_cnpj} do servidor spring-ai-mcp-brasil.
 * Conecta na primeira consulta e guarda as respostas em memória.
 */
public class ConsultaCnpjMcp implements ConsultaCnpj, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ConsultaCnpjMcp.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String url;
    private final Map<String, Situacao> cache = new ConcurrentHashMap<>();
    private McpSyncClient cliente;

    public ConsultaCnpjMcp(String url) {
        this.url = url;
    }

    @Override
    public Situacao consultar(String cnpj) {
        Situacao s = cache.get(cnpj);
        if (s != null) {
            return s;
        }
        s = buscar(cnpj);
        if (!(s instanceof Indisponivel)) {
            cache.put(cnpj, s);
        }
        return s;
    }

    private Situacao buscar(String cnpj) {
        try {
            var r = cliente().callTool(new McpSchema.CallToolRequest("consultar_cnpj", Map.of("cnpj", cnpj)));
            String texto = r.content().stream()
                    .filter(McpSchema.TextContent.class::isInstance)
                    .map(c -> ((McpSchema.TextContent) c).text())
                    .reduce("", String::concat);
            return interpretar(Boolean.TRUE.equals(r.isError()), texto);
        } catch (RuntimeException e) {
            log.warn("Falha ao consultar CNPJ em {}", url, e);
            synchronized (this) {
                cliente = null;
            }
            return new Indisponivel("o servidor de consulta não respondeu");
        }
    }

    static Situacao interpretar(boolean erro, String texto) {
        if (erro) {
            return texto.contains("não encontrado") ? new NaoEncontrado() : new Indisponivel(texto.lines().findFirst().orElse(""));
        }
        JsonNode n = JSON.readTree(texto);
        return new Encontrado(n.path("razaoSocial").asString(""), n.path("situacaoCadastral").asString(""));
    }

    private synchronized McpSyncClient cliente() {
        if (cliente == null) {
            var novo = McpClient.sync(HttpClientStreamableHttpTransport.builder(url).build())
                    .requestTimeout(Duration.ofSeconds(20)).build();
            novo.initialize();
            cliente = novo;
        }
        return cliente;
    }

    @Override
    public synchronized void close() {
        if (cliente != null) {
            cliente.closeGracefully();
        }
    }
}
