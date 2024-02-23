/*
 * This file is part of git-commit-id-plugin-core by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin-core.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles waiting for a {@link Process} and reading its stdout and stderr output.
 */
class ProcessHandler implements AutoCloseable {
  private final Process process;

  private final ExecutorService outputReaderExecutor;
  private final Future<Void> stdoutFuture;
  private final Future<String> stderrFuture;

  private String stderrOutput = null;

  /**
   * @param process the process which should be handled
   * @param stdoutLineConsumer called asynchronously with the lines read from stdout. The consumer
   *        must either be thread-safe, or the result it is building must only be used after
   *        {@link #exitValue(long, TimeUnit)} has returned without throwing an exception. The
   *        consumer must not block, otherwise this could prevent the process from writing any
   *        output, causing it to get stuck.
   */
  public ProcessHandler(Process process, Consumer<String> stdoutLineConsumer) {
    this.process = Objects.requireNonNull(process);
    Objects.requireNonNull(stdoutLineConsumer);

    // 2 threads, one for stdout, one for stderr
    // The process output is consumed concurrently by separate threads because otherwise the process
    // could get stuck if the output is not consumed and the output buffer is full
    ThreadFactory threadFactory = Executors.defaultThreadFactory();
    outputReaderExecutor = Executors.newFixedThreadPool(2, runnable -> {
      Thread t = threadFactory.newThread(runnable);
      // Don't prevent JVM exit
      t.setDaemon(true);
      return t;
    });

    String processInfo;
    try {
      processInfo = this.process.info().command().orElse("?") + " [" + this.process.pid() + "]";
    } catch (UnsupportedOperationException e) {
      processInfo = "<unknown-process>";
    }
    stdoutFuture =
        outputReaderExecutor.submit(new ProcessOutputReader<>("stdout reader (" + processInfo + ")",
            this.process.getInputStream(), stdoutLineConsumer,
            // Don't create a 'result', `stdoutLineConsumer` will do that itself if needed
            () -> null));

    StringBuilder stderrBuilder = new StringBuilder();
    stderrFuture =
        outputReaderExecutor.submit(new ProcessOutputReader<>("stderr reader (" + processInfo + ")",
            this.process.getErrorStream(), line -> stderrBuilder.append(line).append('\n'),
            stderrBuilder::toString));
  }

  /**
   * Waits for the process to finish and returns the exit value.
   *
   * @throws TimeoutException if waiting for the process to finish times out
   */
  public int exitValue(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException, ExecutionException {
    boolean finished = process.waitFor(timeout, timeUnit);
    if (finished) {

      outputReaderExecutor.shutdown();
      try {
        stdoutFuture.get();
      } catch (ExecutionException e) {
        throw new ExecutionException("Failed waiting for stdout", e.getCause());
      }
      try {
        stderrOutput = stderrFuture.get();
      } catch (ExecutionException e) {
        throw new ExecutionException("Failed waiting for stderr", e.getCause());
      }
      return process.exitValue();
    }
    throw new TimeoutException();
  }

  /**
   * Gets the stderr output. Must only be called after {@link #exitValue(long, TimeUnit)} has
   * returned successfully.
   */
  public String getStderr() {
    if (stderrOutput == null) {
      throw new IllegalStateException("Process has not finished");
    }
    return stderrOutput;
  }

  @Override
  public void close() {
    // Perform clean-up; has no effect if process or executor have already been stopped
    process.destroy();
    outputReaderExecutor.shutdownNow();
  }

  private static class ProcessOutputReader<T> implements Callable<T> {
    private final String threadName;
    private final InputStream is;
    private final Consumer<String> lineConsumer;
    private final Supplier<T> resultCreator;

    ProcessOutputReader(String threadName, InputStream is, Consumer<String> lineConsumer, Supplier<T> resultCreator) {
      this.threadName = threadName;
      this.is = is;
      this.lineConsumer = lineConsumer;
      this.resultCreator = resultCreator;
    }

    @Override
    public T call() throws Exception {
      Thread.currentThread().setName(threadName);

      try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

        String line;
        while ((line = br.readLine()) != null) {
          lineConsumer.accept(line);
        }
      }
      return resultCreator.get();
    }
  }
}
