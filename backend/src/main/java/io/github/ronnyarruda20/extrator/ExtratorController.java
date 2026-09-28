package io.github.ronnyarruda20.extrator;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.github.ronnyarruda20.extrator.extracao.ExtracaoService;

@RestController
public class ExtratorController {

    static final Map<String, MimeType> ACEITOS = Map.of(
            "pdf", MimeType.valueOf("application/pdf"),
            "png", MimeType.valueOf("image/png"),
            "jpg", MimeType.valueOf("image/jpeg"),
            "jpeg", MimeType.valueOf("image/jpeg"));

    public record Info(String provedor, String modelo) {
    }

    private final ExtracaoService service;
    private final Info info;

    public ExtratorController(ExtracaoService service, Environment env) {
        this.service = service;
        String provedor = env.getProperty("spring.ai.model.chat", "google-genai");
        this.info = new Info(provedor, "anthropic".equals(provedor)
                ? env.getProperty("spring.ai.anthropic.chat.options.model")
                : env.getProperty("spring.ai.google.genai.chat.options.model"));
    }

    @GetMapping("/api/info")
    public Info info() {
        return info;
    }

    @PostMapping(value = "/api/extracoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtracaoService.Resultado extrair(@RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase();
        MimeType tipo = ACEITOS.get(nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1) : "");
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um PDF, PNG ou JPG.");
        }
        if (arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo está vazio.");
        }
        return service.processar(arquivo.getBytes(), tipo);
    }

    /** Falha do provedor de IA vira 502 com mensagem clara, sem detalhes internos. */
    @ExceptionHandler(RuntimeException.class)
    ProblemDetail falha(RuntimeException e) {
        if (e instanceof ResponseStatusException r) {
            return ProblemDetail.forStatusAndDetail(r.getStatusCode(), r.getReason());
        }
        org.slf4j.LoggerFactory.getLogger(ExtratorController.class).error("Falha na extração", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "O modelo não conseguiu ler o documento agora. Tente de novo em instantes.");
    }
}
