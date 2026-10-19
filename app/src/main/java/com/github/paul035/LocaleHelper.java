package com.github.paul035;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    public static Context setLocale(Context context, String language) {
        if (language == null || language.isEmpty())
            return context;
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? updateResources(context, language)
                : updateResourcesLegacy(context, language);
    }

    private static Locale toLocale(String language) {
        String[] codes = language.split("-");
        String lang = codes[0];
        String country = codes.length > 1 ? codes[1] : "";
        if (country.startsWith("r") && country.length() > 1)
            country = country.substring(1);
        return country.isEmpty() ? new Locale(lang) : new Locale(lang, country);
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = toLocale(language);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = toLocale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        configuration.setLayoutDirection(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }
}
