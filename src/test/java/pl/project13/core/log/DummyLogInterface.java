package pl.project13.core.log;

public class DummyLogInterface implements LogInterface {
  @Override
  public void debug(String msg) {
    // ignore
  }

  @Override
  public void info(String msg) {
    // ignore
  }

  @Override
  public void warn(String msg) {
    // ignore
  }

  @Override
  public void error(String msg) {
    // ignore
  }

  @Override
  public void error(String msg, Throwable t) {
    // ignore
  }
}
