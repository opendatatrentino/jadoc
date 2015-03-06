package eu.trentorise.opendata.jedoc.test;

import eu.trentorise.opendata.jedoc.Jedocs;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Logger;
import org.eclipse.egit.github.core.RepositoryTag;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class TestGit {
    private static final Logger LOG = Logger.getLogger(TestGit.class.getName());


           

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
    public void testEgit() throws IOException  {
        List<RepositoryTag> tags = Jedocs.fetchTags("opendatatrentino", "odt-commons");
        SortedMap<String, RepositoryTag> filteredTags = Jedocs.filterTags("odt-commons", tags);
        for (String tagName: filteredTags.keySet()){
            RepositoryTag tag = filteredTags.get(tagName);
            System.out.println(tag.getName());
            System.out.println(tag.getCommit().getSha());
        }
    }
}

