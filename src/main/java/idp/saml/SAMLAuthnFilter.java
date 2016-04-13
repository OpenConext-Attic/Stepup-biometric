package idp.saml;

import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.SigningUtil;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SAMLAuthnFilter extends OncePerRequestFilter {

  private final SAMLMessageHandler samlMessageHandler;
  private final AuthenticationManager authenticationManager;

  public SAMLAuthnFilter(AuthenticationManager authenticationManager, SAMLMessageHandler samlMessageHandler) {
    this.authenticationManager = authenticationManager;
    this.samlMessageHandler = samlMessageHandler;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    if (!isSAMLRequest(request) || !authenticationIsRequired()) {
      chain.doFilter(request, response);
      return;
    }
    if (isExpired() && request.getHeader("X-POLLING") == null) {
      Authentication authentication = authenticationManager.authenticate(SecurityContextHolder.getContext().getAuthentication());
      SecurityContextHolder.getContext().setAuthentication(authentication);
      chain.doFilter(request, response);
      return;
    }

    SAMLMessageContext messageContext;
    try {
      /*
       * The SAMLRequest parameters are urlEncoded and the extraction expected unencoded parameters
       */
      messageContext = samlMessageHandler.extractSAMLMessageContext(new ParameterDecodingHttpServletRequestWrapper(request));
    } catch (MessageDecodingException | SecurityException e) {
      throw new RuntimeException(e);
    }

    SAMLObject inboundSAMLMessage = messageContext.getInboundSAMLMessage();
    if (!(inboundSAMLMessage instanceof AuthnRequest)) {
      throw new RuntimeException("Expected inboundSAMLMessage to be AuthnRequest, but actual " + inboundSAMLMessage.getClass());
    }

    AuthnRequest authnRequest = (AuthnRequest) inboundSAMLMessage;

    samlMessageHandler.validate(request, authnRequest);

    Authentication authentication = authenticationManager.authenticate(new SAMLAuthenticationToken(authnRequest));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    chain.doFilter(request, response);
  }

  private boolean isSAMLRequest(HttpServletRequest request) {
    return request.getParameter("SAMLRequest") != null;
  }

  private boolean authenticationIsRequired() {
    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
    return existingAuth == null || existingAuth instanceof AnonymousAuthenticationToken || !existingAuth.isAuthenticated();
  }

  private boolean isExpired() {
    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
    return existingAuth != null && existingAuth instanceof SAMLAuthenticationToken && ((SAMLAuthenticationToken) existingAuth).isExpired();
  }

}
