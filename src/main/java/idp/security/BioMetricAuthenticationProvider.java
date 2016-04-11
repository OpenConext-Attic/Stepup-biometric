package idp.security;

import idp.saml.SAMLAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

public class BioMetricAuthenticationProvider implements AuthenticationProvider {
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
    //TODO
    return new SAMLAuthenticationToken(token.getAuthnRequest(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.isAssignableFrom(SAMLAuthenticationToken.class);
  }
}
