# Password Encryption Maven Project

This Java Maven project implements RSA password encryption using a public key fetched from a REST API endpoint.

## Project Structure

```
password-encryption/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── chongwm/
│   │           └── passwordencryption/
│   │               ├── Main.java
│   │               ├── PublicKeyFetcher.java
│   │               └── PasswordEncryptor.java
│   └── test/
│       └── java/
│           └── chongwm/
│               └── passwordencryption/
│                   └── PasswordEncryptorTest.java
└── README.md
```

## Features

- Fetches RSA public key from REST API endpoint
- Encrypts passwords using RSA/ECB/PKCS1Padding
- Base64 encoding/decoding
- Command-line arguments for password and host
- Configurable API endpoint
- Unit tests included
- Maven build configuration

## Dependencies

- Apache HttpClient 4.5.14 (for REST API calls)
- Jackson 2.15.2 (for JSON parsing)
- JUnit 5.9.3 (for testing)

## Building and Running

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Build the project
```bash
mvn clean compile
```

### Run tests
```bash
mvn test
```

### Create executable JAR
```bash
mvn clean package
```

## Usage

### Command Line Arguments

The application accepts the following arguments:

```bash
java -jar target/password-encryption-1.0-SNAPSHOT.jar <password> [host]
```

**Arguments:**
- `password` (required) - The password to encrypt
- `host` (optional) - The host URL for the public key API
  - Default: `http://jll-dev.exploredoxis.com:8080`

### Examples

```bash
# Using default host
java -jar target/password-encryption-1.0-SNAPSHOT.jar "MySecret123"

# Using custom host
java -jar target/password-encryption-1.0-SNAPSHOT.jar "MySecret123" "http://localhost:8080"

# Using HTTPS host with different port
java -jar target/password-encryption-1.0-SNAPSHOT.jar "MySecret123" "https://prod-server.com:9090"

# Using Maven exec plugin
mvn exec:java -Dexec.mainClass="chongwm.passwordencryption.Main" -Dexec.args="\"MyPassword123\" \"http://localhost:8080\""
```

### Running with Maven

```bash
# With default host
mvn exec:java -Dexec.mainClass="chongwm.passwordencryption.Main" -Dexec.args="\"YourPassword\""

# With custom host
mvn exec:java -Dexec.mainClass="chongwm.passwordencryption.Main" -Dexec.args="\"YourPassword\" \"http://your-host:8080\""
```

## How It Works

The application will:
1. Parse command line arguments for password and optional host
2. Fetch the public key from: `{host}/restws/publicws/rest/api/v1/publicKey`
   - Default host: `http://jll-dev.exploredoxis.com:8080`
3. Encrypt the provided password using RSA encryption
4. Display the encrypted result in Base64 format

## Configuration

### API Endpoint Structure
The API endpoint is constructed as: `{host}/restws/publicws/rest/api/v1/publicKey`

### Supported Host Formats
- `http://hostname:port`
- `https://hostname:port`
- `http://hostname` (default port 80)
- `https://hostname` (default port 443)

The application automatically handles trailing slashes in host URLs.

## API Response Format

The code handles multiple possible JSON response formats:
- Direct string value
- `{"publicKey": "base64-key"}`
- `{"key": "base64-key"}`

Adjust the parsing logic in `PublicKeyFetcher.java` if your API uses a different format.

## Error Handling

The application provides detailed error messages for:
- Missing required arguments
- Invalid host URLs
- Network connection failures
- Invalid API responses
- Encryption failures

## Security Notes

- This implementation uses RSA with PKCS1 padding
- The public key should be in X.509 format (Base64 encoded)
- Always use HTTPS in production environments
- Consider key validation and error handling for production use
- Passwords are passed as command-line arguments (be aware of shell history)
