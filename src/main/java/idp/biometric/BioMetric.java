package idp.biometric;

import java.io.Serializable;

public interface BioMetric {

  Response registration();

  Response authenticate(String uuid);

  PollResponse poll(String sessionID, boolean isRegistration);

  enum PollResponse {
    pending, expired, complete
  }

  class Response implements Serializable {
    private final String sessionID;
    private final String expirationTime;
    private final String qrCode;
    private final String uuid;

    public Response(String sessionID, String expirationTime, String qrCode, String uuid) {
      this.sessionID = sessionID;
      this.expirationTime = expirationTime;
      this.qrCode = qrCode;
      this.uuid = uuid;
    }

    public String getSessionID() {
      return sessionID;
    }

    public String getExpirationTime() {
      return expirationTime;
    }

    public String getQrCode() {
      return qrCode;
    }

    public String getUuid() {
      return uuid;
    }

    @Override
    public String toString() {
      return "Response{" +
          "uuid='" + uuid + '\'' +
          ", expirationTime='" + expirationTime + '\'' +
          '}';
    }
  }
}
