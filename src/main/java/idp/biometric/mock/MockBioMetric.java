package idp.biometric.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import idp.biometric.BioMetric;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MockBioMetric implements BioMetric {

  private ObjectMapper objectMapper = new ObjectMapper();
  private Map<String, Integer> sessions = new HashMap<>();

  @Override
  public Response registration() {
    return fromMap(json("biometric/registration_response.json", Map.class));
  }

  @Override
  public Response authenticate(String uuid) {
    return fromMap(json("biometric/registration_response.json", Map.class));
  }

  @Override
  public PollResponse poll(String sessionID, boolean isRegistration) {
    int count = sessions.getOrDefault(sessionID, 0).intValue();
    if (count == 5) {
      sessions.remove(sessionID);
      return PollResponse.expired;
    } else {
      sessions.put(sessionID, count + 1);
      return PollResponse.pending;
    }
  }

  private Response fromMap(Map<String, String> map) {
    return new Response(
        map.get("sessionID"),
        map.get("expirationTime"),
        map.get("qrCode"),
        map.get("uuid"));
  }

  private <T> T json(String path, Class<T> clazz) {
    try {
      return objectMapper.readValue(new ClassPathResource(path).getInputStream(), clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
