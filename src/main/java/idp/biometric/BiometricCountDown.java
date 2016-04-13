package idp.biometric;

public class BiometricCountDown {

  private final long start;
  private final long end;
  private final long now;

  public BiometricCountDown() {
    this.start = System.currentTimeMillis();
    this.end = (start + (10 * 60 * 1000)) ;
    this.now = start;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  public long getNow() {
    return now;
  }
}
