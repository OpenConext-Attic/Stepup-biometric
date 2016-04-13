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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

@RestController
public class PollController {

  private static final Logger LOG = LoggerFactory.getLogger(PollController.class);

  @Autowired
  private BioMetric bioMetric;

  @RequestMapping(method = RequestMethod.GET, value = "/poll")
  public Map<String, String> poll(Authentication authentication) {
    LOG.debug("Poll request for {}", authentication);
    SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
    BioMetric.PollResponse status = bioMetric.poll(token.getBiometricReponse().getSessionID(), token.isRegistration());
    token.setStatus(status);
    LOG.debug("Poll response {} for {}", status, authentication);
    return singletonMap("status", status.name());
  }

}
