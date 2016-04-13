package idp;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;

public class SingleSignSSOTest extends AbstractIntegrationTest {

  @Rule
  public WireMockRule bioMetricMock = new WireMockRule(9000);

  //List<LoggedRequest> requests = findAll(putRequestedFor(urlMatching("/api/.*")));

  @Before
  public void before() throws IOException {
    super.before();
    String json = IOUtils.toString(new ClassPathResource("biometric/registration_response.json").getInputStream());
    bioMetricMock.stubFor(post(urlPathEqualTo("/registration/create")).willReturn(
        aResponse().withStatus(200).withHeader("Content-type", "application/json").withBody(json)
    ));
  }

  @Test
  public void testAuthnRequest() throws Exception {
    String destination = "http://localhost:" + port + "/";

    String url = samlRequestUtils.redirectUrl(entityId, destination, Optional.empty());
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    List<String> cookies = response.getHeaders().get("Set-Cookie");
    assertEquals(1, cookies.size());

    //Something like JSESSIONID=j2qqhxkq9wfy1ngsqouvebxud;Path=/
    String sessionId = cookies.get(0);

    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add("Cookie", sessionId.replaceAll(";.*", ""));

    String secondUrl = samlRequestUtils.redirectUrl(entityId, destination, Optional.empty());
    response = restTemplate.getForEntity(secondUrl, String.class);
    System.out.println(response);
  }

}
