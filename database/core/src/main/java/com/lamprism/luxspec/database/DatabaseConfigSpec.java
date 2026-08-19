package com.lamprism.luxspec.database;

import com.lamprism.luxspec.config.ConfigCodec;
import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigSpecBuilder;
import com.lamprism.luxspec.config.policy.ConfigPolicies;
import com.lamprism.luxspec.config.policy.ConfigPolicy;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.message.LocalizedText;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Typed configuration definitions owned by the database capability.
 */
public final class DatabaseConfigSpec {
    private static final ConfigPolicy BOOTSTRAP_ONLY = ConfigPolicies.sourceScope(ConfigSourceScope.BOOTSTRAP);

    public static final ConfigSpec<DatabaseType> TYPE = bootstrap(
            "database.type",
            localized("Database engine used to create the connection pool.", "用于创建连接池的数据库引擎。"),
            ConfigCodecs.serialized(DatabaseType::of, DatabaseType::getName),
            null,
            false
    );
    public static final ConfigSpec<DatabaseTarget> TARGET = bootstrap(
            "database.target",
            localized("Database connection target: memory, file, or a network host and optional port.",
                    "数据库连接目标：内存、文件，或带可选端口的网络主机。"),
            ConfigCodecs.serialized(DatabaseTarget::parse, DatabaseTarget::toString),
            null,
            false
    );
    public static final ConfigSpec<String> NAME = bootstrap(
            "database.name",
            localized("Database name. Network database engines require this value.", "数据库名称。网络数据库引擎必须提供此值。"),
            ConfigCodecs.string(),
            null,
            false
    );
    public static final ConfigSpec<String> USERNAME = bootstrap(
            "database.username",
            localized("Optional username used to authenticate the database connection.", "用于认证数据库连接的可选用户名。"),
            ConfigCodecs.string(),
            null,
            false
    );
    public static final ConfigSpec<String> PASSWORD = bootstrap(
            "database.password",
            localized("Optional password used to authenticate the database connection.", "用于认证数据库连接的可选密码。"),
            ConfigCodecs.string(),
            null,
            true
    );
    public static final ConfigSpec<String> CHARACTER_SET = bootstrap(
            "database.character-set",
            localized("Optional character set requested from database drivers that support it.",
                    "请求支持该能力的数据库驱动使用的可选字符集。"),
            ConfigCodecs.string(),
            null,
            false
    );
    public static final ConfigSpec<List<String>> OPTIONS = bootstrap(
            "database.options",
            localized("Additional driver properties in key=value format. Managed SSL properties are not allowed.",
                    "以 key=value 格式提供的附加驱动属性。不允许配置由系统管理的 SSL 属性。"),
            ConfigCodecs.list(ConfigCodecs.string()),
            List.of(),
            false
    );
    public static final ConfigSpec<SslMode> SSL_MODE = bootstrap(
            "database.ssl.mode",
            localized("SSL mode for database connections. Defaults to disabled.", "数据库连接的 SSL 模式。默认禁用。"),
            ConfigCodecs.enumValue(SslMode.class),
            SslMode.DISABLED,
            false
    );
    public static final ConfigSpec<String> SSL_SERVER_CA = bootstrap(
            "database.ssl.server-ca",
            localized("Optional server CA certificate descriptor using file: or value: syntax.",
                    "可选服务器 CA 证书描述符，使用 file: 或 value: 语法。"),
            ConfigCodecs.string(),
            null,
            true
    );
    public static final ConfigSpec<String> SSL_CLIENT_CERTIFICATE = bootstrap(
            "database.ssl.client-certificate",
            localized("Optional client certificate descriptor using file: or value: syntax.",
                    "可选客户端证书描述符，使用 file: 或 value: 语法。"),
            ConfigCodecs.string(),
            null,
            true
    );
    public static final ConfigSpec<String> SSL_CLIENT_PRIVATE_KEY = bootstrap(
            "database.ssl.client-private-key",
            localized("Optional client private key descriptor using file: or value: syntax.",
                    "可选客户端私钥描述符，使用 file: 或 value: 语法。"),
            ConfigCodecs.string(),
            null,
            true
    );
    public static final ConfigSpec<Integer> POOL_MAXIMUM_SIZE = bootstrap(
            "database.pool.maximum-size",
            localized("Maximum number of connections in the pool. Defaults to 10.", "连接池中的最大连接数。默认值为 10。"),
            ConfigCodecs.integer(),
            10,
            false
    );
    public static final ConfigSpec<Integer> POOL_MINIMUM_IDLE = bootstrap(
            "database.pool.minimum-idle",
            localized("Minimum number of idle connections maintained by the pool. Defaults to 10.",
                    "连接池中保持的最小空闲连接数。默认值为 10。"),
            ConfigCodecs.integer(),
            10,
            false
    );
    public static final ConfigSpec<Duration> POOL_CONNECTION_TIMEOUT = bootstrap(
            "database.pool.connection-timeout",
            localized("Maximum time to wait for a pool connection. Defaults to 30 seconds.",
                    "等待连接池连接的最长时间。默认值为 30 秒。"),
            ConfigCodecs.duration(),
            Duration.ofSeconds(30),
            false
    );
    public static final ConfigSpec<Duration> POOL_IDLE_TIMEOUT = bootstrap(
            "database.pool.idle-timeout",
            localized("Maximum idle connection lifetime. Defaults to 10 minutes.", "连接在池中的最长空闲时间。默认值为 10 分钟。"),
            ConfigCodecs.duration(),
            Duration.ofMinutes(10),
            false
    );
    public static final ConfigSpec<Duration> POOL_MAXIMUM_LIFETIME = bootstrap(
            "database.pool.maximum-lifetime",
            localized("Maximum connection lifetime in the pool. Defaults to 30 minutes.",
                    "连接在池中的最长生命周期。默认值为 30 分钟。"),
            ConfigCodecs.duration(),
            Duration.ofMinutes(30),
            false
    );
    public static final ConfigSpec<Duration> POOL_LEAK_DETECTION_THRESHOLD = bootstrap(
            "database.pool.leak-detection-threshold",
            localized("Connection leak detection threshold. Zero disables detection.", "连接泄漏检测阈值。零表示禁用检测。"),
            ConfigCodecs.duration(),
            Duration.ZERO,
            false
    );

    private DatabaseConfigSpec() {
    }

    public static List<ConfigSpec<?>> all() {
        return List.of(
                TYPE,
                TARGET,
                NAME,
                USERNAME,
                PASSWORD,
                CHARACTER_SET,
                OPTIONS,
                SSL_MODE,
                SSL_SERVER_CA,
                SSL_CLIENT_CERTIFICATE,
                SSL_CLIENT_PRIVATE_KEY,
                POOL_MAXIMUM_SIZE,
                POOL_MINIMUM_IDLE,
                POOL_CONNECTION_TIMEOUT,
                POOL_IDLE_TIMEOUT,
                POOL_MAXIMUM_LIFETIME,
                POOL_LEAK_DETECTION_THRESHOLD
        );
    }

    private static <T> ConfigSpec<T> bootstrap(
            String key,
            LocalizedText description,
            ConfigCodec<T> codec,
            @Nullable T defaultValue,
            boolean sensitive
    ) {
        ConfigSpecBuilder<T> builder = ConfigSpec.builder(key, codec)
                .localizedDescription(description)
                .defaultValue(defaultValue)
                .sensitive(sensitive)
                .policy(BOOTSTRAP_ONLY);
        return builder.build();
    }

    private static LocalizedText localized(String defaultText, String simplifiedChineseText) {
        return LocalizedText.of(defaultText, Locale.SIMPLIFIED_CHINESE, simplifiedChineseText);
    }
}
