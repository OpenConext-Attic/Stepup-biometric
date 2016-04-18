package biometric.api;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonMap;

public class DefaultBioMetric implements BioMetric {

  private final BioMetricHttpHeaders httpHeaders;
  private final String biometricApiBaseUrl;

  private RestTemplate restTemplate = new RestTemplate();

  public DefaultBioMetric(BioMetricHttpHeaders httpHeaders, String biometricApiBaseUrl) {
    this.httpHeaders = httpHeaders;
    this.biometricApiBaseUrl = biometricApiBaseUrl;
    restTemplate.setErrorHandler(new BioMetricErrorHandler());
  }

  @Override
  public Response registration() {
    String uuid = UUID.randomUUID().toString().toUpperCase();
    return doGetResponse(singletonMap("userID", uuid), true, uuid);
  }

  @Override
  public Response authenticate(String uuid) {
    return doGetResponse(singletonMap("biometric", "face"), false, uuid);
  }

  @Override
  public PollResponse poll(String sessionID, boolean isRegistration) {
    BioMetricHttpHeaders sessionHeader = httpHeaders.sessionHeader(sessionID);
    HttpEntity<?> requestEntity = new HttpEntity<>(sessionHeader);
    String action = isRegistration ? "registration" : "authentication";
    try {
      Map<String, String> body = restTemplate.exchange(biometricApiBaseUrl + action + "/poll", HttpMethod.GET, requestEntity, Map.class).getBody();
      return PollResponse.valueOf(body.get("status"));
    } catch (BioMetricExpiredSessionException e) {
      return PollResponse.expired;
    }
  }

  private Response doGetResponse(Map<String, String> body, boolean isRegistration, String uuid) {
    HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, httpHeaders);
    String action = isRegistration ? "registration" : "authentication";
    Map<String, String> result = restTemplate.exchange(biometricApiBaseUrl + action + "/create", HttpMethod.POST, requestEntity, Map.class).getBody();
    return new Response(
        result.get("sessionID"),
        result.get("expirationTime"),
        result.get("qrCode"),
        uuid);
  }
}
