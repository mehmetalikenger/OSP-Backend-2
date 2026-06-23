package org.offitec.osp.application.report;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Localized label provider for the unit selection PDF report.
 *
 * Labels live in {@code report/i18n/report.properties} (English base) with a
 * {@code report_de.properties} German override; any key missing in German falls
 * back to the English base. The whole bundle is exposed as a flat {@code Map} so
 * the Thymeleaf template can reference labels by key (e.g. {@code ${m.t.projectInformation}}).
 */
@Component
public class ReportMessages {

    private static final String BUNDLE = "report.i18n.report";

    /** Maps a UI language code ("en"/"de", case-insensitive) to a report Locale. English is the default. */
    public static Locale toLocale(String language) {
        if (language != null && language.trim().toLowerCase(Locale.ROOT).startsWith("de")) {
            return Locale.GERMAN;
        }
        return Locale.ENGLISH;
    }

    /** All report labels for the given locale, copied into a plain map for template access. */
    public Map<String, String> labels(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, locale);
        Map<String, String> map = new HashMap<>();
        for (String key : bundle.keySet()) {
            map.put(key, bundle.getString(key));
        }
        return map;
    }
}
