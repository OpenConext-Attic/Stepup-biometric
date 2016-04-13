package idp.control;

import idp.biometric.BioMetric;
import idp.saml.SAMLAuthenticationToken;
import idp.saml.SAMLMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SingleSignOnController {

  private static final Logger LOG = LoggerFactory.getLogger(SingleSignOnController.class);

  @Autowired
  private SAMLMessageHandler samlMessageHandler;

  @Value("${sa.home}")
  private String strongAuthenticationHome;

  @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/")
  public ModelAndView singleSignOn(Authentication authentication) {
    LOG.debug("SingleSignOn request for {}", authentication);
    Map<String, Object> model = new HashMap<>();
    model.put("authentication", authentication);
    model.put("sa_home", strongAuthenticationHome);
    return new ModelAndView("singleSignOn", model);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/done")
  public void authnResponder(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws IOException {
    SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
    if (!token.getStatus().equals(BioMetric.PollResponse.complete)) {
      throw new IllegalArgumentException("Biometric authentication has not been completed for " + authentication);
    }
    LOG.debug("Sending response for successful authentication {}", authentication);
    response.setStatus(200);
    response.flushBuffer();
    //endError(HttpServletResponse.SC_FORBIDDEN);

//    AuthnRequestInfo info = (AuthnRequestInfo) request.getSession().getAttribute(AuthnRequestInfo.class.getName());
//
//    if (info == null) {
//      logger.warn("Could not find AuthnRequest on the request.  Responding with SC_FORBIDDEN.");
//      response.sendError(HttpServletResponse.SC_FORBIDDEN);
//      return;
//    }
//
//    logger.debug("AuthnRequestInfo: {}", info);
//
//    SimpleAuthentication authToken = (SimpleAuthentication) SecurityContextHolder.getContext().getAuthentication();
//    DateTime authnInstant = new DateTime(request.getSession().getCreationTime());
//
//    CriteriaSet criteriaSet = new CriteriaSet();
//    criteriaSet.add(new EntityIDCriteria(idpConfiguration.getEntityID()));
//    criteriaSet.add(new UsageCriteria(UsageType.SIGNING));
//    Credential signingCredential = null;
//    try {
//      signingCredential = credentialResolver.resolveSingle(criteriaSet);
//    } catch (org.opensaml.xml.biometric.SecurityException e) {
//      logger.warn("Unable to resolve EntityID while signing", e);
//      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//      return;
//    }
//    Validate.notNull(signingCredential);
//    Signature signature = (Signature) Configuration.getBuilderFactory().getBuilder(Signature.DEFAULT_ELEMENT_NAME)
//    .buildObject(Signature.DEFAULT_ELEMENT_NAME);
//
//    signature.setSigningCredential(signingCredential);
//    signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
//    signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
//
//    authnRequest.setSignature(signature);
//
//    Configuration.getMarshallerFactory().getMarshaller(authnRequest).marshall(authnRequest);
//
//    Signer.signObject(signature);

//    AuthnResponseGenerator authnResponseGenerator = new AuthnResponseGenerator(signingCredential, idpConfiguration.getEntityID(), timeService, idService, idpConfiguration);
//    EndpointGenerator endpointGenerator = new EndpointGenerator();
//
//    final String remoteIP = request.getRemoteAddr();
//    String attributeJson = null;
//
//    if (null != request.getCookies()) {
//      for (Cookie current : request.getCookies()) {
//        if (current.getName().equalsIgnoreCase("mujina-attr")) {
//          logger.info("Found a attribute cookie, this is used for the assertion response");
//          attributeJson = URLDecoder.decode(current.getValue(), "UTF-8");
//        }
//      }
//    }
//    String acsEndpointURL = info.getAssertionConsumerURL();
//    if (idpConfiguration.getAcsEndpoint() != null) {
//      acsEndpointURL = idpConfiguration.getAcsEndpoint().getUrl();
//    }
//    Response authResponse = authnResponseGenerator.generateAuthnResponse(remoteIP, authToken, acsEndpointURL,
//        responseValidityTimeInSeconds, info.getAuthnRequestID(), authnInstant, attributeJson, info.getEntityId());
//    Endpoint endpoint = endpointGenerator.generateEndpoint(org.opensaml.saml2.metadata.AssertionConsumerService.DEFAULT_ELEMENT_NAME,
//        acsEndpointURL, null);
//
//    request.getSession().removeAttribute(AuthnRequestInfo.class.getName());
//
//    String relayState = request.getParameter("RelayState");
//
//    //we could use a different adapter to send the response based on request issuer...
//    try {
//      adapter.sendSAMLMessage(authResponse, endpoint, response, relayState, signingCredential);
//    } catch (MessageEncodingException mee) {
//      logger.error("Exception encoding SAML message", mee);
//      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
//    }
  }

}
