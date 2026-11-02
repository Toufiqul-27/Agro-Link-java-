package com.agrolinkbd.agrolinkbd;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void hashesAndVerifiesWithoutStoringPlaintext() {
        String hash = PasswordHasher.hash("example-passphrase-123");
        assertTrue(PasswordHasher.isHashed(hash));
        assertNotEquals("example-passphrase-123", hash);
        assertTrue(PasswordHasher.verify("example-passphrase-123", hash));
        assertFalse(PasswordHasher.verify("incorrect-password", hash));
        assertNotEquals(hash, PasswordHasher.hash("example-passphrase-123"));
    }

    @Test void handlesInvalidValuesAndLegacyRecords() {
        assertFalse(PasswordHasher.verify(null, "something"));
        assertFalse(PasswordHasher.verify("password", null));
        assertFalse(PasswordHasher.verify("pass", "$pbkdf2-sha256$not-valid"));
        assertTrue(PasswordHasher.verify("old-pass", "old-pass"));
        assertFalse(PasswordHasher.verify("not-old-pass", "old-pass"));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash("short"));
    }
}
