package io.github.ronnyarruda20.extrator.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Sem a chave do provedor, o Spring AI falha no meio da criação dos beans com uma
 * cadeia de exceções difícil de ler. Esta verificação roda antes e diz exatamente
 * qual variável de ambiente falta.
 */
public class VerificadorDeChaves implements EnvironmentPostProcessor, Ordered {

    static final String GEMINI = "google-genai";
    static final String ANTHROPIC = "anthropic";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        List<String> faltando = faltando(env);
        if (!faltando.isEmpty()) {
            throw new IllegalStateException("Defina " + String.join(" e ", faltando)
                    + " antes de iniciar. O provedor do modelo vem de CHAT_PROVIDER ("
                    + env.getProperty("spring.ai.model.chat", GEMINI) + ").");
        }
    }

    static List<String> faltando(ConfigurableEnvironment env) {
        String chat = env.getProperty("spring.ai.model.chat", GEMINI);
        List<String> faltando = new ArrayList<>();
        if (GEMINI.equals(chat)
                && !StringUtils.hasText(env.getProperty("spring.ai.google.genai.api-key"))) {
            faltando.add("GEMINI_API_KEY");
        }
        if (ANTHROPIC.equals(chat) && !StringUtils.hasText(env.getProperty("spring.ai.anthropic.api-key"))) {
            faltando.add("ANTHROPIC_API_KEY");
        }
        return faltando;
    }

    /** Depois do carregamento do application.yml e das variáveis de ambiente. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
