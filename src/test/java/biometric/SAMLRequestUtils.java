package biometric;

import org.joda.time.DateTime;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.saml2.binding.encoding.HTTPRedirectDeflateEncoder;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.CredentialResolver;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.security.criteria.UsageCriteria;
import org.opensaml.xml.signature.SignatureException;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

import static biometric.saml.SAMLBuilder.buildSAMLObject;
import static biometric.saml.SAMLBuilder.buildIssuer;
import static biometric.saml.SAMLBuilder.buildSubject;

public class SAMLRequestUtils {

  private final CredentialResolver credentialResolver;

  public SAMLRequestUtils(CredentialResolver credentialResolver) {
    this.credentialResolver = credentialResolver;
  }

  /*
   * The OpenSAML API is very verbose..
   */
  @SuppressWarnings("unchecked")
  public String redirectUrl(String entityName, String destination, Optional<String> userId) throws SecurityException, MessageEncodingException, SignatureException, MarshallingException, UnknownHostException {
    AuthnRequest authnRequest = buildSAMLObject(AuthnRequest.class, AuthnRequest.DEFAULT_ELEMENT_NAME);
    authnRequest.setID(UUID.randomUUID().toString());
    authnRequest.setIssueInstant(new DateTime());
    authnRequest.setDestination(destination);
    authnRequest.setAssertionConsumerServiceURL("http://localhost/acs");

    authnRequest.setIssuer(buildIssuer(entityName));

    if (userId.isPresent()) {
      Subject subject = buildSubject(userId.get(), "http://localhost:8080", UUID.randomUUID().toString(), InetAddress.getLocalHost().toString());
      authnRequest.setSubject(subject);
    }

    Endpoint endpoint = buildSAMLObject(Endpoint.class, SingleSignOnService.DEFAULT_ELEMENT_NAME);
    endpoint.setLocation(destination);

    MockHttpServletResponse response = new MockHttpServletResponse();
    HttpServletResponseAdapter outTransport = new HttpServletResponseAdapter(response, false);

    HTTPRedirectDeflateEncoder encoder = new HTTPRedirectDeflateEncoder();

    BasicSAMLMessageContext messageContext = new BasicSAMLMessageContext();

    messageContext.setOutboundMessageTransport(outTransport);
    messageContext.setPeerEntityEndpoint(endpoint);
    messageContext.setOutboundSAMLMessage(authnRequest);

    CriteriaSet criteriaSet = new CriteriaSet();
    criteriaSet.add(new EntityIDCriteria(entityName));
    criteriaSet.add(new UsageCriteria(UsageType.SIGNING));

    Credential signingCredential = credentialResolver.resolveSingle(criteriaSet);

    messageContext.setOutboundSAMLMessageSigningCredential(signingCredential);

    messageContext.setRelayState(null);

    encoder.encode(messageContext);

    return response.getRedirectedUrl();
  }

}
