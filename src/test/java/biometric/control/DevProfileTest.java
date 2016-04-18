package biometric.control;

import biometric.AbstractIntegrationTest;
import biometric.api.BioMetric;
import biometric.api.mock.MockBioMetric;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@WebIntegrationTest(value = {"server.port=0", "spring.profiles.active=dev", "biometric.api.key=secret", "biometric.api.base.url=http://localhost:9000/"})
public class DevProfileTest extends AbstractIntegrationTest {

  @Autowired
  private BioMetric bioMetric;

  @Test
  public void testMockAuthnRequest() throws Exception {
    //We don't mock the Biometric endpoint, as we will hit both the MockSAMLAuthnFilter and MockBioMetric because of dev-profile
    String url = "http://localhost:" + port;
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    assertTrue(response.getBody().contains("<img id=\"qrcode\" class=\"qr\" src=\"data:image/jpeg;base64,"));

    HttpHeaders httpHeaders = buildCookieHeaders(response);

    response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
    assertTrue(response.getBody().contains("<img id=\"qrcode\" class=\"qr\" src=\"data:image/jpeg;base64,"));

    Map map = restTemplate.getForObject("http://localhost:" + port + "/poll", Map.class);
    assertEquals("pending", map.get("status"));

    assertEquals(1, ((MockBioMetric)bioMetric).getSessions().size());
  }

}
