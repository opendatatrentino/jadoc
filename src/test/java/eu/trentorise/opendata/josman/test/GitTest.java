package eu.trentorise.opendata.josman.test;

import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.josman.Josmans;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class GitTest {

    private static final Logger LOG = Logger.getLogger(GitTest.class.getName());

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(GitTest.class);
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    /*@Test
     public void testJcabi()) throws IOException {

     Github github = new RtGithub();
     Repo repo = github.repos().get(
     new Coordinates.Simple("opendatatrentino/odt-commons")
     );
     repo.git().commits().
     for (Release rel : repo.releases().iterate()){
     System.out.println("release: " + rel);    
     for (ReleaseAsset asset : rel.assets().iterate()){
     System.out.println("asset: " + asset);    
     }
     }
        
     for (Release rel : repo.releases().iterate()){
     System.out.println("release: " + rel);    
     for (ReleaseAsset asset : rel.assets().iterate()){
     System.out.println("asset: " + asset);    
     }
     }
        
        
     }
     */
    @Test
    public void testEgit() throws IOException {
        List<RepositoryTag> tags = Josmans.fetchTags("opendatatrentino", "josman");
        SortedMap<String, RepositoryTag> filteredTags = Josmans.filterTags("josman", tags);
        for (String tagName : filteredTags.keySet()) {
            RepositoryTag tag = filteredTags.get(tagName);
            LOG.info(tag.getName());
            LOG.info(tag.getCommit().getSha());
        }
    }

    @Test
    public void testReadRepo() throws IOException, GitAPIException {
        File repoFile = createSampleGitRepo();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(repoFile)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        LOG.log(Level.INFO, "directory: {0}", repo.getDirectory().getAbsolutePath());

        LOG.log(Level.INFO, "Having repository: {0}", repo.getDirectory().getAbsolutePath());

        LOG.log(Level.INFO, "current branch: {0}", repo.getBranch());
    }

    private static File createSampleGitRepo() throws IOException, GitAPIException {
        Repository repository = CookbookHelper.createNewRepository();

        System.out.println("Temporary repository at " + repository.getDirectory());

        // create the file
        File myfile = new File(repository.getDirectory().getParent(), "testfile");
        myfile.createNewFile();

        // run the add-call
        new Git(repository).add()
                .addFilepattern("testfile")
                .call();

        // and then commit the changes
        new Git(repository).commit()
                .setMessage("Added testfile")
                .call();

        LOG.info("Added file " + myfile + " to repository at " + repository.getDirectory());

        File dir = repository.getDirectory();

        repository.close();

        return dir;
    }

    private static class CookbookHelper {

        public static Repository openJGitCookbookRepository() throws IOException {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();
            return repository;
        }

        public static Repository createNewRepository() throws IOException {
            // prepare a new folder
            File localPath = File.createTempFile("TestGitRepository", "");
            localPath.delete();

            // create the directory
            Repository repository = FileRepositoryBuilder.create(new File(localPath, ".git"));
            repository.create();

            return repository;
        }
    }

}
