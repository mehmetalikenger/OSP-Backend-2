package org.offitec.osp.application.report.chart;

import org.offitec.osp.application.report.UnitReportModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Renders the Working Limit graph as an SVG string:
 *   x-axis = Water Outlet Temperature [°C], y-axis = Ambient Temperature [°C].
 * A translucent green rectangle marks the safe operating envelope and a dot marks
 * the (water outlet, ambient) operating point with a small value label.
 */
@Component
public class WorkingLimitSvgBuilder {

    private static final int W = 520, H = 320;
    private static final int LEFT = 55, RIGHT = 20, TOP = 15, BOTTOM = 45;

    public String build(UnitReportModel.WorkingLimit wl) {
        double plotL = LEFT, plotR = W - RIGHT, plotT = TOP, plotB = H - BOTTOM;

        // Domains padded to include both the safe rectangle and the operating point.
        double xLo = Math.min(wl.getMinWaterOutlet(), wl.getPointWaterOutlet()) - 5;
        double xHi = Math.max(wl.getMaxWaterOutlet(), wl.getPointWaterOutlet()) + 5;
        double yLo = Math.min(wl.getMinAmbient(), wl.getPointAmbient()) - 10;
        double yHi = Math.max(wl.getMaxAmbient(), wl.getPointAmbient()) + 10;

        ChartScale sx = new ChartScale(xLo, xHi, plotL, plotR);
        ChartScale sy = new ChartScale(yLo, yHi, plotB, plotT); // inverted: data up -> pixels up

        StringBuilder s = new StringBuilder();
        s.append(String.format(Locale.US,
                "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d' viewBox='0 0 %d %d'>", W, H, W, H));

        // Plot frame
        s.append(rect(plotL, plotT, plotR - plotL, plotB - plotT, "none", "#cccccc", 1));

        // Gridlines + labels
        for (double t : sx.ticks(6)) {
            double x = sx.px(t);
            s.append(line(x, plotT, x, plotB, "#eeeeee", 1));
            s.append(text(x, plotB + 16, "middle", ChartScale.label(t)));
        }
        for (double t : sy.ticks(6)) {
            double y = sy.px(t);
            s.append(line(plotL, y, plotR, y, "#eeeeee", 1));
            s.append(text(plotL - 8, y + 4, "end", ChartScale.label(t)));
        }

        // Safe-area rectangle
        double rx = sx.px(wl.getMinWaterOutlet());
        double rw = sx.px(wl.getMaxWaterOutlet()) - rx;
        double ryTop = sy.px(wl.getMaxAmbient());
        double rh = sy.px(wl.getMinAmbient()) - ryTop;
        // Batik doesn't support 8-digit RGBA hex; use an explicit fill-opacity instead.
        s.append(String.format(Locale.US,
                "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' fill='#46a046' fill-opacity='0.40' "
              + "stroke='#2e7d32' stroke-width='1.5'/>", rx, ryTop, rw, rh));

        // Operating point + label
        double px = sx.px(wl.getPointWaterOutlet());
        double py = sy.px(wl.getPointAmbient());
        String ptLabel = String.format(Locale.US, "%.2f; %.2f", wl.getPointWaterOutlet(), wl.getPointAmbient());
        s.append(String.format(Locale.US,
                "<circle cx='%.1f' cy='%.1f' r='4' fill='#1b1b1b'/>", px, py));
        s.append(labelBox(px + 6, py - 10, ptLabel));

        // Axis titles
        s.append(text((plotL + plotR) / 2, H - 8, "middle", "Water Outlet Temperature [°C]"));
        s.append(String.format(Locale.US,
                "<text x='14' y='%.1f' text-anchor='middle' transform='rotate(-90 14 %.1f)' "
              + "font-family='Helvetica,Arial,sans-serif' font-size='10' fill='#333'>Ambient Temperature [°C]</text>",
                (plotT + plotB) / 2, (plotT + plotB) / 2));

        s.append("</svg>");
        return s.toString();
    }

    // --- tiny SVG primitives ---

    private String rect(double x, double y, double w, double h, String fill, String stroke, double sw) {
        return String.format(Locale.US,
                "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' fill='%s' stroke='%s' stroke-width='%.1f'/>",
                x, y, w, h, fill, stroke, sw);
    }

    private String line(double x1, double y1, double x2, double y2, String stroke, double sw) {
        return String.format(Locale.US,
                "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='%.1f'/>",
                x1, y1, x2, y2, stroke, sw);
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
