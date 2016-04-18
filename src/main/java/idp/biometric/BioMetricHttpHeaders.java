package idp.biometric;

import org.springframework.http.HttpHeaders;

public class BioMetricHttpHeaders extends HttpHeaders {

  public BioMetricHttpHeaders() {
  }

  public BioMetricHttpHeaders(String apiKey) {
    super();
    this.add(HttpHeaders.CONTENT_TYPE, "application/json");
    this.add(HttpHeaders.ACCEPT,  "application/json");
    this.add("apikey", apiKey);
  }

  public BioMetricHttpHeaders sessionHeader(String sessionID) {
    BioMetricHttpHeaders copy = new BioMetricHttpHeaders();
    copy.setAll(this.toSingleValueMap());
    copy.add("sessionid", sessionID);
    return copy;
  }
}
