package eu.trentorise.opendata.jadoc.test;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
/**
 *
 * @author David Leoni
 */
public class TestMojo {

    /**
     * Says "Hi" to the user.
     *
     */
    @Mojo(name = "sayhi")
    public static class GreetingMojo extends AbstractMojo {

        /**
         * @parameter default-value="${project}"
         * @required
         * @readonly
         */
        MavenProject project;

        public void execute() throws MojoExecutionException {
            getLog().info("Hello, world.");
            MavenProject prj = new MavenProject(new Model());
            prj.setVersion(ROLE);
        }
    }
}
