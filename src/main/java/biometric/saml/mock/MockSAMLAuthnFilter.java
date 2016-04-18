package biometric.saml.mock;

import biometric.saml.SAMLAuthenticationToken;
import org.opensaml.Configuration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import java.io.IOException;
import java.util.*;

import static biometric.saml.SAMLBuilder.buildSAMLObject;
import static biometric.saml.SAMLBuilder.buildIssuer;

public class MockSAMLAuthnFilter extends GenericFilterBean {

  private XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

  private final String issuerEntityId;
  private final AuthenticationManager authenticationManager;

  public MockSAMLAuthnFilter(String issuerEntityId, AuthenticationManager authenticationManager) {
    this.issuerEntityId = issuerEntityId;
    this.authenticationManager = authenticationManager;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
    if (existingAuth != null && existingAuth instanceof SAMLAuthenticationToken && existingAuth.isAuthenticated()) {
      chain.doFilter(request, response);
      return;
    }

    AuthnRequest authnRequest = buildSAMLObject(AuthnRequest.class,AuthnRequest.DEFAULT_ELEMENT_NAME);
    authnRequest.setID(UUID.randomUUID().toString());
    authnRequest.setIssuer(buildIssuer(issuerEntityId));
    authnRequest.setAssertionConsumerServiceURL("http://localhost:8080/assertionConsumerUrl");

    Authentication authentication = authenticationManager.authenticate(
        new SAMLAuthenticationToken(authnRequest, "relayState", "localhost:8080"));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    chain.doFilter(request, response);
  }

}
