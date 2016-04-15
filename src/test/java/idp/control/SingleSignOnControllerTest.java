package idp.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import idp.AbstractIntegrationTest;
import idp.biometric.BioMetric;
import idp.biometric.BioMetric.PollResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;

import java.io.IOException;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SingleSignOnControllerTest extends AbstractIntegrationTest {

  @Test
  public void testRegistrationProcess() throws Exception {
    stubBiometricRegistration();

    String destination = "http://localhost:" + port + "/";

    String url = samlRequestUtils.redirectUrl(entityId, destination, Optional.empty());
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    HttpHeaders httpHeaders = buildCookieHeaders(response);

    String secondUrl = samlRequestUtils.redirectUrl(entityId, destination, Optional.empty());
    response = restTemplate.exchange(secondUrl, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);

    assertTrue(response.getBody().contains("<img id=\"qrcode\" class=\"qr\" src=\"data:image/jpeg;base64,"));

    List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching("/registration/create")));
    // because we used the cookie the second time no registration is done
    assertEquals(1, requests.size());

    bioMetricMock.stubFor(get(urlPathEqualTo("/registration/poll")).willReturn(aResponse().withStatus(403)));
    Map map = restTemplate.exchange("http://localhost:" + port + "/poll", HttpMethod.GET, new HttpEntity<>(httpHeaders), Map.class).getBody();
    assertEquals("expired", map.get("status"));

    stubBiometricRegistrationPoll(PollResponse.complete);
    map = restTemplate.exchange("http://localhost:" + port + "/poll", HttpMethod.GET, new HttpEntity<>(httpHeaders), Map.class).getBody();
    assertEquals("complete", map.get("status"));

    response = restTemplate.exchange("http://localhost:" + port + "/done", HttpMethod.POST, new HttpEntity<>(httpHeaders), String.class);
    assertEquals(200, response.getStatusCode().value());

    String html = response.getBody();
    //this is the POST of the SAMLResponse
    assertTrue(html.contains("<input type=\"hidden\" name=\"SAMLResponse\""));
    assertTrue(html.contains("<body onload=\"document.forms[0].submit()\">"));
  }

  @Test
  public void testAuthenticationProcess() throws Exception {
    stubBiometricAuthentication();

    String destination = "http://localhost:" + port + "/";

    String url = samlRequestUtils.redirectUrl(entityId, destination, Optional.of(UUID.randomUUID().toString()));
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertEquals(200, response.getStatusCode().value());

    List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching("/authentication/create")));
    assertEquals(1, requests.size());

    HttpHeaders headers = buildCookieHeaders(response);

    stubBiometricAuthenticationPoll(PollResponse.pending);
    Map map = restTemplate.exchange("http://localhost:" + port + "/poll", HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
    assertEquals("pending", map.get("status"));
  }

  private void stubBiometricRegistration() throws IOException {
    doMockBiometric("registration");
  }

  private void stubBiometricAuthentication() throws IOException {
    doMockBiometric("authentication");
  }

  private void doMockBiometric(String action) throws IOException {
    String json = IOUtils.toString(new ClassPathResource("biometric/registration_response.json").getInputStream());
    bioMetricMock.stubFor(post(urlPathEqualTo("/" + action + "/create")).willReturn(
        aResponse().withStatus(200).withHeader("Content-type", "application/json").withBody(json)
    ));
  }

  private void stubBiometricRegistrationPoll(PollResponse response) throws JsonProcessingException {
    doMockBiometricPoll("registration", response);
  }

  private void stubBiometricAuthenticationPoll(PollResponse response) throws JsonProcessingException {
    doMockBiometricPoll("authentication", response);
  }

  private void doMockBiometricPoll(String action, PollResponse response) throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(singletonMap("status", response.name()));
    bioMetricMock.stubFor(get(urlPathEqualTo("/" + action + "/poll")).willReturn(
        aResponse().withStatus(200).withHeader("Content-type", "application/json").withBody(json)
    ));
  }
}
