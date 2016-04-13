package idp.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.common.SignableSAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.common.binding.encoding.SAMLMessageEncoder;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.SigningUtil;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.CredentialResolver;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.Validator;
import org.opensaml.xml.validation.ValidatorSuite;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import static java.util.Arrays.asList;
import static org.opensaml.xml.Configuration.getValidatorSuite;

public class SAMLMessageHandler {

  private final CredentialResolver credentialResolver;
  private final SAMLMessageEncoder encoder;
  private final SAMLMessageDecoder decoder;
  private final SecurityPolicyResolver resolver;
  private final String entityId;

  private final List<ValidatorSuite> validatorSuites;

  public SAMLMessageHandler(CredentialResolver credentialResolver, SAMLMessageDecoder samlMessageDecoder, SAMLMessageEncoder samlMessageEncoder, SecurityPolicyResolver securityPolicyResolver, String entityId) {
    this.credentialResolver = credentialResolver;
    this.encoder = samlMessageEncoder;
    this.decoder = samlMessageDecoder;
    this.resolver = securityPolicyResolver;
    this.entityId = entityId;
    this.validatorSuites = asList(getValidatorSuite("saml2-core-schema-validator"), getValidatorSuite("saml2-core-spec-validator"));
  }

  public SAMLMessageContext extractSAMLMessageContext(HttpServletRequest request) throws SecurityException, MessageDecodingException {
    BasicSAMLMessageContext messageContext = new BasicSAMLMessageContext();

    messageContext.setInboundMessageTransport(new HttpServletRequestAdapter(request));
    messageContext.setSecurityPolicyResolver(resolver);

    decoder.decode(messageContext);

    return messageContext;
  }

  public void sendSAMLMessage(SignableSAMLObject samlMessage, Endpoint endpoint, HttpServletResponse response, String relayState, Credential signingCredential) throws MessageEncodingException {

    HttpServletResponseAdapter outTransport = new HttpServletResponseAdapter(response, false);

    BasicSAMLMessageContext messageContext = new BasicSAMLMessageContext();

    messageContext.setOutboundMessageTransport(outTransport);
    messageContext.setPeerEntityEndpoint(endpoint);
    messageContext.setOutboundSAMLMessage(samlMessage);
    messageContext.setOutboundSAMLMessageSigningCredential(signingCredential);

    messageContext.setOutboundMessageIssuer(entityId);
    messageContext.setRelayState(relayState);

    encoder.encode(messageContext);
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
    byte[] input = request.getQueryString().replaceFirst("Signature.*&", "").getBytes();
    byte[] signature = Base64.decode(urlDecode(base64signature));

    Credential credential = credential(issuer);
    SigningUtil.verifyWithURI(credential, urlDecode(sigAlg), signature, input);
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

  public void validate(HttpServletRequest request, AuthnRequest authnRequest) {
    try {
      validateXMLObject(authnRequest);
      validateSignature(authnRequest);
      validateRawSignature(request, authnRequest.getIssuer().getValue());
    } catch (ValidationException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

}