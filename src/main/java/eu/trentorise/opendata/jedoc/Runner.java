/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.trentorise.opendata.jedoc;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 *
 * @author David Leoni
 */
public class Runner {
    
     public static void main(String[] args) throws IOException, URISyntaxException {

        String repoName = "jedoc";
        String repoTitle = "Jedoc";

        JedocProject jedoc = new JedocProject(
                repoName,
                repoTitle,
                "opendatatrentino",
                "..\\..\\" + repoName + "\\prj", // todo fixed path!
                "..\\..\\" + repoName + "\\prj\\target\\site", // todo fixed path!
                true
        );

        jedoc.generateSite();
    }   
}
