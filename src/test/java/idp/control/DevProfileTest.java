package idp.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import idp.AbstractIntegrationTest;
import idp.biometric.BioMetric;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@WebIntegrationTest(value = {"server.port=0", "spring.profiles.active=dev", "biometric.api.key=secret", "biometric.api.base.url=http://localhost:9000/"})
public class DevProfileTest extends AbstractIntegrationTest {

  @Test
  public void testMockAuthnRequest() throws Exception {
    //We don't mock the Biometric endpoint, as we will hit both the MockSAMLAuthnFilter and MockBioMetric because of dev-profile
    ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port, String.class);
    assertTrue(response.getBody().contains("<img id=\"qrcode\" class=\"qr\" src=\"data:image/jpeg;base64,"));

    Map map = restTemplate.getForObject("http://localhost:" + port + "/poll", Map.class);
    assertEquals("pending", map.get("status"));
  }

}
