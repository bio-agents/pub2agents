/*
 * Copyright © 2018, 2019 Erik Jaaniso
 *
 * This file is part of Pub2Agents.
 *
 * Pub2Agents is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Pub2Agents is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pub2Agents.  If not, see <http://www.gnu.org/licenses/>.
 */

package agents.bio.pub2agents.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.edamontology.edammap.core.input.json.DocumentationType;
import org.edamontology.edammap.core.input.json.DownloadType;
import org.edamontology.edammap.core.input.json.LinkType;
import org.edamontology.edammap.core.input.json.Agent;

public final class Common {

	public static final String MAIN_MARKER = "MAIN";

	public static final String LOG_FILE = "pub2agents.log";
	public static final String EDAM_FILE = "EDAM.owl";
	public static final String IDF_FILE = "tf.idf";
	public static final String IDF_STEMMED_FILE = "tf.stemmed.idf";
	public static final String BIOTOOLS_FILE = "bioagents.json";
	public static final String PUB_FILE = "pub.txt";
	public static final String DB_FILE = "db.db";
	public static final String STEP_FILE = "step.txt";
	public static final String WEB_FILE = "web.txt";
	public static final String DOC_FILE = "doc.txt";
	public static final String PASS1_FILE = "pass1.json";
	static final String RESULTS_FILE = "results.csv";
	static final String DIFF_FILE = "diff.csv";
	public static final String NEW_FILE = "new.json";
	public static final String MAP_TXT_FILE = "map.txt";
	public static final String MAP_HTML_DIRECTORY = "map";
	public static final String MAP_JSON_FILE = "map.json";
	public static final String TO_BIOTOOLS_FILE = "to_bioagents.json";

	static final Pattern SCHEMA_START = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");
	static final Pattern KNOWN_SCHEMA_START = Pattern.compile("(?i)^(http|https|ftp)://");

	static final Pattern LINK_COMPARE_START = Pattern.compile("(?i)^((http|https|ftp)://)?(www\\.)?");
	static final Pattern LINK_COMPARE_END = Pattern.compile("/+$");
	private static final Pattern LINK_COMPARE_INDEX = Pattern.compile("/+index\\.[\\p{L}\\p{N}]+$");
	static final Pattern LINK_COMPARE_REST = Pattern.compile("^(\\p{Ll}\\p{Lu}|./*[.]?[\"(\\[{<>}\\])]+[.]?|.\\.\\p{Lu}[\\p{L}\\p{N}'-]|//*\\.|./+\\.|./*--|./*[^/]+@[^/]+\\.[^/]+)[\\p{L}\\p{N}'-]*$");
	static final Pattern LINK_COMPARE_SCHEMA = Pattern.compile("(http|https|ftp)://");

	static final Pattern PATH_SPLIT = Pattern.compile("[-_]");

	static final Pattern USE_IN_PATTERN = Pattern.compile("^[\\p{L}\\p{N}]$");

	private static final String TOOL_TITLE_GENERAL = "database|data|web|server|webserver|web-server|package|agentkit|agentbox|suite|agentsuite|agents|agent|kit|framework|workbench|pipeline|software|program|platform|project|resource|r";
	private static final Pattern TOOL_TITLE_PRUNE = Pattern.compile("(?i)^(update|v|ver|version|(v|ver|version)?\\p{N}+([.-]\\p{N}+)*|" + TOOL_TITLE_GENERAL + ")$");
	static final Pattern TOOL_TITLE_TRIM = Pattern.compile("( ?(db|v|ver|version|update))*( ?\\p{N}{0,4})?( ?(db|v|ver|version|update))*$");

	static final Pattern BIOTOOLS_EXTRACTED_VERSION_TRIM = Pattern.compile(" ?([vV](er(sion)?)?)? ?\\p{N}+([.-]\\p{N}+)*$");
	static final Pattern BIOTOOLS_PROCESSED_VERSION_TRIM = Pattern.compile(" ?([v](er(sion)?)?)? ?\\p{N}+$");

	static final Pattern TITLE_SEPARATOR = Pattern.compile("(?i)(: | - |--a |--an |--|-a |-an |:a |:an |, a |, an |\n|\r|\\|)");
	static final Pattern WEBPAGE_TITLE_SEPARATOR = Pattern.compile("(?i)(\\||---|--| - |::|: | // | / | @ | \\< | \\> |«|»|·|•|\n|\r|, a |, an )");

	static final double QUERY_IDF_SCALING = 2;

	static final Pattern WHITESPACE = Pattern.compile("[\\p{Z}\\p{Cc}\\p{Cf}]+");
	static final Pattern INTERNAL_TRIM = Pattern.compile("  +");

	private static final Pattern LINK_HELPDESK = Pattern.compile("(?i)(^|[^\\p{L}-])(contact|contactus|help[-_]?desk)s?([^\\p{L}-]|$)");
	private static final Pattern LINK_ISSUES = Pattern.compile("(?i)^(https?://)?(www\\.)?(github\\.com/+[^/]+/+[^/]+/+issues|sourceforge\\.net/+p/+[^/]+/+tickets|code\\.google\\.com/+(archive/+)?p/+[^/]+/+issues|bitbucket\\.org/+[^/]+/+[^/]+/+issues)([^\\p{L}]|$)");
	private static final Pattern LINK_LIST_ADDR = Pattern.compile("(?i)^(https?://)?(www\\.)?(sourceforge\\.net/+projects/+[^/]+/+lists)([^\\p{L}]|$)");
	private static final Pattern LINK_LIST_BOTH = Pattern.compile("(?i)(^|[^\\p{L}-])(mailman|listinfo|mailing[-_]?lists?)([^\\p{L}-]|$)");
	private static final Pattern LINK_FORUM = Pattern.compile("(?i)^(https?://)?(www\\.)?(groups\\.google\\.com|gitter\\.im|sourceforge\\.net/+p/+[^/]+/+discussion)([^\\p{L}]|$)");
	private static final Pattern LINK_SOFTWARE_CATALOGUE = Pattern.compile("(?i)^(https?://)?(www\\.)?(mybiosoftware\\.com|biocatalogue\\.org)([^\\p{L}]|$)");
	private static final Pattern LINK_REPOSITORY = Pattern.compile("(?i)^(https?://)?(www\\.)?(bioconductor\\.org|github\\.com|sourceforge\\.net|code\\.google\\.com|cran\\.r-project\\.org|bitbucket\\.org|gitlab\\.com|pypi\\.(python\\.)?org|apps\\.cytoscape\\.org)([^\\p{L}]|$)");
	private static final Pattern LINK_SOCIAL = Pattern.compile("(?i)^(https?://)?(www\\.)?(twitter\\.com|facebook\\.com)([^\\p{L}]|$)");

	private static final Pattern DOWNLOAD_SRC_CODE = Pattern.compile("(?i)^(https?://)?(www\\.)?(git\\.bioconductor\\.org|github\\.com/+[^/]+/+[^/]+/+tree|sourceforge\\.net/+projects/+[^/]+/+files|code\\.google\\.com/+(archive/+)?p/+[^/]+/+source|bitbucket\\.org/+[^/]+/+[^/]+/+src)([^\\p{L}]|$)");
	private static final Pattern DOWNLOAD_PKG = Pattern.compile("(?i)^(https?://)?(www\\.)?(github\\.com/+[^/]+/+[^/]+/+releases|sourceforge\\.net/+projects/+[^/]+/+files/+.+/+download|code\\.google\\.com/+(archive/+)?p/+[^/]+/+downloads|bitbucket\\.org/+[^/]+/+[^/]+/+downloads|apps\\.cytoscape\\.org/+download)([^\\p{L}]|$)");
	private static final String DOWNLOAD_EXT_PKG_STRING = "gz|zip|bz2|tar|tgz|7z|rar|xz";
	private static final String DOWNLOAD_EXT_BIN_STRING = "jar|exe";
	private static final Pattern DOWNLOAD_EXT_PKG = Pattern.compile("(?i)\\.(" + DOWNLOAD_EXT_PKG_STRING + ")([^\\p{L}-]|$)");
	private static final Pattern DOWNLOAD_EXT_BIN = Pattern.compile("(?i)\\.(" + DOWNLOAD_EXT_BIN_STRING + ")([^\\p{L}-]|$)");
	static final Pattern DOWNLOAD_EXT = Pattern.compile("(?i)\\.(" + DOWNLOAD_EXT_PKG_STRING + "|" + DOWNLOAD_EXT_BIN_STRING + ")([^\\p{L}-]|$)");
	private static final Pattern DOWNLOAD_FTP = Pattern.compile("(?i)^ftp://");
	private static final Pattern DOWNLOAD_API = Pattern.compile("(?i)\\.(wsdl)([^\\p{L}-]|$)");
	private static final Pattern DOWNLOAD_CONTAINER = Pattern.compile("(?i)(^|[^\\p{L}-])(docker)([^\\p{L}-]|$)");
	private static final Pattern DOWNLOAD_CWL = Pattern.compile("(?i)\\.(cwl)([^\\p{L}-]|$)");
	private static final Pattern DOWNLOAD_PAGE = Pattern.compile("(?i)(^|[^\\p{Ll}])download(s|ing)?([^\\p{Ll}]|$)");

	private static final Pattern DOCUMENTATION_API = Pattern.compile("(?i)(^|[^\\p{L}-])(api|apidoc)s?([^\\p{L}-]|$)");
	private static final String DOCUMENTATION_CITE_EITHER = "citing";
	private static final Pattern DOCUMENTATION_CITE = Pattern.compile("(?i)((^|[^\\p{L}-])(references|cite|citation)s?([^\\p{L}-]|$))|((" + DOCUMENTATION_CITE_EITHER + ")s?([^\\p{L}-]|$))|((^|[^\\p{L}-])(" + DOCUMENTATION_CITE_EITHER + "))");
	private static final Pattern DOCUMENTATION_FAQ = Pattern.compile("(?i)(^|[^\\p{L}])faqs?([^\\p{L}]|$)");
	private static final String DOCUMENTATION_GENERAL_EITHER = "about|read[-_]?me|information|overview|description|features";
	private static final Pattern DOCUMENTATION_GENERAL = Pattern.compile("(?i)((" + DOCUMENTATION_GENERAL_EITHER + ")s?([^\\p{L}-]|$))|((^|[^\\p{L}-])(" + DOCUMENTATION_GENERAL_EITHER + "))");
	private static final String DOCUMENTATION_INSTALL_EITHER = "install|installation|installing";
	private static final Pattern DOCUMENTATION_INSTALL = Pattern.compile("(?i)((" + DOCUMENTATION_INSTALL_EITHER + ")s?([^\\p{L}-]|$))|((^|[^\\p{L}-])(" + DOCUMENTATION_INSTALL_EITHER + "))");
	private static final String DOCUMENTATION_QUICK_EITHER = "quick[-_]?tour|getting[-_]?started|beginners?[-_]?guide|start[-_]?guide|quick[-_]?(start|guide)";
	private static final Pattern DOCUMENTATION_QUICK = Pattern.compile("(?i)((" + DOCUMENTATION_QUICK_EITHER + ")s?([^\\p{L}-]|$))|((^|[^\\p{L}-])(" + DOCUMENTATION_QUICK_EITHER + "))");
	private static final String DOCUMENTATION_TERMS_EITHER = "terms[-_]?of[-_]?use|conditions[-_]?of[-_]?use";
	private static final Pattern DOCUMENTATION_TERMS = Pattern.compile("(?i)((^|[^\\p{L}-])(terms|conditions|legal|license|copyright|copying)s?([^\\p{L}-]|$))|((" + DOCUMENTATION_TERMS_EITHER + ")s?([^\\p{L}-]|$))|((^|[^\\p{L}-])(" + DOCUMENTATION_TERMS_EITHER + "))");
	private static final String DOCUMENTATION_TRAINING_EITHER = "training|exercise";
	private static final Pattern DOCUMENTATION_TRAINING = Pattern.compile("(?i)((" + DOCUMENTATION_TRAINING_EITHER + ")s?([^\\p{L}-]|$))|((^|[^\\p{L}-])(" + DOCUMENTATION_TRAINING_EITHER + "))");
	private static final String DOCUMENTATION_TUTORIAL_EITHER = "tutorial|example|guided[-_]?tour";
	private static final Pattern DOCUMENTATION_TUTORIAL = Pattern.compile("(?i)((^|[^\\p{L}-])(demo|tour)s?([^\\p{L}-]|$))|((" + DOCUMENTATION_TUTORIAL_EITHER + ")s?([^\\p{L}-]|$))|((^|[^\\p{L}-])(" + DOCUMENTATION_TUTORIAL_EITHER + "))");

	private static final String DOCUMENTATION_EITHER = "vignette|manual|documentation|how[-_]?to|introduction|instruction|users?[-_]?guide";
	private static final Pattern DOCUMENTATION = Pattern.compile("(?i)((^|[^\\p{L}-])(usage|guide|how|use)s?([^\\p{L}-]|$))|((help|doc|intro|" + DOCUMENTATION_EITHER + ")s?([^\\p{L}-]|$))|((^|[^\\p{L}-])(" + DOCUMENTATION_EITHER + "))");
	private static final Pattern DOCUMENTATION_WIKI = Pattern.compile("(?i)^(https?://)?(www\\.)?(github\\.com/+[^/]+/+[^/]+/+wiki|sourceforge\\.net/+p/+[^/]+/wiki|sourceforge\\.net/+p/+[^/]+/+home|code\\.google\\.com/+(archive/+)?p/+[^/]+/+wikis?|bitbucket\\.org/+[^/]+/+[^/]+/+wiki)([^\\p{L}]|$)");
	private static final Pattern DOCUMENTATION_EXT = Pattern.compile("(?i)\\.(pdf|ps|doc|docx|ppt|pptx)([^\\p{L}-]|$)");

	static final Pattern BIOTOOLS_SCHEMA_URL_PATTERN = Pattern.compile("^https?://[^\\s/$.?#]*\\.[^\\s]*$");
	static final Pattern BIOTOOLS_SCHEMA_URLFTP_PATTERN = Pattern.compile("^(https?|s?ftp)://[^\\s/$.?#]*\\.[^\\s]*$");

	private static final Pattern PUNCTUATION_NUMBERS = Pattern.compile("[\\p{P}\\p{S}\\p{N}]+");
	private static final Pattern NAME_SEPARATOR = Pattern.compile("[ \\u002D\\u2010]+");
	private static final Pattern PERIOD = Pattern.compile("[.]");
	private static final Pattern UPPERCASE = Pattern.compile("^\\{Lu}+$");

	static final int LINK_MATCH_DISPLAY_LIMIT = 5;

	static String prependHttp(String url) {
		if (!SCHEMA_START.matcher(url).find()) {
			return "http://" + url;
		} else {
			return url;
		}
	}

	static String trimUrl(String url) {
		if (url == null) return "";
		url = LINK_COMPARE_INDEX.matcher(LINK_COMPARE_END.matcher(LINK_COMPARE_START.matcher(url).replaceFirst("")).replaceFirst("")).replaceFirst("");
		int slash = url.indexOf('/');
		if (slash < 0) {
			return url.toUpperCase(Locale.ROOT);
		} else {
			return url.substring(0, slash).toUpperCase(Locale.ROOT) + url.substring(slash);
		}
	}

	static String pruneToMax(String string, int maxLength) {
		if (string.length() <= maxLength) {
			return string;
		} else if (maxLength < 1) {
			return "";
		} else if (maxLength - 4 < 1) {
			return string.substring(0, maxLength);
		} else {
			return string.substring(0, maxLength - 4) + " ...";
		}
	}

	static String fillToMin(String string, int minLength) {
		if (string.length() >= minLength) {
			return string;
		} else if (string.length() + 1 == minLength) {
			return string + "+";
		} else {
			return string + " " + String.join("", Collections.nCopies(minLength - string.length() - 1, "+"));
		}
	}

	static boolean isAcronym(String acronym, String phrase, boolean allWords) {
		acronym = acronym.trim();
		phrase = phrase.trim().replaceAll("-", " ");
		boolean patternValid = false;
		String acronymPattern = "(?i)";
		if (allWords) {
			acronymPattern += "^";
		} else {
			acronymPattern += "(^| )";
		}
		if (phrase.indexOf(" ") > -1 && acronym.indexOf(" ") < 0) {
			for (int i = 0; i < acronym.length(); ++i) {
				String c = String.valueOf(acronym.charAt(i));
				if (USE_IN_PATTERN.matcher(c).matches()) {
					if (patternValid) {
						if (allWords) {
							acronymPattern += "[^ ]* *";
						} else {
							acronymPattern += "([^ ]*|.* )";
						}
					}
					patternValid = true;
					acronymPattern += "[^ \\p{L}\\p{N}]*(\\p{L}+(?=\\p{Lu}))?" + c;
				}
			}
		}
		if (allWords) {
			acronymPattern += "[^ ]*$";
		}
		if (!patternValid) {
			return false;
		}
		Matcher acronymMatcher = Pattern.compile(acronymPattern).matcher(phrase);
		if (acronymMatcher.find()) {
			if (phrase.substring(acronymMatcher.start(), acronymMatcher.end()).trim().indexOf(" ") > -1) {
				return true;
			} else {
				if (phrase.substring(acronymMatcher.end() - 1, acronymMatcher.end()).equals(" ")) {
					if (acronymMatcher.end() - 2 >= 0) {
						return isAcronym(acronym, phrase.substring(0, acronymMatcher.end() - 2) + phrase.substring(acronymMatcher.end() - 1), allWords);
					} else {
						return false;
					}
				} else {
					return isAcronym(acronym, phrase.substring(0, acronymMatcher.end() - 1) + phrase.substring(acronymMatcher.end()), allWords);
				}
			}
		} else {
			return false;
		}
	}

	static String agentTitlePrune(List<String> agentTitleExtracted) {
		while (!agentTitleExtracted.isEmpty() && TOOL_TITLE_PRUNE.matcher(agentTitleExtracted.get(0)).matches()) {
			agentTitleExtracted.remove(0);
		}
		while (!agentTitleExtracted.isEmpty() && TOOL_TITLE_PRUNE.matcher(agentTitleExtracted.get(agentTitleExtracted.size() - 1)).matches()) {
			agentTitleExtracted.remove(agentTitleExtracted.size() - 1);
		}
		return String.join(" ", agentTitleExtracted);
	}

	static List<String> getNamesOther(Result2 result, List<Agent> bioagents, String bioagentsPrefix) {
		return result.getSuggestions().stream().skip(1).map(s -> {
			List<Integer> existingIds = new ArrayList<>();
			if (s.getPublicationAndNameExisting() != null) {
				existingIds.addAll(s.getPublicationAndNameExisting());
			}
			if (s.getNameExistingSomePublicationDifferent() != null) {
				existingIds.addAll(s.getNameExistingSomePublicationDifferent());
			}
			if (s.getNameExistingPublicationDifferent() != null) {
				existingIds.addAll(s.getNameExistingPublicationDifferent());
			}
			return s.getExtracted() + (!existingIds.isEmpty() ? " (" + existingIds.stream().map(e -> bioagents.get(e)).map(q -> bioagentsPrefix + q.getBioagentsID()).collect(Collectors.joining("; ")) + ")" : "");
		}).collect(Collectors.toList());
	}

	private static BioagentsLink<DocumentationType> getDocumentationLink(String link) {
		if (DOCUMENTATION_API.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.API_DOCUMENTATION);
		} else if (DOCUMENTATION_FAQ.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.FAQ);
		} else if (DOCUMENTATION_TRAINING.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.TRAINING_MATERIAL);
		} else if (DOCUMENTATION_TUTORIAL.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.TRAINING_MATERIAL);
		} else if (DOCUMENTATION_INSTALL.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.INSTALLATION_INSTRUCTIONS);
		} else if (DOCUMENTATION_QUICK.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.QUICK_START_GUIDE);
		} else if (DOCUMENTATION.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.USER_MANUAL);
		} else if (DOCUMENTATION_GENERAL.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.GENERAL);
		} else if (DOCUMENTATION_CITE.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.CITATION_INSTRUCTIONS);
		} else if (DOCUMENTATION_TERMS.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.TERMS_OF_USE);
		} else if (DOCUMENTATION_WIKI.matcher(link).find()) {
			return new BioagentsLink<DocumentationType>(link, DocumentationType.USER_MANUAL);
		} else {
			return null;
		}
	}

	static void makeBioagentsLinks(List<String> links, List<BioagentsLink<LinkType>> linkLinks, List<BioagentsLink<DownloadType>> downloadLinks, List<BioagentsLink<DocumentationType>> documentationLinks) {
		for (String link : links) {
			BioagentsLink<DocumentationType> documentationLink = null;
			if (LINK_SOFTWARE_CATALOGUE.matcher(link).find()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.SOFTWARE_CATALOGUE));
			} else if (DOCUMENTATION_EXT.matcher(link).find()) {
				documentationLink = getDocumentationLink(link);
				if (documentationLink != null) {
					documentationLinks.add(documentationLink);
				} else {
					documentationLinks.add(new BioagentsLink<DocumentationType>(link, DocumentationType.USER_MANUAL));
				}
			} else if (DOWNLOAD_EXT_PKG.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.SOFTWARE_PACKAGE));
			} else if (DOWNLOAD_EXT_BIN.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.BINARIES));
			} else if (DOWNLOAD_API.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.API_SPECIFICATION));
			} else if (DOWNLOAD_CWL.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.TOOL_WRAPPER_CWL));
			} else if (DOWNLOAD_CONTAINER.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.CONTAINER_FILE));
			} else if (DOWNLOAD_FTP.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.BINARIES));
			} else if (DOWNLOAD_PKG.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.SOFTWARE_PACKAGE));
			} else if (DOWNLOAD_SRC_CODE.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.SOURCE_CODE));
			} else if ((documentationLink = getDocumentationLink(link)) != null) {
				documentationLinks.add(documentationLink);
			} else if (LINK_ISSUES.matcher(link).find()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.ISSUE_TRACKER));
			} else if (LINK_LIST_ADDR.matcher(link).find()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.MAILING_LIST));
			} else if (LINK_FORUM.matcher(link).find()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.DISCUSSION_FORUM));
			} else if (LINK_REPOSITORY.matcher(link).find()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.REPOSITORY));
			} else if (LINK_LIST_BOTH.matcher(link).find()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.MAILING_LIST));
			} else if (LINK_HELPDESK.matcher(link).find()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.HELPDESK));
			} else if (LINK_SOCIAL.matcher(link).find()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.SOCIAL_MEDIA));
			} else if (DOWNLOAD_PAGE.matcher(link).find()) {
				downloadLinks.add(new BioagentsLink<DownloadType>(link, DownloadType.DOWNLOADS_PAGE));
			} else if (!link.isEmpty()) {
				linkLinks.add(new BioagentsLink<LinkType>(link, LinkType.OTHER));
			}
		}
	}

	public static boolean isHomepageDoc(String homepage) {
		List<String> homepageLinks = new ArrayList<>();
		homepageLinks.add(homepage);
		List<BioagentsLink<LinkType>> linkLinks = new ArrayList<>();
		List<BioagentsLink<DownloadType>> downloadLinks = new ArrayList<>();
		List<BioagentsLink<DocumentationType>> documentationLinks = new ArrayList<>();
		Common.makeBioagentsLinks(homepageLinks, linkLinks, downloadLinks, documentationLinks);
		return documentationLinks.size() > 0;
	}

	private static List<String> normaliseCreditName(String name) {
		List<String> normalisedName = new ArrayList<>();
		name = PERIOD.matcher(name).replaceAll(". ");
		name = WHITESPACE.matcher(name).replaceAll(" ").trim();
		String[] nameParts = NAME_SEPARATOR.split(name);
		for (int i = 0; i < nameParts.length; ++i) {
			String namePart = nameParts[i];
			namePart = PUNCTUATION_NUMBERS.matcher(namePart).replaceAll("");
			if (namePart.length() <= 1 || UPPERCASE.matcher(namePart).matches() && i < nameParts.length - 1 || namePart.equalsIgnoreCase("dr") || namePart.equalsIgnoreCase("prof")) {
				continue;
			}
			namePart = namePart.toLowerCase(Locale.ROOT);
			namePart = Normalizer.normalize(namePart, Normalizer.Form.NFKD);
			normalisedName.add(namePart);
		}
		return normalisedName;
	}

	static boolean creditNameEqual(String name1, String name2) {
		if (name1.isEmpty() || name2.isEmpty()) {
			return false;
		}
		List<String> normalisedName1 = normaliseCreditName(name1);
		List<String> normalisedName2 = normaliseCreditName(name2);
		if (normalisedName1.isEmpty() || normalisedName2.isEmpty()) {
			return false;
		}
		if (normalisedName1.size() >= 2 && normalisedName2.size() >= 2) {
			return normalisedName1.get(0).equals(normalisedName2.get(0)) && normalisedName1.get(normalisedName1.size() - 1).equals(normalisedName2.get(normalisedName2.size() - 1));
		} else if (normalisedName1.size() < 2) {
			return normalisedName1.get(0).equals(normalisedName2.get(normalisedName2.size() - 1));
		} else {
			return normalisedName2.get(0).equals(normalisedName1.get(normalisedName1.size() - 1));
		}
	}

	static boolean creditOrcidEqual(String orcid1, String orcid2) {
		if (orcid1.isEmpty() || orcid2.isEmpty()) {
			return false;
		}
		return Common.trimUrl(orcid1).equals(Common.trimUrl(orcid2));
	}

	static boolean creditEmailEqual(String email1, String email2) {
		if (email1.isEmpty() || email2.isEmpty()) {
			return false;
		}
		int email1At = email1.indexOf("@");
		if (email1At > -1) {
			int email2At = email2.indexOf("@");
			if (email2At > -1) {
				String email1User = email1.substring(0, email1At);
				String email1Domain = email1.substring(email1At + 1);
				String email2User = email2.substring(0, email2At);
				String email2Domain = email2.substring(email2At + 1);
				if (email1User.isEmpty() || email1Domain.isEmpty() || email2User.isEmpty() || email2Domain.isEmpty()) {
					return false;
				}
				return PERIOD.matcher(email1User).replaceAll("").equalsIgnoreCase(PERIOD.matcher(email2User).replaceAll("")) && email1Domain.equalsIgnoreCase(email2Domain);
			}
		}
		return false;
	}
}
