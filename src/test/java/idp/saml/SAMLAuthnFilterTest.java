package idp.saml;

import idp.biometric.BioMetric;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.xml.ConfigurationException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static idp.saml.SAMLBuilder.buildIssuer;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SAMLAuthnFilterTest {

  private SAMLAuthnFilter subject;
  private AuthenticationManager authenticationManager;
  private SAMLMessageHandler samlMessageHandler;

  @BeforeClass
  public static void beforeClass() throws ConfigurationException {
    DefaultBootstrap.bootstrap();
  }

  @Before
  public void before() {
    this.authenticationManager = mock(AuthenticationManager.class);
    this.samlMessageHandler = mock(SAMLMessageHandler.class);
    subject = new SAMLAuthnFilter(this.authenticationManager, this.samlMessageHandler);
  }

  @Test
  public void testDoFilterInternal() throws Exception {
    SAMLAuthenticationToken token = buildToken("19990412T160502");
    SecurityContextHolder.getContext().setAuthentication(token);

    when(this.authenticationManager.authenticate(token)).thenReturn(new TestingAuthenticationToken("principal", "cred", "ROLE_TEST"));

    subject.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

    assertEquals(SecurityContextHolder.getContext().getAuthentication().getPrincipal(), "principal");
  }

  private SAMLAuthenticationToken buildToken(String expirationTime) {
    AuthnRequest authnRequest = mock(AuthnRequest.class);
    when(authnRequest.getAssertionConsumerServiceURL()).thenReturn("http://acs");
    when(authnRequest.getID()).thenReturn("ID");
    when(authnRequest.getIssuer()).thenReturn(buildIssuer("issuer"));
    return new SAMLAuthenticationToken(
        new SAMLAuthenticationToken(authnRequest, "relayState", "127.0.0.1"),
        new BioMetric.Response("sessionID", expirationTime, "qrCode", "uuid"),
        AuthorityUtils.createAuthorityList("ROLE_USER"));
  }
}