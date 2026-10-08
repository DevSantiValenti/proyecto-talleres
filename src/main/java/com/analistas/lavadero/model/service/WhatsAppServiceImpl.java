package com.analistas.lavadero.model.service;

import com.analistas.lavadero.model.domain.dto.Dtos.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class WhatsAppServiceImpl implements IWhatsAppService {
    @Value("${app.whatsapp.enabled}")
    private boolean enabled;

    @Value("${app.whatsapp.token}")
    private String token;

    @Value("${app.whatsapp.phone-id}")
    private String phoneId;

    @Value("${app.whatsapp.app-secret}")
    private String appSecret;

    @Value("${app.whatsapp.verify-token}")
    private String verifyToken;

    @Value("${app.whatsapp.template}")
    private String template;

    @Value("${app.whatsapp.language}")
    private String language;

    @Value("${app.whatsapp.graph-version}")
    private String graphVersion;

    private final RestClient client;

    public WhatsAppServiceImpl() {
        var http =
                java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(20));
        client =
                RestClient.builder()
                        .requestFactory(factory)
                        .baseUrl("https://graph.facebook.com")
                        .build();
    }

    public boolean habilitado() {
        return enabled
                && !token.isBlank()
                && !phoneId.isBlank()
                && !appSecret.isBlank()
                && !verifyToken.isBlank();
    }

    public ResultadoEnvioDto enviar(MensajeWhatsAppDto m) {
        if (!habilitado())
            return new ResultadoEnvioDto("DESHABILITADO", null, "WhatsApp no configurado");
        var params =
                List.of(m.vehiculo(), m.mantenimiento(), m.objetivo(), m.enlace()).stream()
                        .map(x -> Map.of("type", "text", "text", x))
                        .toList();
        var body =
                Map.of(
                        "messaging_product",
                        "whatsapp",
                        "biz_opaque_callback_data",
                        m.referencia(),
                        "to",
                        m.telefono().replace("+", ""),
                        "type",
                        "template",
                        "template",
                        Map.of(
                                "name",
                                template,
                                "language",
                                Map.of("code", language),
                                "components",
                                List.of(Map.of("type", "body", "parameters", params))));
        try {
            String response =
                    client.post()
                            .uri("/{version}/{phone}/messages", graphVersion, phoneId)
                            .headers(h -> h.setBearerAuth(token))
                            .body(body)
                            .retrieve()
                            .body(String.class);
            String id =
                    JsonMapper.builder()
                            .build()
                            .readTree(response)
                            .path("messages")
                            .path(0)
                            .path("id")
                            .asText("");
            return id.isBlank()
                    ? new ResultadoEnvioDto(
                            "INCIERTO", null, "Respuesta sin identificador; revisar en Meta")
                    : new ResultadoEnvioDto("ACEPTADO", id, "Aceptado por Meta");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429)
                return new ResultadoEnvioDto("REINTENTAR", null, "Límite de solicitudes");
            return new ResultadoEnvioDto(
                    e.getStatusCode().is5xxServerError() ? "INCIERTO" : "FALLIDO",
                    null,
                    "HTTP " + e.getStatusCode().value() + "; revisar configuración de Meta");
        } catch (Exception e) {
            return new ResultadoEnvioDto(
                    "INCIERTO",
                    null,
                    "No se pudo confirmar el envío; revisar en Meta antes de reenviar");
        }
    }

    public boolean verificarToken(String supplied) {
        return !verifyToken.isBlank()
                && supplied != null
                && MessageDigest.isEqual(
                        verifyToken.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8));
    }

    public boolean verificarFirma(byte[] body, String supplied) {
        if (appSecret.isBlank() || supplied == null || !supplied.startsWith("sha256="))
            return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return MessageDigest.isEqual(
                    mac.doFinal(body), HexFormat.of().parseHex(supplied.substring(7)));
        } catch (Exception e) {
            return false;
        }
    }
}
