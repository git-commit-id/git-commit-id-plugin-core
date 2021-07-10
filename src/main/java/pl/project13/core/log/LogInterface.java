package pl.project13.core.log;

public interface LogInterface {
  void debug(String msg);
  void info(String msg);
  void warn(String msg);
  void error(String msg, Throwable t);
}
