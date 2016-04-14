package idp.control;

import idp.AbstractIntegrationTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class MetadataControllerTest extends AbstractIntegrationTest {

  @Test
  public void testMetadata() {
    String metadata = restTemplate.getForObject("http://localhost:" + port + "/metadata", String.class);
    assertTrue(metadata.contains("<ds:X509Certificate>MIIDEzCCAfugAwIBAgIJAKoK/heBjcOYMA0GC"));
    assertTrue(metadata.contains("entityID=\"http://biometric-idp\""));
    assertTrue(metadata.contains("<md:EntityDescriptor "));
  }

}