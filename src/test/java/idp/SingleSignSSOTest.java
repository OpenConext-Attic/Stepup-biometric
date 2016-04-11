package idp;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class SingleSignSSOTest extends AbstractIntegrationTest {

  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  @Rule
  public WireMockRule serviceProviderMock = new WireMockRule(9090);

  @Test
  public void testAuthnRequest() throws IOException {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    String destination = "http://localhost:" + port + "/singleSignOn";
    map.add("SAMLRequest", new String(Base64.getEncoder().encode(authnRequest(destination).getBytes())));
    ResponseEntity<Void> response = restTemplate.postForEntity(destination, map, Void.class);
    System.out.println(response);
  }

  private String authnRequest(String destination) throws IOException {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    df.setTimeZone(UTC);
    String xml = IOUtils.toString(new ClassPathResource("saml/authnRequest.xml").getInputStream());
    return xml
        .replace("@@Destination@@", destination)
        .replace("@@ID@@", UUID.randomUUID().toString())
        .replace("@@IssueInstant@@", df.format(new Date()));
  }

}
