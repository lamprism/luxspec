package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Combines multiple configuration key browsers into one ordered, duplicate-free browser.
 *
 * <p>Every browser is queried for every definition. A browser that does not own a definition should
 * return an empty result.</p>
 *
 * @author RollW
 */
public class CompositeConfigKeyBrowser implements ConfigKeyBrowser {
    private final List<ConfigKeyBrowser> browsers;

    /**
     * Creates a composite browser.
     *
     * @param browsers the browsers to query in iteration order
     */
    public CompositeConfigKeyBrowser(Iterable<? extends ConfigKeyBrowser> browsers) {
        List<ConfigKeyBrowser> registeredBrowsers = new ArrayList<>();
        for (ConfigKeyBrowser browser : Objects.requireNonNull(browsers, "browsers")) {
            registeredBrowsers.add(Objects.requireNonNull(browser, "browser"));
        }
        this.browsers = List.copyOf(registeredBrowsers);
    }

    @Override
    public boolean supports(ConfigSpec<?> spec) {
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        for (ConfigKeyBrowser browser : browsers) {
            if (browser.supports(nonNullSpec)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<ConfigKey> browse(ConfigSpec<?> spec) {
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        Set<ConfigKey> keys = new LinkedHashSet<>();
        for (ConfigKeyBrowser browser : browsers) {
            if (browser.supports(nonNullSpec)) {
                keys.addAll(ConfigKeyBrowser.validate(
                        nonNullSpec,
                        browser.browse(nonNullSpec)
                ));
            }
        }
        return List.copyOf(keys);
    }
}
