package com.analistas.lavadero.model.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class WhatsAppFirmaTest {
    @Test
    void validaHmacSobreCuerpoOriginal() throws Exception {
        var service = new WhatsAppServiceImpl();
        ReflectionTestUtils.setField(service, "appSecret", "secreto-de-prueba");
        byte[] cuerpo = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(
                new SecretKeySpec(
                        "secreto-de-prueba".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = "sha256=" + HexFormat.of().formatHex(mac.doFinal(cuerpo));
        assertThat(service.verificarFirma(cuerpo, firma)).isTrue();
        assertThat(service.verificarFirma("{}".getBytes(), firma)).isFalse();
        assertThat(service.verificarFirma(cuerpo, "sha256=00")).isFalse();
        assertThat(service.verificarFirma(cuerpo, null)).isFalse();
    }
}
