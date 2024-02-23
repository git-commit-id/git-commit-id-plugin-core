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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Test;

public class ProcessHandlerTest {
  @FunctionalInterface
  private interface TestAction {
    void run(Process p) throws Exception;
  }

  private static final String STDOUT_LINE = "some text written to the stdout stream; line ";
  private static final String STDERR_LINE = "some text written to the stderr stream; line ";

  private static void runJavaProcess(int exitCode, int outRepeatCount, long sleepMillis, TestAction testAction) {
    // Creates a small Java program and starts it directly from the source file (JEP 330)
    // This is done instead of using shell scripts to be more independent of the OS running the test
    String javaBinaryPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    String javaCode = "public class Test {\n"
        + "  public static void main(String... args) throws Exception {\n"
        + "    int exitCode = Integer.parseInt(args[0]);\n"
        + "    int outRepeatCount = Integer.parseInt(args[1]);\n"
        + "    long sleepMillis = Long.parseLong(args[2]);\n"
        + "\n"
        + "    for (int i = 0; i < outRepeatCount; i++) {\n"
        + "      System.out.println(\"" + STDOUT_LINE + "\" + (i + 1));\n"
        + "      System.err.println(\"" + STDERR_LINE + "\" + (i + 1));\n"
        + "    }\n"
        + "\n"
        + "    if (sleepMillis > 0) {\n"
        + "      Thread.sleep(sleepMillis);\n"
        + "    }\n"
        + "\n"
        + "    System.exit(exitCode);\n"
        + "  }\n"
        + "}";

    try {
      Path tempJavaFile = Files.createTempFile("GitCommitIdPluginTest", ".java");
      try {
        Files.writeString(tempJavaFile, javaCode);
        String[] command =
            {javaBinaryPath, tempJavaFile.toAbsolutePath().toString(), Integer.toString(exitCode),
                Integer.toString(outRepeatCount), Long.toString(sleepMillis)};
        Process process = new ProcessBuilder(command).start();
        testAction.run(process);
      } finally {
        Files.delete(tempJavaFile);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected exception", e);
    }
  }

  @Test
  public void exitSuccess() {
    int exitCode = 0;
    int outputRepeatCount = 2;
    long sleepMillis = 0;

    runJavaProcess(exitCode, outputRepeatCount, sleepMillis, process -> {
      StringBuilder stdoutBuilder = new StringBuilder();
      try (ProcessHandler processHandler = new ProcessHandler(process, outLine -> stdoutBuilder.append(outLine).append('\n'))) {
        int actualExitCode = processHandler.exitValue(5, TimeUnit.SECONDS);
        String stderr = processHandler.getStderr();
        // For troubleshooting include `stderr` in message, e.g. in case there is a compilation error
        assertEquals("Process failed:\n" + stderr, exitCode, actualExitCode);
        assertEquals(STDOUT_LINE + "1\n" + STDOUT_LINE + "2\n", stdoutBuilder.toString());
        assertEquals(STDERR_LINE + "1\n" + STDERR_LINE + "2\n", stderr);
      }
    });
  }

  @Test
  public void noOutput() {
    int exitCode = 0;
    int outputRepeatCount = 0;
    long sleepMillis = 0;

    runJavaProcess(exitCode, outputRepeatCount, sleepMillis, process -> {
      AtomicReference<String> lastStdoutLine = new AtomicReference<>();
      try (ProcessHandler processHandler = new ProcessHandler(process, outLine -> lastStdoutLine.set(outLine))) {
        int actualExitCode = processHandler.exitValue(5, TimeUnit.SECONDS);
        String stderr = processHandler.getStderr();
        // For troubleshooting include `stderr` in message, e.g. in case there is a compilation error
        assertEquals("Process failed:\n" + stderr, exitCode, actualExitCode);
        assertNull(lastStdoutLine.get());
        assertEquals("", stderr);
      }
    });
  }

  @Test
  public void exitError() {
    int exitCode = 1;
    int outputRepeatCount = 2;
    long sleepMillis = 0;

    runJavaProcess(exitCode, outputRepeatCount, sleepMillis, process -> {
      StringBuilder stdoutBuilder = new StringBuilder();
      try (ProcessHandler processHandler = new ProcessHandler(process, outLine -> stdoutBuilder.append(outLine).append('\n'))) {
        int actualExitCode = processHandler.exitValue(5, TimeUnit.SECONDS);
        assertEquals(exitCode, actualExitCode);
        assertEquals(STDOUT_LINE + "1\n" + STDOUT_LINE + "2\n", stdoutBuilder.toString());
        assertEquals(STDERR_LINE + "1\n" + STDERR_LINE + "2\n", processHandler.getStderr());
      }
    });
  }

  @Test
  public void timeout() {
    int exitCode = 0;
    int outputRepeatCount = 2;
    long sleepMillis = TimeUnit.SECONDS.toMillis(3);

    runJavaProcess(exitCode, outputRepeatCount, sleepMillis, process -> {
      // Ignore stdout; it is not deterministic how many output has already been read
      Consumer<String> stdoutConsumer = line -> {};
      try (ProcessHandler processHandler = new ProcessHandler(process, stdoutConsumer)) {
        assertThrows(TimeoutException.class, () -> processHandler.exitValue(1, TimeUnit.MILLISECONDS));
        assertThrows(IllegalStateException.class, processHandler::getStderr);
      }
    });
  }

  /**
   * Tests behavior when the process writes large amounts of output to stdout and stderr.
   *
   * <p>
   * If the code handling the {@link Process} does not asynchronously read the output while waiting
   * for the process to finish, the process can get stuck because the output buffers are full. This
   * test verifies that this does not occur.
   */
  @Test
  public void largeOutput() {
    /*
     * Note: Can replicate the process output buffer becoming full, and the process getting stuck by
     * either setting a breakpoint in the `stdoutLineConsumer` lambda, or by changing it to perform
     * a sleep.
     * Unlike the other tests in this class this will then cause a timeout while waiting for the
     * process to finish (because the process is blocked writing any further data to stdout).
     *
     * (might be OS and configuration dependent whether this can be reproduced)
     */

    int exitCode = 0;
    int outputRepeatCount = 2_000;
    long sleepMillis = 0;

    runJavaProcess(exitCode, outputRepeatCount, sleepMillis, process -> {
      AtomicReference<String> lastStdoutLine = new AtomicReference<>();
      try (ProcessHandler processHandler = new ProcessHandler(process, outLine -> lastStdoutLine.set(outLine))) {
        int actualExitCode = processHandler.exitValue(5, TimeUnit.SECONDS);
        String stderr = processHandler.getStderr();
        // For troubleshooting include `stderr` in message, e.g. in case there is a compilation error
        assertEquals("Process failed:\n" + stderr, exitCode, actualExitCode);
        assertEquals(STDOUT_LINE + outputRepeatCount, lastStdoutLine.get());

        assertTrue(stderr.startsWith(STDERR_LINE + 1));
        assertTrue(stderr.endsWith(STDERR_LINE + outputRepeatCount + "\n"));
      }
    });
  }
}
