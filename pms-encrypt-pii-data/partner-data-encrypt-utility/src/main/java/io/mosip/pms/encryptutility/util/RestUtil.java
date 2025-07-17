package io.mosip.pms.encryptutility.util;

import com.google.gson.Gson;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;
import io.mosip.pms.encryptutility.constants.ErrorCode;
import io.mosip.pms.encryptutility.dto.Metadata;
import io.mosip.pms.encryptutility.dto.SecretKeyRequest;
import io.mosip.pms.encryptutility.dto.TokenRequestDTO;
import io.mosip.pms.encryptutility.exception.PartnerEncryptUtilityException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Component
public class RestUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestUtil.class);

	private static final String AUTHORIZATION = "Authorization=";
	private static final String BEARER = "Bearer ";
	private static final String COOKIE = HttpHeaders.COOKIE;
	private static final String TOKEN_PROPERTY = "token";

	@Autowired
	private Environment environment;

	/**
	 * Build RestTemplate that trusts all SSL certs.
	 */
	public RestTemplate getRestTemplate() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		TrustStrategy trustAllCerts = (chain, authType) -> true;
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
				SSLContexts.custom().loadTrustMaterial(null, trustAllCerts).build()
		);

		org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager connectionManager =
				org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
						.setSSLSocketFactory(sslSocketFactory)
						.build();

		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

		return new RestTemplate(factory);
	}

	/**
	 * Executes a secure POST API call.
	 */
	public <T> T postApi(String apiUrl, List<String> pathSegments, String queryParamName,
						 String queryParamValue, MediaType mediaType,
						 Object requestBody, Class<T> responseClass) {

		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl);

			Optional.ofNullable(pathSegments)
					.ifPresent(paths -> paths.stream()
							.filter(StringUtils::isNotEmpty)
							.forEach(builder::pathSegment));

			if (StringUtils.isNotEmpty(queryParamName) && StringUtils.isNotEmpty(queryParamValue)) {
				String[] keys = queryParamName.split(",");
				String[] values = queryParamValue.split(",");
				for (int i = 0; i < Math.min(keys.length, values.length); i++) {
					builder.queryParam(keys[i], values[i]);
				}
			}

			HttpEntity<Object> entity = buildRequestEntity(requestBody, mediaType);
			return getRestTemplate().postForObject(builder.toUriString(), entity, responseClass);

		} catch (Exception e) {
			LOGGER.error("API call failed for URL: {}", apiUrl, e);
			throw new PartnerEncryptUtilityException(
					ErrorCode.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
					ErrorCode.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage() + " Error: " + e.getMessage()
			);
		}
	}

	/**
	 * Constructs HttpEntity with headers and token.
	 */
	private HttpEntity<Object> buildRequestEntity(Object body, MediaType mediaType)
			throws java.io.IOException, NoSuchAlgorithmException, KeyStoreException {

		HttpHeaders headers = new HttpHeaders();
		String token = getToken();

		headers.add(COOKIE, token);
		headers.add(HttpHeaders.AUTHORIZATION, token.replace(AUTHORIZATION, BEARER));
		if (mediaType != null) {
			headers.setContentType(mediaType);
		}

		if (body instanceof HttpEntity<?>) {
			HttpEntity<?> entityBody = (HttpEntity<?>) body;
			headers.putAll(entityBody.getHeaders());
			return new HttpEntity<>(entityBody.getBody(), headers);
		}

		return new HttpEntity<>(body, headers);
	}

	/**
	 * Returns valid token or regenerates a new one.
	 */
	private String getToken() throws java.io.IOException, NoSuchAlgorithmException, KeyStoreException {
		String token = System.getProperty(TOKEN_PROPERTY);
		String issuerUrl = environment.getProperty("pms.cert.service.token.request.issuerUrl");
		String clientId = environment.getProperty("pms.cert.service.token.request.clientId");

		if (StringUtils.isNotEmpty(token) &&
				TokenHandlerUtil.isValidBearerToken(token, issuerUrl, clientId)) {
			return AUTHORIZATION + token;
		}

		return AUTHORIZATION + regenerateToken();
	}

	/**
	 * Regenerates token via issuer API call.
	 */
	private String regenerateToken() throws java.io.IOException {
		Gson gson = new Gson();

		TokenRequestDTO<SecretKeyRequest> tokenRequest = new TokenRequestDTO<>();
		tokenRequest.setMetadata(new Metadata());
		tokenRequest.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
		tokenRequest.setRequest(buildSecretKeyRequest());

		String issuerUrl = environment.getProperty("pms.cert.service.token.request.issuerUrl");

		HttpPost post = new HttpPost(issuerUrl);
		post.setEntity(new StringEntity(gson.toJson(tokenRequest)));
		post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			return client.execute(post, response -> {
				Header[] cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
				if (cookies.length == 0) {
					throw new java.io.IOException("Empty cookie: token not received.");
				}

				String cookieValue = cookies[0].getValue();
				int tokenStartIndex = cookieValue.indexOf('=') + 1;
				int tokenEndIndex = cookieValue.indexOf(';');
				String token = cookieValue.substring(tokenStartIndex, tokenEndIndex);

				System.setProperty(TOKEN_PROPERTY, token);
				return token;
			});
		}
	}

	/**
	 * Builds SecretKeyRequest from environment.
	 */
	private SecretKeyRequest buildSecretKeyRequest() {
		SecretKeyRequest request = new SecretKeyRequest();
		request.setAppId(environment.getProperty("mosip.pmp.auth.appId"));
		request.setClientId(environment.getProperty("mosip.pmp.auth.clientId"));
		request.setSecretKey(environment.getProperty("mosip.pmp.auth.secretKey"));
		return request;
	}
}
