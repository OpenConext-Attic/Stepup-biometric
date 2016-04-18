package biometric.saml;

import org.opensaml.common.binding.SAMLMessageContext;
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
