/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Selects the first applicable configuration key browser.
 *
 * <p>An applicable browser owns the complete result. An empty result therefore stops fallback and
 * is not treated as an indication to try the next browser.</p>
 *
 * @author RollW
 */
public class FallbackConfigKeyBrowser implements ConfigKeyBrowser {
    private final List<ConfigKeyBrowser> browsers;

    /**
     * Creates an ordered fallback browser.
     *
     * @param browsers the browsers in priority order
     */
    public FallbackConfigKeyBrowser(Iterable<? extends ConfigKeyBrowser> browsers) {
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
        for (ConfigKeyBrowser browser : browsers) {
            if (browser.supports(nonNullSpec)) {
                return ConfigKeyBrowser.validate(
                        nonNullSpec,
                        browser.browse(nonNullSpec)
                );
            }
        }
        return List.of();
    }
}
