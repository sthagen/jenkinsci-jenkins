package hudson.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.model.utils.AbortExceptionPublisher;
import hudson.model.utils.IOExceptionPublisher;
import hudson.model.utils.ResultWriterPublisher;
import hudson.model.utils.TrueFalsePublisher;
import hudson.tasks.ArtifactArchiver;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Freestyle publishers statuses tests
 *
 * @author Kanstantsin Shautsou
 */
public class FreestyleJobPublisherTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Execute all publishers even one of publishers return false.
     */
    @Issue("JENKINS-26964")
    @Test
    public void testFreestyleWithFalsePublisher() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.getPublishersList().add(new TrueFalsePublisher(true)); // noop
        p.getPublishersList().add(new TrueFalsePublisher(false));   // FAIL build with false
        p.getPublishersList().add(new ResultWriterPublisher("result.txt")); // catch result to file
        final ArtifactArchiver artifactArchiver = new ArtifactArchiver("result.txt");
        artifactArchiver.setOnlyIfSuccessful(false);
        p.getPublishersList().add(artifactArchiver); // transfer file to build dir

        FreeStyleBuild b = j.buildAndAssertStatus(Result.FAILURE, p);
        File file = new File(b.getArtifactsDir(), "result.txt");
        assertTrue("ArtifactArchiver is executed even prior publisher fails", file.exists());
        assertEquals("Publisher, after publisher with return false status, must see FAILURE status", FileUtils.readFileToString(file, StandardCharsets.UTF_8), Result.FAILURE.toString());
    }

    /**
     * Execute all publishers even one of them throws AbortException.
     */
    @Issue("JENKINS-26964")
    @Test
    public void testFreestyleWithExceptionPublisher() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.getPublishersList().add(new TrueFalsePublisher(true)); // noop
        p.getPublishersList().add(new AbortExceptionPublisher()); // FAIL build with AbortException
        p.getPublishersList().add(new ResultWriterPublisher("result.txt")); // catch result to file
        final ArtifactArchiver artifactArchiver = new ArtifactArchiver("result.txt");
        artifactArchiver.setOnlyIfSuccessful(false);
        p.getPublishersList().add(artifactArchiver); // transfer file to build dir

        FreeStyleBuild b = j.buildAndAssertStatus(Result.FAILURE, p);

        j.assertLogNotContains("\tat", b); // log must not contain stacktrace
        j.assertLogContains("Threw AbortException from publisher!", b); // log must contain exact error message
        File file = new File(b.getArtifactsDir(), "result.txt");
        assertTrue("ArtifactArchiver is executed even prior publisher fails", file.exists());
        assertEquals("Third publisher must see FAILURE status", FileUtils.readFileToString(file, StandardCharsets.UTF_8), Result.FAILURE.toString());
    }

    /**
     * Execute all publishers even one of them throws any Exceptions.
     */
    @Issue("JENKINS-26964")
    @Test
    public void testFreestyleWithIOExceptionPublisher() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.getPublishersList().add(new TrueFalsePublisher(true)); // noop
        p.getPublishersList().add(new IOExceptionPublisher());   // fail with IOException
        p.getPublishersList().add(new ResultWriterPublisher("result.txt")); //catch result to file
        final ArtifactArchiver artifactArchiver = new ArtifactArchiver("result.txt");
        artifactArchiver.setOnlyIfSuccessful(false);
        p.getPublishersList().add(artifactArchiver); // transfer file to build dir

        FreeStyleBuild b = j.buildAndAssertStatus(Result.FAILURE, p);

        j.assertLogContains("\tat hudson.model.utils.IOExceptionPublisher", b); // log must contain stacktrace
        j.assertLogContains("Threw IOException from publisher!", b); // log must contain exact error message
        File file = new File(b.getArtifactsDir(), "result.txt");
        assertTrue("ArtifactArchiver is executed even prior publisher fails", file.exists());
        assertEquals("Third publisher must see FAILURE status", FileUtils.readFileToString(file, StandardCharsets.UTF_8), Result.FAILURE.toString());
    }
}
