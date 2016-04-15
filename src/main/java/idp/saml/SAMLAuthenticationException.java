package idp.saml;

import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.AuthnRequest;
import org.springframework.security.core.AuthenticationException;

public class SAMLAuthenticationException extends AuthenticationException {

  private final SAMLMessageContext messageContext;

  public SAMLAuthenticationException(String msg, Throwable t, SAMLMessageContext messageContext) {
    super(msg, t);
    this.messageContext = messageContext;
  }

  public SAMLMessageContext getMessageContext() {
    return messageContext;
  }
}
