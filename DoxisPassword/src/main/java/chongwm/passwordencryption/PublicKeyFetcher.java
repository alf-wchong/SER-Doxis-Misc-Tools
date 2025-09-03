package chongwm.passwordencryption;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to fetch the public key from the REST API
 */
public class PublicKeyFetcher
{

	private static final String DEFAULT_HOST = "http://jll-dev.exploredoxis.com:8080";
	private static final String API_PATH = "/restws/publicws/rest/api/v1/publicKey";

	/**
	 * Fetches the public key from the REST API endpoint using default host
	 * 
	 * @return Base64 encoded public key string
	 * @throws Exception if there's an error fetching or parsing the key
	 */
	public static String fetchPublicKey() throws Exception
	{
		return fetchPublicKey(DEFAULT_HOST);
	}

	/**
	 * Fetches the public key from the REST API endpoint using specified host
	 * 
	 * @param host The host URL (e.g., "http://jll-dev.exploredoxis.com:8080")
	 * @return Base64 encoded public key string
	 * @throws Exception if there's an error fetching or parsing the key
	 */
	public static String fetchPublicKey(String host) throws Exception
	{
		// Ensure host doesn't end with slash and construct full URL
		String cleanHost = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
		String fullUrl = cleanHost + API_PATH;

		System.out.println("Fetching public key from: " + fullUrl);

		try (CloseableHttpClient httpClient = HttpClients.createDefault())
		{
			HttpGet request = new HttpGet(fullUrl);
			request.addHeader("accept", "application/json");

			try (CloseableHttpResponse response = httpClient.execute(request))
			{
				if (response.getStatusLine().getStatusCode() != 200)
				{
					throw new RuntimeException("Failed to fetch public key. HTTP status: " + response.getStatusLine().getStatusCode() + " from URL: " + fullUrl);
				}

				String jsonString = EntityUtils.toString(response.getEntity());
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode jsonNode = objectMapper.readTree(jsonString);

				// The API might return the key directly as a string or in a field
				// Adjust this line based on the actual API response structure
				if (jsonNode.isTextual())
				{
					return jsonNode.asText();
				} else if (jsonNode.has("publicKey"))
				{
					return jsonNode.get("publicKey").asText();
				} else if (jsonNode.has("key"))
				{
					return jsonNode.get("key").asText();
				} else
				{
					// If the response is just the key value, try to get it
					return jsonNode.toString().replace("\"", "");
				}
			}
		}
	}
}
