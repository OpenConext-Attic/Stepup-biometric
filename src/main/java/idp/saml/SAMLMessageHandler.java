package idp.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.VelocityEngine;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.saml2.binding.encoding.HTTPPostSimpleSignEncoder;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.Credential;

public class SAMLMessageHandler {

  private static final String SAML_REQUEST_POST_PARAM_NAME = "SAMLRequest";
  private static final String SAML_RESPONSE_POST_PARAM_NAME = "SAMLResponse";

  private static String UNSIGNED_TEMPLATE = "/templates/saml2-post-simplesign-binding.vm";
  private static String SIGNED_TEMPLATE = "/templates/saml2-post-simplesign-binding.vm";

  private final VelocityEngine velocityEngine;

  private final SAMLMessageDecoder decoder;
  private final SecurityPolicyResolver resolver;
  private final String entityId = "http://mock-idp";
  private final boolean signMessage = false;

  public SAMLMessageHandler(VelocityEngine velocityEngine, SAMLMessageDecoder decoder, SecurityPolicyResolver resolver) {
    super();
    this.velocityEngine = velocityEngine;
    this.decoder = decoder;
    this.resolver = resolver;
//    this.entityId = entityId;
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
    if (signMessage) {
      messageContext.setOutboundSAMLMessageSigningCredential(signingCredential);
    }

    messageContext.setOutboundMessageIssuer(entityId);
    messageContext.setRelayState(relayState);
    String template = signMessage ? SIGNED_TEMPLATE : UNSIGNED_TEMPLATE;
    HTTPPostSimpleSignEncoder encoder = new HTTPPostSimpleSignEncoder(velocityEngine, template, true);
    encoder.encode(messageContext);
  }
//
//  public String extractSAMLMessage(HttpServletRequest request) {
//    String parameter = request.getParameter(SAML_REQUEST_POST_PARAM_NAME);
//    return StringUtils.hasText(parameter) ? parameter : request.getParameter(SAML_RESPONSE_POST_PARAM_NAME);
//  }

}