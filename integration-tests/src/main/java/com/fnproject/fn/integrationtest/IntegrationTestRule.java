package com.fnproject.fn.integrationtest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.assertj.core.api.Assertions;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around fn cli to invoke function integration tests.
 * Created on 14/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class IntegrationTestRule implements TestRule {


    private static final String repoPlaceholder = "<url>https://dl.bintray.com/fnproject/fnproject</url>";
    private static final String versionPlaceholder = "<fdk.version>1.0.0-SNAPSHOT</fdk.version>";
    private static final String snapshotPlaceholderRegex = "<snapshots>.*</snapshots>";
    private int appCount = 0;
    private String testName;


    private final List<File> cleanupDirs = new ArrayList<>();
    private final List<String> cleanupApps = new ArrayList<>();

    public String getFlowURL() {
        String url = System.getenv("COMPLETER_BASE_URL");
        if (url == null) {
            return "http://" + getDockerLocalhost() + ":8081";
        }
        return url;
    }

    /**
     * Returns a hostname tha resolves to the test host from within a docker container
     */
    public String getDockerLocalhost() {
        String dockerLocalhost = System.getenv("DOCKER_LOCALHOST");
        if (dockerLocalhost == null) {
            String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

            if (osName.contains("darwin") || osName.contains("mac")) {
                return "docker.for.mac.host.internal";
            } else
                throw new RuntimeException("Unable to determine docker localhost address - set 'DOCKER_LOCALHOST' env variable to the docker host network address ");
        }
        return dockerLocalhost;
    }

    private String getLocalFnRepo() {
        String envRepo = System.getenv("MAVEN_REPOSITORY");
        if (envRepo == null) {
            envRepo = "http://" + getDockerLocalhost() + ":18080";
        }
        return envRepo;
    }


    private String getProjectVersion() {
        String version = System.getenv("FN_JAVA_FDK_VERSION");

        if (version == null) {
            version = "1.0.0-SNAPSHOT";
        }
        return version;
    }

    private String getFnLogFile() {
        return System.getenv("FN_LOG_FILE");
    }

    private String getFlowLogFile() {
        return System.getenv("FLOW_LOG_FILE");
    }

    private String getFnCmd() {
        String cmd = System.getenv("FN_CMD");
        if (cmd == null) {
            return "fn";
        }
        return cmd;
    }


    public static class CmdResult {
        private final String cmd;
        private final boolean success;
        private final String stdout;
        private final String stderr;

        private CmdResult(String cmd, boolean success, String stdout, String stderr) {
            this.success = success;
            this.stdout = stdout;
            this.stderr = stderr;
            this.cmd = cmd;
        }

        boolean isSuccess() {
            return success;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public String toString() {
            return "CmdResult: cmd=" + cmd + ", success=" + success + ", stdout=" + stdout + ", stderr=" + stderr;
        }
    }

    public class TestContext {

        private File baseDir;
        private final String testName;

        public TestContext(File baseDir, String testName) {
            this.baseDir = baseDir;
            this.testName = testName;
        }

        /**
         * Copies the contents of a given directory (relative to test root) into the temporary test directory
         *
         * @param location local directory
         * @return
         * @throws IOException
         */
        public TestContext withDirFrom(String location) throws IOException {
            FileUtils.copyDirectory(new File(location), baseDir);
            return this;
        }

        public CmdResult runFnWithInput(String input, String... args) throws Exception {
            CmdResult res = runFnWithInputAllowError(input, args);

            if (res.isSuccess() == false) {
                System.err.println("FN FAIL!");
                System.err.println(res.toString());
            }

            Assertions.assertThat(res.isSuccess()).withFailMessage("Expected command '" + res.cmd + "' to return 0." + "FN FAIL: " + res).isTrue();
            return res;
        }

        /**
         * Runs the configured Fn command with input  returning a process result
         *
         * @param input the input string ot pass as fn stdin
         * @param args  args to fn (excluding the fn command itself)
         * @return a command result to get the result of a command
         * @throws Exception
         */
        public CmdResult runFnWithInputAllowError(String input, String... args) throws Exception {
            List<String> cmd = new ArrayList<>();
            cmd.add(getFnCmd());
            cmd.addAll(Arrays.asList(args));

            System.err.println("Running '" + String.join(" ", cmd) + "'");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(baseDir);

            if (System.getenv("FN_JAVA_FDK_VERSION") == null) {
                // this means that FN init will pick up the local version not the latest.
                pb.environment().put("FN_JAVA_FDK_VERSION", "1.0.0-SNAPSHOT");
            }

            // Sort of a hack for local mac running  with a proxy
            String noProxy = Optional.ofNullable(System.getenv("no_proxy")).map((f) -> f + ",").orElse("") + getDockerLocalhost();
            System.err.printf("setting no_proxy '%s'\n",noProxy);
            pb.environment().put("no_proxy", noProxy);

            Process p = pb.start();

            p.getOutputStream().write(input.getBytes());
            p.getOutputStream().close();

            CompletableFuture<String> stderr = new CompletableFuture<>();

            new Thread(() -> {

                try {
                    BufferedReader bri = new BufferedReader
                      (new InputStreamReader(p.getErrorStream()));

                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = bri.readLine()) != null) {
                        System.err.println("FN ERR: " + line);
                        output.append(line);
                    }
                    stderr.complete(output.toString());
                } catch (IOException e) {
                    stderr.completeExceptionally(e);
                }
            }).start();

            BufferedReader bri = new BufferedReader
              (new InputStreamReader(p.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = bri.readLine()) != null) {
                System.err.println("FN OUT: " + line);
                output.append(line);
            }
            p.waitFor(600, TimeUnit.SECONDS);
            System.err.println("Command '" + String.join(" ", cmd) + "' with code " + p.exitValue());

            return new CmdResult(String.join(" ", cmd), p.exitValue() == 0, output.toString(), stderr.get());
        }

        /**
         * Runs the configure `fn` command with given arguments fails if the command returns without  succes
         *
         * @param args
         * @return a command result capturinng the output of fn
         * @throws Exception
         */
        public CmdResult runFn(String... args) throws Exception {
            return runFnWithInput("", args);
        }


        /**
         * Rewrites the POM to reflect the correct target repo
         */
        public TestContext rewritePOM() throws Exception {
            File pomFile = new File(baseDir, "pom.xml");

            String pomFileContent = FileUtils.readFileToString(pomFile, StandardCharsets.UTF_8);
            String newPomContent = pomFileContent.replace(repoPlaceholder, "<url>" + getLocalFnRepo() + "</url>");
            Assertions.assertThat(newPomContent).withFailMessage("No placeholder found in POM").isNotEqualTo(pomFileContent);

            String versionPomContent = newPomContent.replace(versionPlaceholder, "<fdk.version>" + getProjectVersion() + "</fdk.version>");

            versionPomContent = versionPomContent.replaceFirst(snapshotPlaceholderRegex, "<snapshots><enabled>true</enabled></snapshots>");

            System.err.println(versionPomContent);
            FileUtils.writeStringToFile(pomFile, versionPomContent, StandardCharsets.UTF_8);
            return this;
        }


        /**
         * Gets the app name you should use for tests.
         *
         * @return
         */
        public String appName() {
            return this.testName;
        }

        public TestContext mkdir(String name) {
            new File(baseDir, name).mkdir();
            return this;
        }

        public TestContext cd(String dir) {
            baseDir = new File(baseDir, dir);
            return this;
        }
    }


    /**
     * creates a new test contesxt
     *
     * @return
     * @throws IOException
     */
    public TestContext newTest() throws IOException {
        Path tmpDir = Files.createTempDirectory("fnitest");
        cleanupDirs.add(tmpDir.toFile());
        String appName = testName + appCount++;
        cleanupApps.add(appName);
        return new TestContext(tmpDir.toFile(), appName);
    }


    @Override
    public Statement apply(Statement statement, Description description) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                testName = description.getMethodName();
                StringBuilder fnOutput = new StringBuilder();
                StringBuilder flowOutput = new StringBuilder();

                Tailer fnTailer = null;
                if (getFnLogFile() != null) {
                    fnTailer = Tailer.create(new File(getFnLogFile()), new TailerListenerAdapter() {
                        @Override
                        public void handle(final String line) {
                            System.err.println("FNSRV:" + line);
                            fnOutput.append(line);
                        }
                    }, 10, true);
                }

                Tailer flowTailer = null;

                if (getFlowLogFile() != null) {
                    flowTailer = Tailer.create(new File(getFlowLogFile()), new TailerListenerAdapter() {
                        @Override
                        public void handle(final String line) {
                            System.err.println("FLOW:" + line);
                            fnOutput.append(line);
                        }
                    }, 10, true);
                }

                try {
                    statement.evaluate();
                } finally {

                    for (File cleanup : cleanupDirs) {
                        FileUtils.deleteDirectory(cleanup);
                    }

                    if (fnTailer != null) {
                        fnTailer.stop();
                    }

                    if (flowTailer != null) {
                        flowTailer.stop();
                    }
                }
            }
        };
    }
}
