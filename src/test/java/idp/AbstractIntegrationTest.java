package idp;

import org.junit.Before;
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
@WebIntegrationTest(value = {"server.port=0", "spring.profiles.active=dev","biometric.api.key=secret", "biometric.api.base.url=http://localhost:9000"})
public abstract class AbstractIntegrationTest {

  protected RestTemplate restTemplate = new TestRestTemplate();

  @Value("${local.server.port}")
  protected int port;

  @Value("${biometric.entity.id}")
  protected String entityId;

  @Autowired
  private CredentialResolver credentialResolver;

  protected SAMLRequestUtils samlRequestUtils;

  @Before
  public void before() throws IOException {
    samlRequestUtils = new SAMLRequestUtils(credentialResolver);
  }

}