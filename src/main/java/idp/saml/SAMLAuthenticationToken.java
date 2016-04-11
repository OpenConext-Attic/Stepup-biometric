package idp.saml;

import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Subject;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collection;

public class SAMLAuthenticationToken extends AbstractAuthenticationToken {

  private final AuthnRequest authnRequest;

  public SAMLAuthenticationToken(AuthnRequest authnRequest) {
    super(AuthorityUtils.NO_AUTHORITIES);
    this.authnRequest = authnRequest;
  }

  public SAMLAuthenticationToken(AuthnRequest authnRequest, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.authnRequest = authnRequest;
    setAuthenticated(true);
  }

  public AuthnRequest getAuthnRequest() {
    return authnRequest;
  }

  @Override
  public Object getCredentials() {
    return "N/A";
  }

  @Override
  public Object getPrincipal() {
    Subject subject = authnRequest.getSubject();
    return subject != null && subject.getNameID() != null ? subject.getNameID().toString() : null;
  }
}
