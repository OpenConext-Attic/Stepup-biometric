package idp.saml.mock;

import idp.saml.SAMLAuthenticationToken;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

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

    AuthnRequestBuilder authnRequestBuilder = (AuthnRequestBuilder) builderFactory.getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);

    AuthnRequest authnRequest = authnRequestBuilder.buildObject();
    authnRequest.setID(UUID.randomUUID().toString());
    authnRequest.setIssuer(generateIssuer(issuerEntityId));
    authnRequest.setAssertionConsumerServiceURL("http://localhost:8080/assertionConsumerUrl");

    Authentication authentication = authenticationManager.authenticate(new SAMLAuthenticationToken(authnRequest));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    chain.doFilter(request, response);
  }

  private Issuer generateIssuer(String issuingEntityName) {
    IssuerBuilder issuerBuilder = (IssuerBuilder) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
    Issuer issuer = issuerBuilder.buildObject();
    issuer.setValue(issuingEntityName);
    issuer.setFormat(NameIDType.ENTITY);
    return issuer;
  }

}
