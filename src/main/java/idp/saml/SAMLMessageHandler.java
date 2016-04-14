package idp.saml;

import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.common.binding.encoding.SAMLMessageEncoder;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.SigningUtil;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.CredentialResolver;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.signature.*;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.ValidatorSuite;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.UUID;

import static idp.saml.SAMLBuilder.*;
import static java.util.Arrays.asList;
import static org.opensaml.xml.Configuration.getValidatorSuite;

public class SAMLMessageHandler {

  private final CredentialResolver credentialResolver;
  private final SAMLMessageEncoder encoder;
  private final SAMLMessageDecoder decoder;
  private final SecurityPolicyResolver resolver;
  private final String entityId;
  private final String spEntityId;
  private final String spMetaDataUrl;

  private final List<ValidatorSuite> validatorSuites;

  public SAMLMessageHandler(CredentialResolver credentialResolver,
                            SAMLMessageDecoder samlMessageDecoder,
                            SAMLMessageEncoder samlMessageEncoder,
                            SecurityPolicyResolver securityPolicyResolver,
                            String entityId,
                            String spEntityId,
                            String spMetaDataUrl) {
    this.credentialResolver = credentialResolver;
    this.encoder = samlMessageEncoder;
    this.decoder = samlMessageDecoder;
    this.resolver = securityPolicyResolver;
    this.entityId = entityId;
    this.spEntityId = spEntityId;
    this.spMetaDataUrl = spMetaDataUrl;
    this.validatorSuites = asList(getValidatorSuite("saml2-core-schema-validator"), getValidatorSuite("saml2-core-spec-validator"));
  }

  public SAMLMessageContext extractSAMLMessageContext(HttpServletRequest request) throws SecurityException, MessageDecodingException {
    BasicSAMLMessageContext messageContext = new BasicSAMLMessageContext();

    messageContext.setInboundMessageTransport(new HttpServletRequestAdapter(request));
    messageContext.setSecurityPolicyResolver(resolver);

    decoder.decode(messageContext);

    SAMLObject inboundSAMLMessage = messageContext.getInboundSAMLMessage();
    if (!(inboundSAMLMessage instanceof AuthnRequest)) {
      throw new RuntimeException("Expected inboundSAMLMessage to be AuthnRequest, but actual " + inboundSAMLMessage.getClass());
    }

    AuthnRequest authnRequest = (AuthnRequest) inboundSAMLMessage;
    validate(request, authnRequest);
    return messageContext;
  }

  public void sendAuthnResponse(SAMLAuthenticationToken token, HttpServletResponse response) throws MarshallingException, SignatureException, MessageEncodingException {
    Credential signingCredential = credential(entityId);

    Response authResponse = buildSAMLObject(Response.class, Response.DEFAULT_ELEMENT_NAME);
    Issuer issuer = buildIssuer(entityId);

    authResponse.setIssuer(issuer);
    authResponse.setID(UUID.randomUUID().toString());
    authResponse.setIssueInstant(new DateTime());
    authResponse.setInResponseTo(token.getId());

    Assertion assertion = buildAssertion(token, entityId, spEntityId, spMetaDataUrl);
    signAssertion(assertion, signingCredential);

    authResponse.getAssertions().add(assertion);
    authResponse.setDestination(token.getAssertionConsumerServiceURL());

    authResponse.setStatus(buildStatus(StatusCode.SUCCESS_URI));

    Endpoint endpoint = buildSAMLObject(Endpoint.class, SingleSignOnService.DEFAULT_ELEMENT_NAME);
    endpoint.setLocation(token.getAssertionConsumerServiceURL());

    HttpServletResponseAdapter outTransport = new HttpServletResponseAdapter(response, false);

    BasicSAMLMessageContext messageContext = new BasicSAMLMessageContext();

    messageContext.setOutboundMessageTransport(outTransport);
    messageContext.setPeerEntityEndpoint(endpoint);
    messageContext.setOutboundSAMLMessage(authResponse);
    messageContext.setOutboundSAMLMessageSigningCredential(signingCredential);

    messageContext.setOutboundMessageIssuer(entityId);
    messageContext.setRelayState(token.getRelayState());

    encoder.encode(messageContext);
  }

  private void signAssertion(Assertion assertion, Credential signingCredential) throws MarshallingException, SignatureException {
    Signature signature = buildSAMLObject(Signature.class, Signature.DEFAULT_ELEMENT_NAME);

    signature.setSigningCredential(signingCredential);
    signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
    signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

    assertion.setSignature(signature);

    Configuration.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
    Signer.signObject(signature);
  }

  public void validate(HttpServletRequest request, AuthnRequest authnRequest) {
    try {
      validateXMLObject(authnRequest);
      validateSignature(authnRequest);
      validateRawSignature(request, authnRequest.getIssuer().getValue());
    } catch (ValidationException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }


  private void validateXMLObject(XMLObject xmlObject) throws ValidationException {
    //lambda is poor with Exceptions
    for (ValidatorSuite validatorSuite : validatorSuites) {
      validatorSuite.validate(xmlObject);
    }
  }

  private void validateRawSignature(HttpServletRequest request, String issuer) throws SecurityException {
    String base64signature = request.getParameter("Signature");
    String sigAlg = request.getParameter("SigAlg");
    if (base64signature == null || sigAlg == null) {
      return;
    }
    byte[] input = request.getQueryString().replaceFirst("&Signature[^&]+", "").getBytes();
    byte[] signature = Base64.decode(base64signature);

    Credential credential = credential(issuer);
    SigningUtil.verifyWithURI(credential, sigAlg, signature, input);
  }

  private void validateSignature(AuthnRequest authnRequest) throws ValidationException {
    Signature signature = authnRequest.getSignature();
    if (signature == null) {
      return;
    }
    new SAMLSignatureProfileValidator().validate(signature);
    String issuer = authnRequest.getIssuer().getValue();
    Credential credential = credential(issuer);
    new SignatureValidator(credential).validate(signature);
  }

  private Credential credential(String entityId) {
    try {
      return credentialResolver.resolveSingle(new CriteriaSet(new EntityIDCriteria(entityId)));
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    }
  }

}