package idp.saml;

import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.ValidatorSuite;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.opensaml.xml.Configuration.getValidatorSuite;

public class SAMLAuthnFilter extends OncePerRequestFilter {

  private final SAMLMessageHandler samlMessageHandler;
  private final List<ValidatorSuite> validatorSuites;
  private final AuthenticationManager authenticationManager;

  public SAMLAuthnFilter(AuthenticationManager authenticationManager, SAMLMessageHandler samlMessageHandler) {
    this.authenticationManager = authenticationManager;
    this.samlMessageHandler = samlMessageHandler;
    this.validatorSuites = asList(getValidatorSuite("saml2-core-schema-validator"), getValidatorSuite("saml2-core-spec-validator"));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    if (!isSAMLRequest(request) || !authenticationIsRequired()) {
      chain.doFilter(request, response);
      return;
    }
    SAMLMessageContext messageContext;
    try {
      messageContext = samlMessageHandler.extractSAMLMessageContext(request);
    } catch (MessageDecodingException | SecurityException e) {
      throw new RuntimeException(e);
    }
    SAMLObject inboundSAMLMessage = messageContext.getInboundSAMLMessage();
    if (!(inboundSAMLMessage instanceof AuthnRequest)) {
      throw new RuntimeException("Expected inboundSAMLMessage to be AuthnRequest, but actual " + inboundSAMLMessage.getClass());
    }
    AuthnRequest authnRequest = (AuthnRequest) inboundSAMLMessage;
    validate(authnRequest);
    SecurityContextHolder.getContext().setAuthentication(new SAMLAuthenticationToken(authnRequest));

    chain.doFilter(request, response);
  }

  private boolean isSAMLRequest(HttpServletRequest request) {
    return request.getParameter("SAMLRequest") != null;
  }

  private boolean authenticationIsRequired() {
    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
    return existingAuth == null || existingAuth instanceof AnonymousAuthenticationToken || !existingAuth.isAuthenticated();
  }

  private void validate(XMLObject xmlObject) {
    this.validatorSuites.forEach(validatorSuite -> {
      try {
        validatorSuite.validate(xmlObject);
      } catch (ValidationException e) {
        throw new RuntimeException(e);
      }
    });
  }

}
