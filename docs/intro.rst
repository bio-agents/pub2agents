
##################
What is Pub2Agents?
##################

Pub2Agents is a Java command-line agent that looks through the scientific literature available in `Europe PMC <https://europepmc.org/>`_ and constructs entry candidates for the `bio.agents <https://bio.agents/>`_ software registry from suitable publications. It automates a lot of the process needed for growing bio.agents, though results of the agent still need some manual curation before they are of satisfactory quality. Pub2Agents could be run at the beginning of each month to add hundreds of agents, databases and services published in bioinformatics and life sciences journals during the previous month.


********
Overview
********

First, Pub2Agents gets a list of publications for the given period by :ref:`narrowing down <select_pub>` the entire selection with combinations of keyphrases. Next, the contents of these publications :ref:`are downloaded <fetch_pub>` and the abstract of each publication :ref:`is mined <pass1>` for the potential agent name. Names are assigned confidence scores, with low confidence publications often not being suitable for bio.agents at all. In addition to the agent name, web links matching the name are extracted from the abstract and full text of a publication and :ref:`divided <divide_links>` to the homepage and other link attributes of bio.agents. In a :ref:`second pass <pass2>`, the :ref:`content of links <fetch_web>` and publications is also mined for :ref:`software license <usage_license>` and :ref:`programming language <usage_language>` information and phrases for the :ref:`agent description <usage_description>` attribute are automatically constructed. Good enough :ref:`non-existing <usage_existing>` results are :ref:`chosen for inclusion <final_decision>` to bio.agents. Terms from the EDAM ontology :ref:`are added <map>` to get the final results.

Pub2Agents can be run from start to finish with only one command (:ref:`all`). But :ref:`setup commands <setup_commands>` (fetching or copying the required input files) and :ref:`steps <steps>` (fetching of publications and web pages, mapping to `EDAM ontology <http://edamontology.org/page>`_ terms and applying the Pub2Agents algorithm) can also be applied individually. Execution can be resumed by restarting the last aborted step (:ref:`resume`). Commands can be influenced by changing the default values of :ref:`parameters <parameters>`. Some :ref:`examples <examples>` of running Pub2Agents are provided. One interesting example is :ref:`improving existing bio.agents entries <improving_existing>` added through some other means than Pub2Agents.

All files of one Pub2Agents run will end up in an :ref:`output directory <output_directory>` chosen by the user. All prerequisite and intermediate files will be saved for reproducibility and debugging purposes. The main results files are :ref:`results_csv` (contains all possible results), :ref:`diff_csv` (contains fix suggestions to existing bio.agents content) and :ref:`to_bioagents_json` (contains new entries to be imported into bio.agents). The following bio.agents attributes can be filled by Pub2Agents: `name <https://bioagents.readthedocs.io/en/latest/curators_guide.html#name-agent>`_, `description <https://bioagents.readthedocs.io/en/latest/curators_guide.html#description>`_, `homepage <https://bioagents.readthedocs.io/en/latest/curators_guide.html#homepage>`_, `function <https://bioagents.readthedocs.io/en/latest/curators_guide.html#function-group>`_, `topic <https://bioagents.readthedocs.io/en/latest/curators_guide.html#topic>`_, `language <https://bioagents.readthedocs.io/en/latest/curators_guide.html#programming-language>`_, `license <https://bioagents.readthedocs.io/en/latest/curators_guide.html#license>`_, `link <https://bioagents.readthedocs.io/en/latest/curators_guide.html#link-group>`_, `download <https://bioagents.readthedocs.io/en/latest/curators_guide.html#download-group>`_, `documentation <https://bioagents.readthedocs.io/en/latest/curators_guide.html#documentation-group>`_, `publication  <https://bioagents.readthedocs.io/en/latest/curators_guide.html#publication-group>`_, `credit <https://bioagents.readthedocs.io/en/latest/curators_guide.html#credit-group>`_. But not all attributes can always be filled, as shown in :ref:`performance`, and sometimes they are filled incorrectly, so Pub2Agents results imported into bio.agents still need some fixing and manual curation. Per month, roughly 500 entries could potentially be added to bio.agents from Pub2Agents results.

In addition to the command-line agent, there is also an :ref:`API <api>` and web application. However, in that case publication(s) have to be manually supplied (instead of automatic selection from Europe PMC) and publications are given and a bio.agents entry is returned for one agent at a time. In addition to a publication, web pages and the agent name can be supplied to help the Pub2Agents algorithm. The :ref:`Pub2Agents API <api>` can be consumed through the :ref:`/api <api_endpoint>` endpoint, either by sending requests to the public instance https://iechor.ut.ee/pub2agents/api or by sending requests to a local instance set up by following the instructions under :ref:`server`. The API could be used for example for automatic filling of fields in a new agent registration interface for bio.agents.


************
Dependencies
************

For selecting suitable publications and downloading their content, Pub2Agents is leveraging `Europe PMC`_, which among other things allows the `inclusion of preprints <http://blog.europepmc.org/2018/07/preprints.html>`_.

Publications are downloaded through the `PubFetcher <https://github.com/edamontology/pubfetcher>`_ library, that in addition to Europe PMC supports fetching publication content from other resources as fallback, for example directly from publisher web sites using the given DOI. In addition, PubFetcher provides support for downloading the content of links extracted by Pub2Agents (with support for metadata extraction from some types of links, like code repositories) and provides a database for storing all downloaded content.

Pub2Agents is also leveraging `EDAMmap <https://github.com/edamontology/edammap>`_, for preprocessing of input free text (including the extraction of links), for downloading and loading of bio.agents content, for `tf–idf <https://en.wikipedia.org/wiki/Tf%E2%80%93idf>`_ support, and of course, for mapping of entries to `EDAM ontology`_ terms.


*******
Caveats
*******

Inevitably, there will be false positives and false negatives, both at entry level (some suggested agents are not actual agents and some actual agents are missed by Pub2Agents) and at individual attribute level. Generally, if we try to decrease the number of FN entries, the number of FPs also tends to increase. Currently, Pub2Agents has been tuned to not have too many FPs, to not discourage curators into looking at all entries in the results. Some FNs are rather hopeless: quite obviously, unpublished agents can't be found by Pub2Agents, but in addition, there is the limitation that the agent name must be mentioned somewhere in the publication title or abstract.

For slightly better results, before a bigger run of Pub2Agents, it could be beneficial to `test if PubFetcher scraping rules <https://pubfetcher.readthedocs.io/en/stable/scraping.html#testing-of-rules>`_ are still up to date. Also, publisher web sites have to be consulted sometimes, so it could be beneficial to run Pub2Agents in a network with good access to journal articles.

Pub2Agents assigns a score for each result entry and orders the results based on this score. However, this score does not describe how "good" or high impact the agent itself is, but rather how confidently the agent name was extracted. A higher score is obtained if the name of the agent is unique, put to the start of the publication title, surrounded by certain keywords (like "called" or "freely") in the abstract and matches a URL in the abstract (but also in the publication full text).


*******
Install
*******

Installation instructions can be found in the project's GitHub repo at `INSTALL <https://github.com/bio-agents/pub2agents/blob/master/INSTALL.md>`_.


**********
Quickstart
**********

This will generate results to the directory ``output`` for publications added to Europe PMC on the 23rd of August 2019:

.. code-block:: bash

  $ java -jar path/to/pub2agents-cli-<version>.jar -all output \
  --edam http://edamontology.org/EDAM.owl \
  --idf https://github.com/edamontology/edammap/raw/master/doc/bioagents.idf \
  --idf-stemmed https://github.com/edamontology/edammap/raw/master/doc/bioagents.stemmed.idf \
  --day 2019-08-23

If this quick example worked, then for the next incarnations of Pub2Agents, the ``EDAM.owl`` and ``.idf`` files could be downloaded to local disk and the corresponding local paths used in the command instead of the URLs, and ``--month 2019-08`` could be used instead of ``--day 2019-08-23`` to fetch results for an entire month. Explanations for the columns and attributes of the results files can be found in the documentation at :ref:`results_csv_columns`, :ref:`diff_csv_columns` and :ref:`to_bioagents_attributes`.

For testing out one bio.agents entry generation from a given publication, the public web application at https://iechor.ut.ee/pub2agents/ can be used by filling in the "Publications" and clicking on "Run all". As for using the API, there are a few :ref:`api_examples`.


****
Repo
****

Pub2Agents is hosted at https://github.com/bio-agents/pub2agents.


*******
Support
*******

Should you need help installing or using Pub2Agents, please get in touch with Erik Jaaniso (the lead developer) directly via the `tracker <https://github.com/bio-agents/pub2agents/issues>`_.


*******
License
*******

Pub2Agents is free and open-source software licensed under the GNU General Public License v3.0, as seen in `COPYING <https://github.com/bio-agents/pub2agents/blob/master/COPYING>`_.
