package idp;

import org.joda.time.DateTime;
import org.junit.Test;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.binding.decoding.HTTPRedirectDeflateDecoder;
import org.opensaml.saml2.binding.encoding.HTTPRedirectDeflateEncoder;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.util.URLBuilder;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.CredentialResolver;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.security.criteria.UsageCriteria;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.UUID;

public class SAMLRequestUtils {

  private final static XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

  private final CredentialResolver credentialResolver;

  public SAMLRequestUtils(CredentialResolver credentialResolver) {
    this.credentialResolver = credentialResolver;
  }

  /*
   * The OpenSaml API is very verbose..
   */
  @SuppressWarnings("unchecked")
  public String redirectUrl(String entityName, String destination, Optional<String> userId) throws SecurityException, MessageEncodingException, SignatureException, MarshallingException {
    AuthnRequestBuilder authnRequestBuilder = (AuthnRequestBuilder) builderFactory.getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);

    AuthnRequest authnRequest = authnRequestBuilder.buildObject();
    authnRequest.setID(UUID.randomUUID().toString());
    authnRequest.setIssueInstant(new DateTime());
    authnRequest.setDestination(destination);

    authnRequest.setIssuer(createIssuer(entityName));

    Endpoint endpoint = generateEndpoint(SingleSignOnService.DEFAULT_ELEMENT_NAME, destination);

    CriteriaSet criteriaSet = new CriteriaSet();
    criteriaSet.add(new EntityIDCriteria(entityName));
    criteriaSet.add(new UsageCriteria(UsageType.SIGNING));

    Credential signingCredential = credentialResolver.resolveSingle(criteriaSet);

    MockHttpServletResponse response = new MockHttpServletResponse();
    HttpServletResponseAdapter outTransport = new HttpServletResponseAdapter(response, false);

    HTTPRedirectDeflateEncoder encoder = new HTTPRedirectDeflateEncoder() ;

    BasicSAMLMessageContext messageContext = new BasicSAMLMessageContext();

    messageContext.setOutboundMessageTransport(outTransport);
    messageContext.setPeerEntityEndpoint(endpoint);
    messageContext.setOutboundSAMLMessage(authnRequest);

    messageContext.setOutboundSAMLMessageSigningCredential(signingCredential);

    messageContext.setRelayState(null);

    encoder.encode(messageContext);

    return response.getRedirectedUrl();
  }

  private Endpoint generateEndpoint(QName service, String location) {
    SAMLObjectBuilder<Endpoint> endpointBuilder = (SAMLObjectBuilder<Endpoint>) builderFactory.getBuilder(service);
    Endpoint samlEndpoint = endpointBuilder.buildObject();
    samlEndpoint.setLocation(location);
    return samlEndpoint;
  }

  private Issuer createIssuer(String issuingEntityName) {
    Issuer issuer = buildSAMLObject(Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);
    issuer.setValue(issuingEntityName);
    return issuer;
  }

  @SuppressWarnings({"unused", "unchecked"})
  private <T> T buildSAMLObject(final Class<T> objectClass, QName qName) {
    XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
    return (T) builderFactory.getBuilder(qName).buildObject(qName);
  }

}
