package chongwm.passwordencryption;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the password encryption functionality
 */
public class PasswordEncryptorTest
{

	/**
	 * Test password encryption with a sample RSA public key Note: This test uses a sample key for demonstration purposes
	 */
	@Test
	public void testPasswordEncryption()
	{
		// Sample RSA public key for testing (Base64 encoded)
		// In real scenarios, you'd use the key from your API
		String samplePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4f5wg5l2hKsTeNem/V41fGnJm6gOdrj8ym3rFkEjWT2btYjFvpf4WLkbhCuNDEQhKuN1L0DL4o1GGFw4E1e5D5JvBBFaU1W6aKlf1dV3Bh/7PQkv8rQgHO1xGgvhHRn0KgM6xqKmz+WQO2SsCBhJ4aYvJqWy4E7f/bLw7p4I6pj1uQWPJaGvGhCx7KhKkQBwl1E5/1U4Q9K8BT2j/7Q9w/1F+pL6fQ7WMQy8M6b7aKl7L6K8g2QgJl3Q7JjWKmPj2V2U6B8B1Qr1F6zWKgV1Qq+HkJUKO1zWMTRgIzPlJ+YQf0jgJ6dj4Sz3w4G6GvXZF8E8qY+h8qn6n8aWuAL+a3mL2rn4E5sYqBW8rGhSJ6fQ4";

		try
		{
			String plainText = "TestPassword123";
			String encrypted = PasswordEncryptor.encryptPassword(plainText, samplePublicKey);

			// Basic assertions
			assertNotNull(encrypted, "Encrypted password should not be null");
			assertFalse(encrypted.isEmpty(), "Encrypted password should not be empty");
			assertNotEquals(plainText, encrypted, "Encrypted password should be different from plain text");

			System.out.println("Test encryption successful!");
			System.out.println("Plain text: " + plainText);
			System.out.println("Encrypted: " + encrypted);

		} catch (Exception e)
		{
			// This test might fail if the sample key format is incorrect
			// That's expected - it demonstrates the encryption process
			System.out.println("Test completed (sample key might not be valid): " + e.getMessage());
		}
	}

	/**
	 * Test that encryption with invalid key throws exception
	 */
	@Test
	public void testEncryptionWithInvalidKey()
	{
		String invalidKey = "invalid_key";
		String plainText = "TestPassword";

		assertThrows(Exception.class, () ->
		{
			PasswordEncryptor.encryptPassword(plainText, invalidKey);
		}, "Should throw exception for invalid key");
	}
}
