package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigParameter;
import com.lamprism.luxspec.config.ConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigKeyBrowserTest {
    private final FiniteConfigKeyBrowser finiteBrowser = new FiniteConfigKeyBrowser();

    @Test
    void browsesFixedDefinitions() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "application.name",
                ConfigCodecs.string(),
                null,
                false
        );

        assertEquals(
                List.of(ConfigKey.of("application.name")),
                keys(finiteBrowser.browse(spec))
        );
    }

    @Test
    void expandsFiniteParametersInExpressionOrder() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "service.{region}.{tier}.name",
                List.of(
                        new ConfigParameter("region", Set.of("us", "eu")),
                        new ConfigParameter("tier", Set.of("pro", "free"))
                ),
                ConfigCodecs.string(),
                null,
                false
        );

        assertEquals(
                List.of(
                        ConfigKey.of("service.eu.free.name"),
                        ConfigKey.of("service.eu.pro.name"),
                        ConfigKey.of("service.us.free.name"),
                        ConfigKey.of("service.us.pro.name")
                ),
                keys(finiteBrowser.browse(spec))
        );
    }

    @Test
    void skipsDefinitionsWithUnconstrainedParameters() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "tenant.{name}.name",
                List.of(new ConfigParameter("name", Set.of())),
                ConfigCodecs.string(),
                null,
                false
        );

        assertEquals(List.of(), keys(finiteBrowser.browse(spec)));
        assertFalse(finiteBrowser.supports(spec));
    }

    @Test
    void composesBrowsersAndRemovesDuplicateKeys() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "application.{name}",
                List.of(new ConfigParameter("name", Set.of("name", "display-name"))),
                ConfigCodecs.string(),
                null,
                false
        );
        ConfigKey first = ConfigKey.of("application.name");
        ConfigKey second = ConfigKey.of("application.display-name");
        ConfigKeyBrowser firstBrowser = ignored -> List.of(first, second);
        ConfigKeyBrowser secondBrowser = ignored -> List.of(first);

        ConfigKeyBrowser composite = ConfigKeyBrowser.compose(List.of(firstBrowser, secondBrowser));

        assertEquals(List.of(first, second), keys(composite.browse(spec)));
    }

    @Test
    void fallbackUsesTheFirstApplicableBrowser() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "application.name",
                ConfigCodecs.string(),
                null,
                false
        );
        ConfigKey first = ConfigKey.of("application.name");
        ConfigKeyBrowser firstBrowser = new ConfigKeyBrowser() {
            @Override
            public boolean supports(ConfigSpec<?> candidate) {
                return false;
            }

            @Override
            public Iterable<ConfigKey> browse(ConfigSpec<?> candidate) {
                return List.of(first);
            }
        };
        ConfigKeyBrowser secondBrowser = ignored -> List.of(first);

        ConfigKeyBrowser fallback = ConfigKeyBrowser.fallback(List.of(firstBrowser, secondBrowser));

        assertEquals(List.of(first), keys(fallback.browse(spec)));
    }

    @Test
    void fallbackStopsOnAnApplicableEmptyResult() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "application.name",
                ConfigCodecs.string(),
                null,
                false
        );
        ConfigKeyBrowser emptyBrowser = new ConfigKeyBrowser() {
            @Override
            public boolean supports(ConfigSpec<?> candidate) {
                return true;
            }

            @Override
            public Iterable<ConfigKey> browse(ConfigSpec<?> candidate) {
                return List.of();
            }
        };
        ConfigKeyBrowser secondBrowser = ignored -> List.of(ConfigKey.of("application.name"));

        ConfigKeyBrowser fallback = ConfigKeyBrowser.fallback(List.of(emptyBrowser, secondBrowser));

        assertEquals(List.of(), keys(fallback.browse(spec)));
    }

    @Test
    void rejectsKeysThatDoNotMatchTheDefinition() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "application.name",
                ConfigCodecs.string(),
                null,
                false
        );
        ConfigKeyBrowser browser = ignored -> List.of(ConfigKey.of("application.other"));
        ConfigKeyBrowser composite = ConfigKeyBrowser.union(List.of(browser));

        assertThrows(IllegalArgumentException.class, () -> composite.browse(spec));
    }

    @Test
    void rejectsNullBrowsersAndResults() {
        List<ConfigKeyBrowser> browsers = new ArrayList<>();
        browsers.add(null);
        assertThrows(NullPointerException.class, () -> ConfigKeyBrowser.compose(browsers));

        ConfigKeyBrowser browser = ignored -> null;
        ConfigKeyBrowser composite = ConfigKeyBrowser.compose(List.of(browser));
        ConfigSpec<String> spec = ConfigSpec.of(
                "application.name",
                ConfigCodecs.string(),
                null,
                false
        );

        assertThrows(NullPointerException.class, () -> composite.browse(spec));
    }

    private static List<ConfigKey> keys(Iterable<ConfigKey> values) {
        List<ConfigKey> result = new ArrayList<>();
        for (ConfigKey value : values) {
            result.add(value);
        }
        return List.copyOf(result);
    }
}
