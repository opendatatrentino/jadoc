/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.trentorise.opendata.josman;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 *
 * @author David Leoni
 */
public class Runner {
    
     public static void main(String[] args) throws IOException, URISyntaxException {

        String repoName = "josman";
        String repoTitle = "Josman";

        JosmanProject josman = new JosmanProject(
                repoName,
                repoTitle,
                "opendatatrentino",
                "..\\..\\" + repoName + "\\prj", // todo fixed path!
                "..\\..\\" + repoName + "\\prj\\target\\site", // todo fixed path!
                true
        );

        josman.generateSite();
    }   
}
