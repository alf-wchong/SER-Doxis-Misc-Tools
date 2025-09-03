package chongwm.passwordencryption;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import java.util.Base64;

/**
 * Utility class for encrypting passwords using RSA public key encryption
 */
public class PasswordEncryptor
{

	/**
	 * Encrypts a plain text password using the provided RSA public key This method follows the exact logic from the
	 * provided screenshot
	 * 
	 * @param plainText       The password to encrypt
	 * @param publicKeyBase64 The Base64 encoded public key string
	 * @return Base64 encoded encrypted password
	 * @throws Exception if there's an error during encryption
	 */
	public static String encryptPassword(String plainText, String publicKeyBase64) throws Exception
	{
		// Step 1: Decode the Base64 public key (equivalent to Base64.getDecoder().decode(pubKey.getBytes()))
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64.getBytes()));

		// Step 2: Create RSA KeyFactory (equivalent to KeyFactory.getInstance("RSA"))
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");

		// Step 3: Generate the PublicKey object (equivalent to keyFactory.generatePublic(publicKeySpec))
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

		// Step 4: Set up the cipher mode (equivalent to "RSA/ECB/PKCS1Padding")
		String cipherMode = "RSA/ECB/PKCS1Padding";

		// Step 5: Get cipher instance (equivalent to Cipher.getInstance(cipherMode))
		Cipher cipher = Cipher.getInstance(cipherMode);

		// Step 6: Initialize cipher for encryption (equivalent to cipher.init(Cipher.ENCRYPT_MODE, publicKey))
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);

		// Step 7: Encrypt the plaintext (equivalent to cipher.doFinal(plainText.getBytes("UTF-8")))
		byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

		// Step 8: Encode the result to Base64 (equivalent to Base64.getEncoder().encodeToString(encryptedBytes))
		return Base64.getEncoder().encodeToString(encryptedBytes);
	}
}
