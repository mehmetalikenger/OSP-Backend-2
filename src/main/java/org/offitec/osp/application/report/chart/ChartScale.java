package org.offitec.osp.application.report.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps a data range onto a pixel range and produces "nice" axis ticks.
 * Small helper shared by the working-limit and pressure-drop SVG builders.
 */
public class ChartScale {

    private final double dataLo, dataHi;
    private final double pxLo, pxHi;

    public ChartScale(double dataLo, double dataHi, double pxLo, double pxHi) {
        // Guard against a degenerate (zero-width) domain.
        if (dataHi <= dataLo) dataHi = dataLo + 1;
        this.dataLo = dataLo;
        this.dataHi = dataHi;
        this.pxLo = pxLo;
        this.pxHi = pxHi;
    }

    /** Map a data value to a pixel coordinate. */
    public double px(double v) {
        double t = (v - dataLo) / (dataHi - dataLo);
        return pxLo + t * (pxHi - pxLo);
    }

    public double lo() { return dataLo; }
    public double hi() { return dataHi; }

    /** ~`target` evenly spaced ticks rounded to a nice step. */
    public List<Double> ticks(int target) {
        double span = dataHi - dataLo;
        double rawStep = span / Math.max(target, 1);
        double step = niceStep(rawStep);
        double start = Math.ceil(dataLo / step) * step;
        List<Double> out = new ArrayList<>();
        for (double v = start; v <= dataHi + step * 1e-6; v += step) {
            // Snap -0.0 and floating dust.
            out.add(Math.abs(v) < step * 1e-6 ? 0.0 : v);
        }
        return out;
    }

    private static double niceStep(double raw) {
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double norm = raw / mag;
        double nice;
        if (norm <= 1) nice = 1;
        else if (norm <= 2) nice = 2;
        else if (norm <= 5) nice = 5;
        else nice = 10;
        return nice * mag;
    }

    /** Tick label: integer when whole, else one decimal. */
    public static String label(double v) {
        if (v == Math.rint(v)) return String.format(Locale.US, "%.0f", v);
        return String.format(Locale.US, "%.1f", v);
    }
}
