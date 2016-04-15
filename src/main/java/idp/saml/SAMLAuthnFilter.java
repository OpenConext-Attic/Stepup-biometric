package idp.saml;

import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.AuthnRequest;
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
      if (isExpired() && request.getHeader("X-POLLING") == null) {
        Authentication authentication = authenticationManager.authenticate(SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
      chain.doFilter(request, response);
      return;
    }
    /**
     * The SAMLRequest parameters are urlEncoded and the extraction expects unencoded parameters
     */
    SAMLMessageContext messageContext = samlMessageHandler.extractSAMLMessageContext(new ParameterDecodingHttpServletRequestWrapper(request));

    AuthnRequest authnRequest = (AuthnRequest) messageContext.getInboundSAMLMessage();

    SAMLAuthenticationToken token = new SAMLAuthenticationToken(authnRequest, messageContext.getRelayState(), request.getRemoteAddr());
    Authentication authentication = authenticationManager.authenticate(token);
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
