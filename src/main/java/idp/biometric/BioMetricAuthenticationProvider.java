package idp.biometric;

import idp.saml.SAMLAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static java.util.Collections.singletonList;

public class BioMetricAuthenticationProvider implements AuthenticationProvider {

  private final BioMetric bioMetric;

  public static final DateTimeFormatter formatter =  DateTimeFormatter.ofPattern ("yyyyMMdd'T'HHmmss");

  public BioMetricAuthenticationProvider(BioMetric bioMetric) {
    this.bioMetric = bioMetric;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
    BioMetric.Response response = token.isRegistration() ? bioMetric.registration() : bioMetric.authenticate(token.getNameId());
    return new SAMLAuthenticationToken(token, response, singletonList(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.isAssignableFrom(SAMLAuthenticationToken.class);
  }

}
