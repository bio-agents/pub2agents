# Pub2Agents

Pub2Agents is a Java command-line agent that looks through the scientific literature available in [Europe PMC](https://europepmc.org/) and constructs entry candidates for the [bio.agents](https://bio.agents/) software registry from suitable publications. It automates a lot of the process needed for growing bio.agents, though results of the agent still need some manual curation before they are of satisfactory quality. Pub2Agents could be run at the beginning of each month to add hundreds of agents, databases and services published in bioinformatics and life sciences journals during the previous month.

## Overview

First, Pub2Agents gets a list of publications for the given period by narrowing down the entire selection with combinations of keyphrases. Next, the contents of these publications are downloaded and the abstract of each publication is mined for the potential agent name. Names are assigned confidence scores, with low confidence publications often not being suitable for bio.agents at all. In addition to the agent name, web links matching the name are extracted from the abstract and full text of a publication and divided to the homepage and other link attributes of bio.agents. In a second pass of the algorithm, the content of links and publications is also mined for software license and programming language information and phrases for the agent description attribute are automatically constructed. Terms from the [EDAM ontology](http://edamontology.org/page) are added to get the final results. Good enough non-existing results are chosen for inclusion to bio.agents. In addition to finding new content for bio.agents, Pub2Agents can also be used to improve the current content when run on existing entries of bio.agents.

## Dependencies

[PubFetcher](https://github.com/edamontology/pubfetcher) is used for downloading publications and links and [EDAMmap](https://github.com/edamontology/edammap) is used for adding EDAM ontology annotations to the new bio.agents entries.

## Install

Installation instructions can be found in [INSTALL.md](INSTALL.md).

## Use online

Pub2Agents can also be run as a web server, however in that case a publication has to be provided manually as input. Based on the input (which can also be multiple publications of the same agent and optionally also include the agent/database name and related web pages) one bio.agents entry candidate of the agent is automatically generated. The web server runs a web application, but also an API, which could be used for example to help in a new agent registration interface to automatically fill in or suggest values to different fields. Public instances of the [web application](https://iechor.ut.ee/pub2agents/) and [API](https://pub2agents.readthedocs.io/en/latest/api.html) are available.

## Documentation

Documentation for Pub2Agents can be found at https://pub2agents.readthedocs.io/en/latest/.
