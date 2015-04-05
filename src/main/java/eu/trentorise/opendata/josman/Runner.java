/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.trentorise.opendata.josman;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

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
                false
        );

        josman.generateSite();
    }   
}
