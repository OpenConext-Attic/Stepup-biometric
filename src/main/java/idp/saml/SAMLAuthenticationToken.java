package idp.saml;

import idp.biometric.BioMetric;
import idp.biometric.BioMetric.PollResponse;
import idp.biometric.BioMetricAuthenticationProvider;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Subject;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.time.LocalDateTime;
import java.util.Collection;

import static idp.biometric.BioMetricAuthenticationProvider.formatter;

public class SAMLAuthenticationToken extends AbstractAuthenticationToken {

  private final String assertionConsumerServiceURL;
  private final String id;
  private final String nameId;
  private final String issuer;
  private final boolean registration;
  private final BioMetric.Response biometricReponse;
  private PollResponse status;

  public SAMLAuthenticationToken(AuthnRequest authnRequest) {
    super(AuthorityUtils.NO_AUTHORITIES);
    this.assertionConsumerServiceURL = authnRequest.getAssertionConsumerServiceURL();
    this.id = authnRequest.getID();
    Subject subject = authnRequest.getSubject();
    this.nameId = subject != null ? subject.getNameID().getValue() : null;
    this.issuer = authnRequest.getIssuer().getValue();
    this.registration = nameId == null;
    this.biometricReponse = null;
    this.status = PollResponse.pending;
  }

  public SAMLAuthenticationToken(SAMLAuthenticationToken token, BioMetric.Response response, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.assertionConsumerServiceURL = token.assertionConsumerServiceURL;
    this.id = token.id;
    this.nameId = token.nameId;
    this.issuer = token.issuer;
    this.registration = token.registration;
    this.biometricReponse = response;
    setAuthenticated(true);
    this.status = PollResponse.pending;
  }

  @Override
  public Object getCredentials() {
    return "N/A";
  }

  @Override
  public Object getPrincipal() {
    return biometricReponse.getUuid();
  }

  public String getAssertionConsumerServiceURL() {
    return assertionConsumerServiceURL;
  }

  public String getId() {
    return id;
  }

  public String getIssuer() {
    return issuer;
  }

  public boolean isRegistration() {
    return registration;
  }

  public String getNameId() {
    return nameId;
  }

  public BioMetric.Response getBiometricReponse() {
    return biometricReponse;
  }

  public boolean isExpired() {
    return biometricReponse != null &&
        LocalDateTime.parse(biometricReponse.getExpirationTime(), formatter).isAfter(LocalDateTime.now());
  }

  public PollResponse getStatus() {
    return status;
  }

  public void setStatus(PollResponse status) {
    this.status = status;
  }
}
