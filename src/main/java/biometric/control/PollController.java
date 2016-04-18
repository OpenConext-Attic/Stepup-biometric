package biometric.control;

import biometric.api.BioMetric;
import biometric.saml.SAMLAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
