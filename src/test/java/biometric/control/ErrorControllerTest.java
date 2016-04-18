package biometric.control;

import biometric.AbstractIntegrationTest;
import org.junit.Test;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.signature.SignatureException;
import org.springframework.http.*;

import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ErrorControllerTest extends AbstractIntegrationTest {

  @Test
  public void testDefaultErrorHandling() {
    ResponseEntity<Void> response = restTemplate.getForEntity("http://localhost:" + port, Void.class);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  public void testSAMLAuthenticationException() throws UnknownHostException, SecurityException, SignatureException, MarshallingException, MessageEncodingException {
    String url = samlRequestUtils.redirectUrl(entityId, "http://localhost:" + port + "/", Optional.empty());
    String mangledUrl = url.replaceFirst("&Signature[^&]+", "&Signature=bogus");
    ResponseEntity<String> response = restTemplate.getForEntity(mangledUrl, String.class);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

    Matcher matcher = Pattern.compile("name=\"SAMLResponse\" value=\"(.*?)\"").matcher(response.getBody());
    assertTrue(matcher.find());

    String saml = new String(Base64.getDecoder().decode(matcher.group(1)));

    assertTrue(saml.contains("Exception during validation of AuthnRequest"));
    assertFalse(saml.contains("Subject"));
  }
}
