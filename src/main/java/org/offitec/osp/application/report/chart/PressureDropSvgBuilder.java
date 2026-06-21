package org.offitec.osp.application.report.chart;

import org.offitec.osp.application.report.UnitReportModel;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Renders the Pressure Drop / Flow Rate graph as an SVG string.
 * The curve is synthesized as PD = PDd * (Q/Qd)^2 (pressure drop scales with the
 * square of flow), swept across [0.6 Qd, 1.3 Qd], with the design point marked.
 */
@Component
public class PressureDropSvgBuilder {

    private static final int W = 520, H = 300;
    private static final int LEFT = 55, RIGHT = 20, TOP = 15, BOTTOM = 45;

    public String build(UnitReportModel.PressureCurve pc) {
        double qd = pc.getDesignFlowRate();
        double pdd = pc.getDesignPressureDrop();
        if (qd <= 0) qd = 1;

        double qStart = qd * 0.6, qEnd = qd * 1.3;
        double pdEnd = pdd * (qEnd / qd) * (qEnd / qd); // max PD at qEnd

        double plotL = LEFT, plotR = W - RIGHT, plotT = TOP, plotB = H - BOTTOM;
        ChartScale sx = new ChartScale(qStart, qEnd, plotL, plotR);
        ChartScale sy = new ChartScale(0, pdEnd * 1.05, plotB, plotT);

        StringBuilder s = new StringBuilder();
        s.append(String.format(Locale.US,
                "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d' viewBox='0 0 %d %d'>", W, H, W, H));

        s.append(rect(plotL, plotT, plotR - plotL, plotB - plotT, "#cccccc"));

        for (double t : sx.ticks(7)) {
            double x = sx.px(t);
            s.append(line(x, plotT, x, plotB, "#eeeeee"));
            s.append(text(x, plotB + 16, "middle", ChartScale.label(t)));
        }
        for (double t : sy.ticks(6)) {
            double y = sy.px(t);
            s.append(line(plotL, y, plotR, y, "#eeeeee"));
            s.append(text(plotL - 8, y + 4, "end", ChartScale.label(t)));
        }

        // The curve
        StringBuilder pts = new StringBuilder();
        int n = 40;
        for (int i = 0; i <= n; i++) {
            double q = qStart + (qEnd - qStart) * i / n;
            double pd = pdd * (q / qd) * (q / qd);
            pts.append(String.format(Locale.US, "%s%.1f,%.1f", i == 0 ? "" : " ", sx.px(q), sy.px(pd)));
        }
        s.append(String.format(Locale.US,
                "<polyline points='%s' fill='none' stroke='#1f4e9b' stroke-width='2'/>", pts));

        // Design point marker
        double dpx = sx.px(qd), dpy = sy.px(pdd);
        s.append(String.format(Locale.US, "<circle cx='%.1f' cy='%.1f' r='4' fill='#c62828'/>", dpx, dpy));
        s.append(labelBox(dpx + 6, dpy - 6,
                String.format(Locale.US, "%.2f m³/h; %.0f kPa", qd, pdd)));

        s.append(text((plotL + plotR) / 2, H - 8, "middle", "Water Flow Rate [m³/h]"));
        s.append(String.format(Locale.US,
                "<text x='14' y='%.1f' text-anchor='middle' transform='rotate(-90 14 %.1f)' "
              + "font-family='Helvetica,Arial,sans-serif' font-size='10' fill='#333'>Pressure drops [kPa]</text>",
                (plotT + plotB) / 2, (plotT + plotB) / 2));

        s.append("</svg>");
        return s.toString();
    }

    private String rect(double x, double y, double w, double h, String stroke) {
        return String.format(Locale.US,
                "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' fill='none' stroke='%s' stroke-width='1'/>",
                x, y, w, h, stroke);
    }

    private String line(double x1, double y1, double x2, double y2, String stroke) {
        return String.format(Locale.US,
                "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='1'/>",
                x1, y1, x2, y2, stroke);
    }

    private String text(double x, double y, String anchor, String txt) {
        return String.format(Locale.US,
                "<text x='%.1f' y='%.1f' text-anchor='%s' font-family='Helvetica,Arial,sans-serif' "
              + "font-size='10' fill='#333'>%s</text>", x, y, anchor, esc(txt));
    }

    private String labelBox(double x, double y, String txt) {
        double w = txt.length() * 5.6 + 8;
        return String.format(Locale.US,
                "<rect x='%.1f' y='%.1f' width='%.1f' height='15' rx='2' fill='#ffffff' stroke='#999' stroke-width='0.7'/>"
              + "<text x='%.1f' y='%.1f' font-family='Helvetica,Arial,sans-serif' font-size='10' fill='#111'>%s</text>",
                x, y - 11, w, x + 4, y, esc(txt));
    }

    private String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
