package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.SslMaterial;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Materializes file or inline SSL content as driver-readable PEM files and Java key stores.
 *
 * <p>Inline files are owner-readable and owner-writable where the host file system exposes those
 * permissions. Temporary files are deleted when the returned artifact is closed.</p>
 *
 * @author RollW
 */
public class DefaultSslMaterializer implements SslMaterializer {
    private static final String KEY_STORE_TYPE = "PKCS12";
    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();

    @Override
    public SslMaterialArtifact materialize(String name, SslMaterial material) {
        String prefix = temporaryPrefix(name);
        SslMaterial nonNullMaterial = Objects.requireNonNull(material, "material");
        if (nonNullMaterial.getSource() == SslMaterial.Source.FILE) {
            return new ManagedSslMaterialArtifact(
                    Objects.requireNonNull(nonNullMaterial.getPath(), "material path")
                            .toAbsolutePath()
                            .normalize(),
                    List.of()
            );
        }

        Path path = null;
        try {
            path = Files.createTempFile(prefix, ".pem");
            restrictPermissions(path);
            Files.writeString(path, nonNullMaterial.getValue(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException exception) {
            deleteAfterFailure(path);
            throw new IllegalArgumentException("Unable to materialize database SSL material", exception);
        }
        Path materialPath = path;
        return new ManagedSslMaterialArtifact(materialPath, List.of(
                () -> Files.deleteIfExists(materialPath)
        ));
    }

    @Override
    public KeyStoreArtifact materializeTrustStore(
            String name,
            SslMaterial serverCaMaterial
    ) {
        SslMaterial nonNullMaterial = Objects.requireNonNull(serverCaMaterial, "serverCaMaterial");
        String password = createPassword();
        try {
            KeyStore keyStore = newKeyStore();
            List<X509Certificate> certificates = readCertificates(nonNullMaterial);
            for (int index = 0; index < certificates.size(); index++) {
                keyStore.setCertificateEntry("server-ca-" + index, certificates.get(index));
            }
            return writeKeyStore(name, keyStore, password);
        } catch (IOException | GeneralSecurityException | RuntimeException exception) {
            throw materializationFailure("server CA", exception);
        }
    }

    @Override
    public KeyStoreArtifact materializeClientKeyStore(
            String name,
            SslMaterial clientCertificate,
            SslMaterial clientPrivateKey
    ) {
        SslMaterial nonNullCertificate = Objects.requireNonNull(clientCertificate, "clientCertificate");
        SslMaterial nonNullPrivateKey = Objects.requireNonNull(clientPrivateKey, "clientPrivateKey");
        String password = createPassword();
        try {
            KeyStore keyStore = newKeyStore();
            List<X509Certificate> certificates = readCertificates(nonNullCertificate);
            PrivateKey privateKey = readPrivateKey(nonNullPrivateKey);
            keyStore.setKeyEntry(
                    "client",
                    privateKey,
                    password.toCharArray(),
                    certificates.toArray(Certificate[]::new)
            );
            return writeKeyStore(name, keyStore, password);
        } catch (IOException | GeneralSecurityException | RuntimeException exception) {
            throw materializationFailure("client certificate", exception);
        }
    }

    private static String temporaryPrefix(String name) {
        String value = Objects.requireNonNull(name, "name").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        StringBuilder prefix = new StringBuilder("luxspec-db-");
        for (int index = 0; index < value.length() && prefix.length() < 24; index++) {
            char character = value.charAt(index);
            if (Character.isLetterOrDigit(character) || character == '-') {
                prefix.append(Character.toLowerCase(character));
            }
        }
        while (prefix.length() < 3) {
            prefix.append('x');
        }
        return prefix.toString();
    }

    private static void restrictPermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));
        } catch (UnsupportedOperationException exception) {
            path.toFile().setReadable(false, false);
            path.toFile().setWritable(false, false);
            path.toFile().setExecutable(false, false);
            boolean readable = path.toFile().setReadable(true, true);
            boolean writable = path.toFile().setWritable(true, true);
            if (!readable || !writable) {
                throw new IOException("Unable to restrict temporary SSL material permissions", exception);
            }
        }
    }

    private static void deleteAfterFailure(@Nullable Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupException) {
            // The original materialization failure is more useful to the caller.
        }
    }

    private static KeyStore newKeyStore() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        keyStore.load(null, null);
        return keyStore;
    }

    private static KeyStoreArtifact writeKeyStore(
            String name,
            KeyStore keyStore,
            String password
    ) throws GeneralSecurityException, IOException {
        Path path = null;
        try {
            path = Files.createTempFile(temporaryPrefix(name), ".p12");
            restrictPermissions(path);
            try (OutputStream output = Files.newOutputStream(path)) {
                keyStore.store(output, password.toCharArray());
            }
            Path materialPath = path;
            return new KeyStoreArtifact(
                    materialPath,
                    KEY_STORE_TYPE,
                    password,
                    List.of(() -> Files.deleteIfExists(materialPath))
            );
        } catch (IOException | GeneralSecurityException | RuntimeException exception) {
            deleteAfterFailure(path);
            throw exception;
        }
    }

    private static List<X509Certificate> readCertificates(SslMaterial material)
            throws IOException, GeneralSecurityException {
        byte[] bytes = readBytes(material);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates;
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            certificates = certificateFactory.generateCertificates(input);
        }
        List<X509Certificate> values = new ArrayList<>();
        for (Certificate certificate : certificates) {
            if (!(certificate instanceof X509Certificate x509Certificate)) {
                throw new GeneralSecurityException("SSL material is not an X.509 certificate");
            }
            values.add(x509Certificate);
        }
        if (values.isEmpty()) {
            throw new GeneralSecurityException("SSL material does not contain a certificate");
        }
        return List.copyOf(values);
    }

    private static PrivateKey readPrivateKey(SslMaterial material)
            throws IOException, GeneralSecurityException {
        PemBlock pemBlock = PemBlock.read(readBytes(material));
        if (!pemBlock.label().equals("PRIVATE KEY")) {
            throw new GeneralSecurityException(
                    "Client private key must use an unencrypted PKCS8 PRIVATE KEY block"
            );
        }
        InvalidKeySpecException failure = null;
        for (String algorithm : List.of("RSA", "EC", "DSA")) {
            try {
                return KeyFactory.getInstance(algorithm)
                        .generatePrivate(new PKCS8EncodedKeySpec(pemBlock.content()));
            } catch (InvalidKeySpecException exception) {
                failure = exception;
            }
        }
        throw new GeneralSecurityException("Unsupported PKCS8 private key algorithm", failure);
    }

    private static byte[] readBytes(SslMaterial material) throws IOException {
        if (material.getSource() == SslMaterial.Source.FILE) {
            return Files.readAllBytes(Objects.requireNonNull(material.getPath(), "material path"));
        }
        return material.getValue().getBytes(StandardCharsets.US_ASCII);
    }

    private static String createPassword() {
        byte[] randomBytes = new byte[24];
        PASSWORD_RANDOM.nextBytes(randomBytes);
        StringBuilder password = new StringBuilder(randomBytes.length * 2);
        for (byte randomByte : randomBytes) {
            int value = randomByte & 0xff;
            password.append(Character.forDigit(value >>> 4, 16));
            password.append(Character.forDigit(value & 0x0f, 16));
        }
        return password.toString();
    }

    private static IllegalArgumentException materializationFailure(
            String materialKind,
            Exception exception
    ) {
        return new IllegalArgumentException(
                "Unable to materialize " + materialKind + " into a PKCS12 key store",
                exception
        );
    }

    private record PemBlock(String label, byte[] content) {
        private static PemBlock read(byte[] bytes) {
            String text = new String(bytes, StandardCharsets.US_ASCII).trim();
            if (!text.startsWith("-----BEGIN ")) {
                return new PemBlock("PRIVATE KEY", bytes);
            }
            int labelStart = "-----BEGIN ".length();
            int markerEnd = text.indexOf("-----", labelStart);
            if (markerEnd < 0) {
                throw new IllegalArgumentException("SSL PEM material has an invalid begin marker");
            }
            String label = text.substring(labelStart, markerEnd);
            String endMarker = "-----END " + label + "-----";
            int endMarkerStart = text.indexOf(endMarker, markerEnd + "-----".length());
            if (endMarkerStart < 0) {
                throw new IllegalArgumentException("SSL PEM material has no matching end marker");
            }
            String encoded = text.substring(markerEnd + "-----".length(), endMarkerStart);
            StringBuilder base64 = new StringBuilder(encoded.length());
            for (int index = 0; index < encoded.length(); index++) {
                char character = encoded.charAt(index);
                if (Character.isWhitespace(character)) {
                    continue;
                }
                if (!isBase64Character(character)) {
                    throw new IllegalArgumentException(
                            "Encrypted or malformed PEM material is not supported"
                    );
                }
                base64.append(character);
            }
            if (base64.isEmpty()) {
                throw new IllegalArgumentException("SSL PEM material has an empty body");
            }
            try {
                return new PemBlock(label, Base64.getDecoder().decode(base64.toString()));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("SSL PEM material has an invalid body", exception);
            }
        }

        private static boolean isBase64Character(char character) {
            return character >= 'A' && character <= 'Z'
                    || character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '+'
                    || character == '/'
                    || character == '=';
        }
    }

}
