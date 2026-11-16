package com.commerceplatform.config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

final class LocalSigningKey {
    private LocalSigningKey() {}

    static byte[] load() throws IOException {
        Path directory = Files.createDirectories(Path.of("data"));
        try (var file = FileChannel.open(directory.resolve(".jwt-signing-key"), CREATE, READ, WRITE);
             var lock = file.lock()) {
            byte[] key = new byte[32];
            ByteBuffer buffer = ByteBuffer.wrap(key);
            if (file.size() == 0) {
                new SecureRandom().nextBytes(key);
                while (buffer.hasRemaining()) file.write(buffer);
                file.force(true);
            } else {
                if (file.size() != key.length) throw new IOException("The local JWT signing key has an unexpected length");
                while (buffer.hasRemaining()) {
                    if (file.read(buffer) < 0) throw new IOException("Incomplete local JWT signing key");
                }
            }
            return key;
        }
    }
}
