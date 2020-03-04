package com.microsoft.commondatamodel.objectmodel.storage;

import com.google.common.base.Strings;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 * It is used for handling authentication items for ADLS requests.
 */
class AdlsAdapterAuthenticator {
  private static final String HMAC_SHA256 = "HmacSHA256";
  // The authorization header key, used during shared key auth.
  private static final String HTTP_AUTHORIZATION = "Authorization";
  // The MS date header key, used during shared key auth.
  private static final String HTTP_XMS_DATE = "x-ms-date";
  // The MS version key, used during shared key auth.
  private static final String HTTP_XMS_VERSION = "x-ms-version";

  private final String sharedKey;
  private final String tenant;
  private final String clientId;
  private final String secret;
  private AuthenticationResult lastAuthenticationResult;

  AdlsAdapterAuthenticator(final String sharedKey) {
    if (sharedKey == null) {
      throw new IllegalArgumentException("sharedKey is null");
    }
    this.sharedKey = sharedKey;
    this.clientId = null;
    this.secret = null;
    this.tenant = null;
  }

  AdlsAdapterAuthenticator(final String tenant, final String clientId, final String secret) {
    if ( tenant == null || clientId == null || secret == null) {
      throw new IllegalArgumentException("tenant or clientId or secret is null");
    }
    this.sharedKey = null;
    this.tenant = tenant;
    this.clientId = clientId;
    this.secret = secret;
  }

  /**
   * Build a ADLS request's authentication header
   * @param url The url of the request
   * @param method The method of the request
   * @param content The content of the request
   * @param contentType The contentType of the request
   * @return The authentication headers
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws URISyntaxException
   */
  Map<String, String> buildAuthenticationHeader(
      final String url,
      final String method,
      final String content,
      final String contentType)
      throws NoSuchAlgorithmException, InvalidKeyException, URISyntaxException {
    if (sharedKey != null) {
      return buildAuthenticationHeaderWithSharedKey(url, method, content, contentType);
    }

    return buildAuthenticationHeaderWithClientIdAndSecret();
  }

  /**
   * Returns the authentication headers with the applied shared key.
   * @param url The URL.
   * @param method The HTTP method.
   * @param content The string content.
   * @param contentType The content type.
   * @return The authentication headers
   */
  private Map<String, String> buildAuthenticationHeaderWithSharedKey(
      final String url,
      final String method,
      final String content,
      final String contentType)
      throws URISyntaxException, NoSuchAlgorithmException, InvalidKeyException {
    final Map<String, String> headers = new LinkedHashMap<>();

    // Add UTC now time and new version.
    headers.put(HTTP_XMS_DATE, DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.ofHours(0))));
    headers.put(HTTP_XMS_VERSION, "2018-06-17");
    int contentLength = 0;
    if (content != null) {
      contentLength = content.getBytes().length;
    }
    final URI uri = new URI(url);
    final StringBuilder builder = new StringBuilder();
    builder.append(method).append("\n"); // Verb.yi
    builder.append("\n"); // Content-Encoding.
    builder.append("\n"); // Content-Language.
    builder.append((contentLength != 0) ? contentLength : "").append("\n"); // Content length.
    builder.append("\n"); // Content-md5.
    builder.append(contentType != null ? contentType : "").append("\n"); // Content-type.
    builder.append("\n"); // Date.
    builder.append("\n"); // If-modified-since.
    builder.append("\n"); // If-match.
    builder.append("\n"); // If-none-match.
    builder.append("\n"); // If-unmodified-since.
    builder.append("\n"); // Range.
    for (final Map.Entry<String, String> header : headers.entrySet()) {
      builder.append(header.getKey()).append(":").append(header.getValue()).append("\n");
    }
    // Append canonicalized resource.
    final String accountName = uri.getHost().split("\\.")[0];
    builder.append("/").append(accountName);
    builder.append(uri.getPath());
    // Append canonicalized queries.
    if (!Strings.isNullOrEmpty(uri.getQuery())) {
      final String queryParameters = uri.getQuery();
      final String[] queryParts = queryParameters.split("&");
      for(final String item : queryParts) {
        final String[] keyValuePair = item.split("=");
        builder.append("\n").append(keyValuePair[0]).append(":").append(keyValuePair[1]);
      }
    }

    final Mac sha256_HMAC = Mac.getInstance(HMAC_SHA256);
    final SecretKeySpec secret_key = new SecretKeySpec(Base64.decodeBase64(sharedKey.getBytes()), HMAC_SHA256);
    sha256_HMAC.init(secret_key);

    final String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8)));

    headers.put(HTTP_AUTHORIZATION, "SharedKey " + accountName + ":" + hash);
    return headers;
  }

  /**
   * Build a ADLS request's authentication header with clientId and secret
   * @return The authentication header
   */
  private Map<String, String> buildAuthenticationHeaderWithClientIdAndSecret() {
    final Map<String, String> header = new LinkedHashMap<>();
    if (this.needsRefreshToken()) {
      this.refreshToken();
    }

    header.put("authorization", this.lastAuthenticationResult.getAccessTokenType() + " " + this.lastAuthenticationResult.getAccessToken());
    return header;
  }

  /**
   * If need to refresh authentication token or not.
   * @return If need to refresh authentication token or not.
   */
  private boolean needsRefreshToken() {
    if (lastAuthenticationResult == null) {
      return true;
    }

    Date now = new Date();
    return now.before(this.lastAuthenticationResult.getExpiresOnDate());
  }

  /**
   * Refresh the authentication token.
   */
  private void refreshToken() {
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    AuthenticationContext authenticationContext = null;
    try {
      authenticationContext = new AuthenticationContext("https://login.windows.net/" + this.tenant,
          true,
          executorService);
      final ClientCredential clientCredentials = new ClientCredential(this.clientId, this.secret);

      this.lastAuthenticationResult = authenticationContext.acquireToken("https://storage.azure.com", clientCredentials, null).get();
    } catch (MalformedURLException | InterruptedException | ExecutionException e) {
      throw new StorageAdapterException("Failed to refresh AdlsAdapter's token", e);
    } finally {
      executorService.shutdown();
    }
  }

  String getSharedKey() {
    return sharedKey;
  }

  String getTenant() {
    return tenant;
  }

  String getClientId() {
    return clientId;
  }

  String getSecret() {
    return secret;
  }
}
