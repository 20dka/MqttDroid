package lightjockey.mqttdroid.ui.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

import lightjockey.mqttdroid.MqttDroidApp;
import lightjockey.mqttdroid.R;

public class LocaleHelper {
    private static final String LANGUAGE_ENGLISH = "en";
    private static final String LANGUAGE_CHINESE = "zh";
    private static final String LANGUAGE_SYSTEM = "system";

    public static Context setLocale(Context context, String languageCode) {
        Locale locale;
        
        if (LANGUAGE_SYSTEM.equals(languageCode)) {
            // Use system default locale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locale = Resources.getSystem().getConfiguration().getLocales().get(0);
            } else {
                locale = Resources.getSystem().getConfiguration().locale;
            }
        } else if (LANGUAGE_CHINESE.equals(languageCode)) {
            locale = new Locale("zh", "CN");
        } else {
            // Default to English
            locale = Locale.ENGLISH;
        }

        return updateResources(context, locale);
    }

    private static Context updateResources(Context context, Locale locale) {
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
            return context;
        }
    }

    public static String getLanguageCode(Context context) {
        return MqttDroidApp.sharedPrefs.getString(
            context.getString(R.string.pref_key_language),
            LANGUAGE_SYSTEM
        );
    }

    public static String getCurrentLanguageDisplayName(Context context) {
        String languageCode = getLanguageCode(context);
        Resources resources = context.getResources();
        
        if (LANGUAGE_SYSTEM.equals(languageCode)) {
            return resources.getString(R.string.language_system);
        } else if (LANGUAGE_CHINESE.equals(languageCode)) {
            return resources.getString(R.string.language_chinese);
        } else {
            return resources.getString(R.string.language_english);
        }
    }
}
