package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import lombok.RequiredArgsConstructor;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TicketServiceImpl implements ITicketService {
    private final IOrdenTrabajoService ordenes;
    private final IVehiculoService vehiculos;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Value("${app.nombre}")
    private String negocio;

    public byte[] orden(Long id) {
        var o = ordenes.obtener(id);
        var lineas = new ArrayList<String>();
        o.servicios().stream()
                .filter(s -> s.estado() != EstadoTrabajo.CANCELADO)
                .forEach(s -> lineas.add(s.nombre() + "  $ " + s.precio()));
        o.repuestos()
                .forEach(
                        r ->
                                lineas.add(
                                        r.cantidad()
                                                + " x "
                                                + r.nombre()
                                                + "  $ "
                                                + r.precio()
                                                        .multiply(
                                                                BigDecimal.valueOf(r.cantidad()))));
        var t =
                new TicketDto(
                        negocio,
                        o.id(),
                        o.vehiculo().marca() + " " + o.vehiculo().modelo(),
                        o.vehiculo().patente(),
                        o.ingreso(),
                        lineas,
                        o.total(),
                        o.pago() == null ? "PENDIENTE" : "PAGADO",
                        o.pago() != null
                                ? o.pago().medio().name()
                                : (o.medioPrevisto() == null
                                        ? "A definir"
                                        : o.medioPrevisto().name()),
                        o.vehiculo().enlace());
        return render(t);
    }

    public byte[] vehiculo(Long id) {
        var v = vehiculos.obtener(id);
        return render(
                new TicketDto(
                        negocio,
                        null,
                        v.marca() + " " + v.modelo(),
                        v.patente(),
                        null,
                        List.of("Historial técnico y próximos mantenimientos"),
                        null,
                        null,
                        null,
                        v.enlace()));
    }

    private byte[] render(TicketDto t) {
        try (var doc = new PDDocument();
                var bytes = new ByteArrayOutputStream();
                var fontStream =
                        getClass().getResourceAsStream("/static/css/fonts/DejaVuSans.ttf")) {
            var font = PDType0Font.load(doc, fontStream);
            var writer = new Impresion(doc, font);
            writer.texto(t.negocio(), 13);
            writer.texto(t.orden() == null ? "FICHA DEL VEHÍCULO" : "ORDEN #" + t.orden(), 11);
            writer.texto(t.vehiculo() + " · " + t.patente(), 10);
            if (t.ingreso() != null)
                writer.texto(
                        "Ingreso: "
                                + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                                        .withZone(clock.getZone())
                                        .format(t.ingreso()),
                        9);
            writer.texto("----------------------------------------", 9);
            for (String linea : t.lineas()) writer.texto(linea, 9);
            if (t.total() != null) {
                writer.texto("TOTAL  $ " + t.total(), 12);
                writer.texto(t.pago() + " · " + t.medio(), 10);
                writer.texto("Comprobante interno. No válido como factura.", 8);
            }
            if (t.enlaceFicha() != null) {
                writer.texto("Historial permanente de taller", 10);
                writer.qr(t.enlaceFicha());
                writer.texto(t.enlaceFicha(), 7);
                writer.texto("Conservá este QR para tus próximos services.", 8);
            }
            writer.cerrar();
            doc.save(bytes);
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    /** Renderizador privado: no contiene reglas del negocio. Páginas de 80 mm. */
    private static class Impresion {
        private final PDDocument doc;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y;

        Impresion(PDDocument doc, PDType0Font font) throws IOException {
            this.doc = doc;
            this.font = font;
            nueva();
        }

        void nueva() throws IOException {
            if (stream != null) stream.close();
            var page = new PDPage(new PDRectangle(226.77f, 720));
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = 700;
        }

        void texto(String texto, float size) throws IOException {
            StringBuilder linea = new StringBuilder();
            for (int cp : texto.codePoints().toArray()) {
                String letra = new String(Character.toChars(cp));
                try {
                    font.getStringWidth(letra);
                } catch (IllegalArgumentException e) {
                    letra = "?";
                }
                if (font.getStringWidth(linea + letra) * size / 1000 > 198 || cp == '\n') {
                    renglon(linea.toString(), size);
                    linea.setLength(0);
                }
                if (cp != '\n') linea.append(letra);
            }
            if (!linea.isEmpty()) renglon(linea.toString(), size);
            y -= 4;
        }

        void renglon(String s, float size) throws IOException {
            if (y < size + 20) nueva();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(14, y);
            stream.showText(s);
            stream.endText();
            y -= size + 4;
        }

        void qr(String url) throws Exception {
            if (y < 180) nueva();
            var matrix =
                    new MultiFormatWriter()
                            .encode(
                                    url,
                                    BarcodeFormat.QR_CODE,
                                    420,
                                    420,
                                    Map.of(
                                            EncodeHintType.ERROR_CORRECTION,
                                            ErrorCorrectionLevel.M,
                                            EncodeHintType.MARGIN,
                                            4));
            var image =
                    LosslessFactory.createFromImage(
                            doc, MatrixToImageWriter.toBufferedImage(matrix));
            stream.drawImage(image, 38, y - 150, 150, 150);
            y -= 164;
        }

        void cerrar() throws IOException {
            stream.close();
        }
    }
}
