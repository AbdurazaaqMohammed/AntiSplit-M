/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.apkeditor;

import com.reandroid.jcommand.CommandStringResource;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 *
 * Provides localized strings based on default locale (Locale.getDefault()).
 *
 * The source of strings is on internal resource file as:
 *    /strings/strings[config].properties
 *
 *
 * Config is a combinations of language code and country code, each prefixed by dash (-) character.
 *     -[language]-[country]
 *  eg: -en-US, -en-CA, -de-DE, -ru-RU ...
 *  country is optional can be specified with language only, e.g -en, -de, -ru ...
 *
 * default strings file is:
 *   /strings/strings.properties
 *
 * */
public class ResourceStrings implements CommandStringResource {

    private static final String DEFAULT_CONFIG = "";
    public static final ResourceStrings INSTANCE = new ResourceStrings();

    private Properties defaultProperties;
    private Properties properties;
    private String config;
    private boolean ignoreMissingResource;

    private ResourceStrings() {
    }

    /**
     * Returns localized string
     * @param resourceName unique key ( string name )
     *
     * if the string is not found on current config then it will look on default config,
     * if the string is not found on both configs then it returns resourceName.
     * */
    @Override
    public String getString(String resourceName) {
        Properties prop = getProperties();
        String result = prop.getProperty(resourceName, null);
        if (result == null) {
            prop = getDefaultProperties();
            result = prop.getProperty(resourceName, resourceName);
        }
        return result;
    }

    /**
     * Returns available and best matching config
     * */
    public String getConfig() {
        String config = this.config;
        if(config == null) {
            config = getAvailableConfig(Locale.getDefault());
            this.config = config;
        }
        return config;
    }

    /**
     * Sets preferred locale
     * */
    public void setConfig(Locale locale) {
        setConfig(getAvailableConfig(locale));
    }

    /**
     * Sets preferred config.
     * If config is different from current, it resets resources
     * If the config not available, then value is discarded
     * */
    public void setConfig(String config) {
        if(!ObjectsUtil.equals(this.config, config)) {
            if(!hasConfig(config, null)) {
                config = null;
            }
            this.config = config;
            this.properties = null;
        }
    }
    public void setIgnoreMissingResource(boolean ignoreMissingResource) {
        this.ignoreMissingResource = ignoreMissingResource;
    }

    private String getAvailableConfig(Locale locale) {
        if(locale != null) {
            String lang = locale.getLanguage();
            String country = locale.getCountry();
            if (!hasConfig(lang, country)) {
                country = null;
                if (!hasConfig(lang, null)) {
                    lang = null;
                }
            }
            return toConfig(lang, country);
        }
        return DEFAULT_CONFIG;
    }
    private boolean hasConfig(String lang, String country) {
        boolean result = false;
        String path = toResourcePath(lang, country);
        try {
            InputStream inputStream = ResourceStrings.class.getResourceAsStream(path);
            if(inputStream != null) {
                result = true;
                inputStream.close();
            }
        } catch (Throwable ignored) {
        }
        return result;
    }
    private String toResourcePath(String lang, String country) {
        return "/strings/strings" + toConfig(lang, country) + ".properties";
    }
    private String toConfig(String lang, String country) {
        StringBuilder builder = new StringBuilder();
        if (!StringsUtil.isEmpty(lang)) {
            if (lang.charAt(0) != '-') {
                builder.append('-');
            }
            builder.append(lang);
            if (!StringsUtil.isEmpty(country)) {
                if (country.charAt(0) != '-') {
                    builder.append('-');
                }
                builder.append(country);
            }
        }
        if(builder.length() == 0) {
            return DEFAULT_CONFIG;
        }
        return builder.toString();
    }

    public Properties getProperties() {
        Properties properties = this.properties;
        if(properties == null) {
            String config = getConfig();
            if (DEFAULT_CONFIG.equals(config)) {
                properties = new Properties();
            } else {
                properties = loadProperties(config);
            }
            this.properties = properties;
        }
        return properties;
    }
    private Properties getDefaultProperties() {
        Properties properties = this.defaultProperties;
        if(properties == null) {
            properties = loadProperties(DEFAULT_CONFIG);
            this.defaultProperties = properties;
            initializeDefaultProperties(properties);
        }
        return properties;
    }
    private void initializeDefaultProperties(Properties properties) {
        properties.put("title_app_name_and_version", APKEditor.getName() + " - " + APKEditor.getVersion());
        properties.put("title_app_repo", APKEditor.getRepo());
    }
    private Properties loadProperties(String config) {
        Properties properties = new Properties();
        String path = toResourcePath(config, null);
        InputStream inputStream = ResourceStrings.class.getResourceAsStream(path);
        if(inputStream == null) {
            if(ignoreMissingResource) {
                return properties;
            }
            throw new RuntimeException("Missing resource: '" + path + "'");
        }
        try {
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            try {
                inputStream.close();
            } catch (Throwable ignored) {
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
