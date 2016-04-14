package idp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.opensaml.xml.security.credential.CredentialResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest(value = {"server.port=0", "biometric.api.key=secret", "biometric.api.base.url=http://localhost:9000/"})
public abstract class AbstractIntegrationTest {

  protected RestTemplate restTemplate = new TestRestTemplate();
  protected ObjectMapper objectMapper = new ObjectMapper();

  @Value("${local.server.port}")
  protected int port;

  @Value("${biometric.entity.id}")
  protected String entityId;

  @Autowired
  private CredentialResolver credentialResolver;

  protected SAMLRequestUtils samlRequestUtils;

  @Rule
  public WireMockRule bioMetricMock = new WireMockRule(9000);

  @Before
  public void before() throws IOException {
    samlRequestUtils = new SAMLRequestUtils(credentialResolver);
  }

}