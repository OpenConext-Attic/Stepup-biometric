package idp.biometric;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

public class BioMetricExpiredSessionException extends HttpStatusCodeException {

  public BioMetricExpiredSessionException(HttpStatus statusCode) {
    super(statusCode);
  }
}
