package idp.control;

import idp.biometric.BioMetric;
import idp.saml.SAMLAuthenticationToken;
import idp.saml.SAMLMessageHandler;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.signature.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    // otherwise the pie timer reset's when you change the language
    model.put("accrued_time", System.currentTimeMillis() - ((SAMLAuthenticationToken) authentication).getCreationTime().getMillis());

    return new ModelAndView("singleSignOn", model);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/done")
  public void authnResponder(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws IOException, MarshallingException, SignatureException, MessageEncodingException {
    SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
    if (!token.getStatus().equals(BioMetric.PollResponse.complete)) {
      throw new IllegalArgumentException("Biometric authentication has not been completed for " + authentication);
    }
    LOG.debug("Sending response for successful authentication {}", authentication);

    samlMessageHandler.sendAuthnResponse(token, response);
  }

}
