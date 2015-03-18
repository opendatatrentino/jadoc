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

        String repoName = "odt-commons";
        String repoTitle = "Odt Commons";

        JedocProject jedoc = new JedocProject(
                repoName,
                repoTitle,
                "opendatatrentino",
                "..\\..\\" + repoName + "\\prj", // todo fixed path!
                "..\\..\\" + repoName + "\\pages", // todo fixed path!
                true
        );

        jedoc.generateSite();
    }   
}
