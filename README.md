
**About**

Jedoc is a Java program to generate documentation for open source projects. 

It's modeled after this workflow, where you:

1) create an open source project on Github
2) keep docs in source code, in folder `docs/` (i.e. see [jedoc docs](docs))
3) edit your markdown files and set relative links between them so they display nice in Github
4) create a branch named `branch-x.y` (i.e `branch-1.2`)
5) release your library using <a href="http://semver.org" target="_blank">semantic versioning</a> and tag it with tagname `projectName-x.y.z` (i.e. `jedoc-1.2.3`)
6) run Jedoc to create a corresponding github website (i.e. http://opendatatrentino.github.io/jedit) out of the docs. Links will be changed accordingly.
7) spam the world with links to your new project website

Project wiki is used for information about contributing to the project.

This way we 

* fully exploit all the existing editing features of Github
* reuse version information from git repo and Maven while generating the website
* evolve documention in separate branches
    * so if you have to patch something or just improve docs, just work in relative `branch-x.y` and then run Jedoc to publish it.
    * if you need to add functionality, create new branch named `branch-x.y+1`


**Project Status**: still a prototype

**News**

* Still no news...

social buttons

**Roadmap**: See [project milestones](../../milestones)

**Usage**: Project is not published yet, so [usage docs](docs) are subject to change. 

**License**: business-friendly [Apache License v2.0](LICENSE.txt)

**Contributing**: see [the wiki](../../wiki)

**Credits**

* David Leoni - DISI at University of Trento - david.leoni@unitn.it
