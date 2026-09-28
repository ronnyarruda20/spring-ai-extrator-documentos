package io.github.ronnyarruda20.extrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.ronnyarruda20.extrator.receita.ConsultaCnpj;
import io.github.ronnyarruda20.extrator.receita.ConsultaCnpjMcp;

@Configuration(proxyBeanMethods = false)
public class ReceitaConfig {

    /** Sem URL do spring-ai-mcp-brasil, a consulta à Receita fica indisponível e vira alerta. */
    @Bean
    @ConditionalOnMissingBean
    ConsultaCnpj consultaCnpj(@Value("${app.mcp-brasil-url:}") String url) {
        if (url.isBlank()) {
            return cnpj -> new ConsultaCnpj.Indisponivel("consulta à Receita desligada: MCP_BRASIL_URL vazio");
        }
        return new ConsultaCnpjMcp(url);
    }
}
