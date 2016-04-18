package biometric.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

public class BioMetricErrorHandler extends DefaultResponseErrorHandler {

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    HttpStatus statusCode = response.getStatusCode();
    if (statusCode.equals(HttpStatus.FORBIDDEN)) {
      throw new BioMetricExpiredSessionException(statusCode);
    }
    super.handleError(response);
  }
}
