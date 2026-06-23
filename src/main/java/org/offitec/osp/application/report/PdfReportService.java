package org.offitec.osp.application.report;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.offitec.osp.application.report.chart.PressureDropSvgBuilder;
import org.offitec.osp.application.report.chart.WorkingLimitSvgBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Renders the unit selection report to PDF bytes, entirely on the backend.
 *
 * Pipeline: UnitReportModel -> (Thymeleaf) HTML with inline SVG charts and base64
 * logos -> (openhtmltopdf + Batik) PDF bytes. No browser is involved.
 */
@Service
public class PdfReportService {

    private final TemplateEngine templateEngine;
    private final WorkingLimitSvgBuilder workingLimitSvg;
    private final PressureDropSvgBuilder pressureDropSvg;

    // Logos read once and cached as data URIs so they embed on every page.
    private final String ospLogoDataUri;
    private final String offitecLogoDataUri;

    // A Unicode TrueType font (full Latin coverage) embedded so locale-specific characters
    // in user-entered fields — e.g. Turkish "İzmir" — render instead of showing "#".
    // The built-in PDF Helvetica only covers Latin-1, which is why those glyphs were missing.
    private final byte[] unicodeFont;

    // Fetches remote unit images (primary image + drawings) so they can be embedded as data
    // URIs, the same way the logos are — keeps PDF rendering self-contained (no live URL loads).
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public PdfReportService(WorkingLimitSvgBuilder workingLimitSvg,
                            PressureDropSvgBuilder pressureDropSvg) {
        this.workingLimitSvg = workingLimitSvg;
        this.pressureDropSvg = pressureDropSvg;
        this.templateEngine = buildTemplateEngine();
        this.ospLogoDataUri = loadAsDataUri("report/logo/osp-logo.png");
        this.offitecLogoDataUri = loadAsDataUri("report/logo/offitec-logo.png");
        this.unicodeFont = loadBytes("report/fonts/Geist-Regular.ttf");
    }

    public byte[] render(UnitReportModel model) {
        Context ctx = new Context(Locale.US);
        ctx.setVariable("m", model);
        ctx.setVariable("ospLogo", ospLogoDataUri);
        ctx.setVariable("offitecLogo", offitecLogoDataUri);
        ctx.setVariable("workingLimitSvg", workingLimitSvg.build(model.getWorkingLimit(), model.getT()));
        ctx.setVariable("pressureDropSvg", pressureDropSvg.build(model.getPressureCurve(), model.getT()));

        // Embed the unit imagery as data URIs (null/failed fetches simply render nothing).
        ctx.setVariable("primaryImage", fetchAsDataUri(model.getPrimaryImageUrl()));
        List<String> drawings = new ArrayList<>();
        if (model.getDrawingUrls() != null) {
            for (String url : model.getDrawingUrls()) {
                String dataUri = fetchAsDataUri(url);
                if (dataUri != null) drawings.add(dataUri);
            }
        }
        ctx.setVariable("drawings", drawings);

        String html = templateEngine.process("report", ctx);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useSVGDrawer(new BatikSVGDrawer());
            // Register the Unicode font under the "Geist" family (normal + bold both map to
            // the same file) so CSS font-family: 'Geist' resolves to a glyph-complete font.
            if (unicodeFont != null && unicodeFont.length > 0) {
                builder.useFont(() -> new ByteArrayInputStream(unicodeFont), "Geist", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
                builder.useFont(() -> new ByteArrayInputStream(unicodeFont), "Geist", 700, BaseRendererBuilder.FontStyle.NORMAL, true);
            }
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render report PDF", e);
        }
    }

    private TemplateEngine buildTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("report/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private String loadAsDataUri(String classpath) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            // Missing logo shouldn't break the whole report.
            return "";
        }
    }

    /**
     * Fetches a remote image and returns it as a {@code data:} URI so it embeds directly in
     * the PDF. Returns {@code null} for blank URLs or any fetch/HTTP failure so a missing or
     * unreachable image never breaks report generation.
     */
    private String fetchAsDataUri(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) return null;
            byte[] bytes = response.body();
            if (bytes == null || bytes.length == 0) return null;
            return "data:" + sniffImageMime(bytes) + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    // Detect the image type from its magic bytes; defaults to JPEG (uploads are optimized to JPEG).
    private String sniffImageMime(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
            return "image/gif";
        }
        return "image/jpeg";
    }

    private byte[] loadBytes(String classpath) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            // Missing font shouldn't break the whole report; it falls back to Helvetica.
            return null;
        }
    }

    // Convenience for ad-hoc local verification (writes a sample PDF). Not used in production paths.
    static String escape(String s) {
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
