package eu.trentorise.opendata.josman;

import eu.trentorise.opendata.commons.SemVersion;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.parboiled.common.ImmutableList;

/**
 *
 * @author David Leoni
 */
public class Runner {
    
     public static void main(String[] args) throws IOException, URISyntaxException {

        String repoName = "traceprov";
        String repoTitle = "TraceProv";
        
        String sep = File.separator;

        JosmanProject josman = new JosmanProject(
                repoName,
                repoTitle,
                "opendatatrentino",
                ".."+ sep + ".." + sep + repoName + sep + "prj", // todo fixed path!
                ".."+ sep + ".." + sep + repoName + sep + "prj" + sep +"target" + sep + "site", // todo fixed path!
                ImmutableList.<SemVersion>of(SemVersion.of("0.1.0")),
                true
        );

        josman.generateSite();
    }   
}
