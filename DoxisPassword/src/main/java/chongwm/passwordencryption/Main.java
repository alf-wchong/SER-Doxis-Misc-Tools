package chongwm.passwordencryption;

/**
 * Main class demonstrating password encryption using RSA public key fetched from REST API
 * 
 * Usage: java Main <password> [host]
 * 
 * Arguments: password (required) - The password to encrypt host (optional) - The host URL for the public key API
 * Default: http://uk.exploredoxis.com:8080
 * 
 * Examples: java Main "MySecret123" java Main "MySecret123" "http://localhost:8080" java Main "MySecret123"
 * "https://prod-server.com:9090"
 */
public class Main
{

	public static void main(String[] args)
	{
		// Validate arguments
		if (args.length == 0)
		{
			System.err.println("Usage: java Main <password> [host]");
			System.err.println();
			System.err.println("Arguments:");
			System.err.println("  password (required) - The password to encrypt");
			System.err.println("  host (optional)     - The host URL for the public key API");
			System.err.println("                        Default: http://uk.exploredoxis.com:8080");
			System.err.println();
			System.err.println("Examples:");
			System.err.println("  java Main \"MySecret123\"");
			System.err.println("  java Main \"MySecret123\" \"http://localhost:8080\"");
			System.err.println("  java Main \"MySecret123\" \"https://prod-server.com:9090\"");
			System.exit(1);
		}

		try
		{
			// Get password from command line arguments
			String plainTextPassword = args[0];

			// Get host from command line arguments or use default
			String host = null;
			if (args.length > 1)
			{
				host = args[1];
				System.out.println("Using custom host: " + host);
			} else
			{
				System.out.println("Using default host: http://jll-dev.exploredoxis.com:8080");
			}

			// Step 1: Fetch the public key from the REST API
			System.out.println("Fetching public key from REST API...");
			String publicKeyBase64;
			if (host != null)
			{
				publicKeyBase64 = PublicKeyFetcher.fetchPublicKey(host);
			} else
			{
				publicKeyBase64 = PublicKeyFetcher.fetchPublicKey();
			}
			System.out.println("Public key fetched successfully");

			System.out.println("Encrypting password...");

			// Step 2: Encrypt the password using the fetched public key
			String encryptedPassword = PasswordEncryptor.encryptPassword(plainTextPassword, publicKeyBase64);

			// Step 3: Display results
			System.out.println();
			System.out.println("=== ENCRYPTION RESULTS ===");
			System.out.println("Original Password: " + plainTextPassword);
			System.out.println("Encrypted Password: " + encryptedPassword);
			System.out.println("Encryption completed successfully!");

		} catch (Exception e)
		{
			System.err.println("Error during password encryption: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}
