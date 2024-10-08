/*
 * Copyright © 2018, 2019, 2020, 2023 Erik Jaaniso
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.publication.CorrespAuthor;
import org.edamontology.pubfetcher.core.db.publication.Publication;
import org.edamontology.pubfetcher.core.db.publication.PublicationIds;

import org.edamontology.edammap.core.idf.Idf;
import org.edamontology.edammap.core.input.json.DocumentationType;
import org.edamontology.edammap.core.input.json.DownloadType;
import org.edamontology.edammap.core.input.json.LinkType;
import org.edamontology.edammap.core.preprocessing.PreProcessor;

public final class Pass1 {

	private static final Logger logger = LogManager.getLogger();

	private static final int ABSTRACT_MAX_LENGTH = 5000;
	private static final int FULLTEXT_MAX_LENGTH = 200000;

	private static final Pattern TOOL_TITLE_INVALID = Pattern.compile("(?i)^(correction|erratum)( to)?$");
	private static final Pattern TOOL_TITLE_SEPARATOR = Pattern.compile("(?i),? (and|&) ");
	private static final Pattern TOOL_TITLE_SEPARATOR_ALL = Pattern.compile("(?i)(,? (and|&) )|(, )");
	private static final int TOOL_TITLE_SEPARATOR_MAX_WORDS = 5;
	private static final int TOOL_TITLE_STANDALONE_MAX_CHARS = 18;

	private static final Pattern ACRONYM_STOP = Pattern.compile("(?i)(http:|https:|ftp:|;|, |: )");

	private static final int COMPOUND_WORDS = 5;
	private static final double COMPOUND_DIVIDER = 2;

	private static final double TOOL_TITLE_MULTIPLIER = 24;

	private static final double TIER_1_MULTIPLIER = 6;
	private static final double TIER_2_MULTIPLIER = 3;
	private static final double TIER_3_MULTIPLIER = 1.5;
	private static final double BEFORE_AFTER_LIMIT = 72;

	private static final Pattern LINK_WWW = Pattern.compile("^[^.]*www[^.]*\\.");
	private static final Pattern LINK_END_REMOVE = Pattern.compile("([.?]\\p{Lu}|--)[\\p{L}\\p{N}'-]*$");
	private static final Pattern LINK_EMAIL_END = Pattern.compile("@[a-zA-Z0-9.-]+\\.[a-z]{2,}$");
	private static final Pattern LINK_EMAIL_REMOVE = Pattern.compile("\\.[^/]+" + LINK_EMAIL_END);

	private static final Pattern PATH_QUERY = Pattern.compile("\\?.*=");
	private static final Pattern PATH_PERIOD = Pattern.compile("(\\.[^\\p{N}][^.]*$)|(\\.$)");
	private static final Pattern PATH_NUMBER = Pattern.compile("^[vV-]?\\p{N}+(\\.\\p{N}+)?$");
	private static final Pattern PATH_ONE_UPPERCASE = Pattern.compile("^..*\\p{Lu}.*$");
	private static final Pattern PATH_UNI = Pattern.compile("^uni([\\p{L}\\p{N}]?-.*|[\\p{L}\\p{N}]{0,2})$");

	private static final double PATH_IDF_MIN = 0.5;
	private static final double PATH_IDF_MIN_2 = 0.24;

	private static final Pattern GOOD_START = Pattern.compile("^(\\p{Lu}|.[^\\p{Ll}-]|.-[^\\p{Ll}]|.[^-]*[^\\p{L}-])[^-]*$");
	private static final Pattern GOOD_END = Pattern.compile("^(.*[^\\p{Ll}]|.*[^\\p{Ll}-].|\\p{Lu}.*|..)$");
	private static final Pattern GOOD_START_MULTI = Pattern.compile("^[^ ]+( \\p{Lu}[^ ]*)*( v| ver| version)?( \\p{Lu}[^ ]*| ([vV](er(sion)?)?)?\\p{N}+([.-]\\p{N}+)*)$");
	private static final Pattern TO_LINK = Pattern.compile("^[^ ]*[^ \\p{Ll}-][^ ]*( [^ ]*[^ \\p{Ll}-][^ ]*)*$");
	private static final Pattern NOT_TO_LINK = Pattern.compile("^[^ ]( [^ ])*$");

	private static final Pattern LINK_TWO_PART = Pattern.compile("^[^./]+\\.[^./]+$");

	private static final double LINK_MULTIPLIER_ABSTRACT = 24;
	private static final double LINK_MULTIPLIER_ABSTRACT_MINIMUM = LINK_MULTIPLIER_ABSTRACT / 2;
	private static final double LINK_MULTIPLIER_ABSTRACT_AUGMENTATION = LINK_MULTIPLIER_ABSTRACT / 4;
	private static final double LINK_MULTIPLIER_ABSTRACT_NEW = LINK_MULTIPLIER_ABSTRACT / 2;
	private static final double LINK_MULTIPLIER_FULLTEXT = LINK_MULTIPLIER_ABSTRACT / 2;

	private static final double TOP_SCORE_LIMIT = 24;
	private static final int SUGGESTION_LIMIT = 5;

	// TODO move BIOTOOLS_SCHEMA stuff to org.edamontology.edammap.core.input.json
	private static final int BIOTOOLS_SCHEMA_NAME_MIN = 1;
	private static final int BIOTOOLS_SCHEMA_NAME_MAX = 100;
	private static final String BIOTOOLS_SCHEMA_NAME_CHARS = " A-Za-z0-9+.,\\-_:;()";
	private static final Pattern BIOTOOLS_SCHEMA_NAME_PATTERN = Pattern.compile("^[" + BIOTOOLS_SCHEMA_NAME_CHARS + "]*$");
	private static final Pattern BIOTOOLS_SCHEMA_NAME_INVALID_CHAR = Pattern.compile("[^" + BIOTOOLS_SCHEMA_NAME_CHARS + "]");
	private static final Pattern BIOTOOLS_SCHEMA_NAME_LETTER = Pattern.compile("[A-Za-z]");
	private static final String[][] BIOTOOLS_SCHEMA_NAME_REPLACEMENTS = {
			{ "\u2010", "-" },
			{ "&", " and " },
			{ "@", "a" },
			{ "α", "a" }, { "β", "b" }, { "μ", "u" }, { "µ", "u" },
			{ "²", "2" }
	};
	private static final Pattern BIOTOOLS_SCHEMA_NAME_APOSTROPHE_QUOTATION_SPACE = Pattern.compile("([\\p{L}\\p{N}])['\"\\u0060\\u00B4\\u2018\\u2019\\u02BC\\u201B\\u0091\\u0092\\u00AB\\u00BB\\u201A\\u201C\\u201D\\u201E\\u201F\\u2039\\u203A\\u2E42]+([\\p{L}\\p{N}])");
	private static final Pattern BIOTOOLS_SCHEMA_PMID_PATTERN = Pattern.compile("^[1-9][0-9]{0,8}$");
	private static final Pattern BIOTOOLS_SCHEMA_PMCID_PATTERN = Pattern.compile("^(PMC)[1-9][0-9]{0,8}$");
	private static final Pattern BIOTOOLS_SCHEMA_DOI_PATTERN = Pattern.compile("^10\\.[0-9]{4,9}/[\\[\\]<>A-Za-z0-9:;)(_/.-]+$");
	private static final int BIOTOOLS_SCHEMA_CREDIT_NAME_MIN = 1;
	private static final int BIOTOOLS_SCHEMA_CREDIT_NAME_MAX = 100;
	private static final Pattern BIOTOOLS_SCHEMA_CREDIT_ORCIDID_PATTERN = Pattern.compile("^https?://orcid\\.org/[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9X]$");
	private static final Pattern BIOTOOLS_SCHEMA_CREDIT_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9_]+([-+.'][A-Za-z0-9_]+)*@[A-Za-z0-9_]+([-.][A-Za-z0-9_]+)*\\.[A-Za-z0-9_]+([-.][A-Za-z0-9_]+)*$");

	private static final String LEFTOVER_AVAILABLE = "(available|availability|accessible|accessed)";
	private static final Pattern LEFTOVER_EXCLUDE = Pattern.compile("(?i)(dataset|(^|[^\\p{L}-])data([^\\p{L}-]|$)|doi\\.org/10\\.(5061|21227|17632|7910|7946|15468))");

	private static final Pattern FIX_LINK = Pattern.compile("([.]?[\"(\\[{<>}\\])]+[.]?|\\.\\p{Lu}|--)[\\p{L}\\p{N}'-]+$");
	private static final Pattern FIX_LINK_KEEP1 = Pattern.compile("(\\.[\\p{Ll}\\p{N}]+)\\p{Lu}[\\p{L}\\p{N}'-]*$");
	private static final Pattern FIX_LINK_KEEP2 = Pattern.compile("(/)\\.[\\p{L}\\p{N}'-]*$");
	private static final Pattern FIX_LINK_EMAIL1 = Pattern.compile("[.]?[^/.]+@[^/]+\\.[^/]+$");
	private static final Pattern FIX_LINK_EMAIL2 = Pattern.compile("[.]?[^/.]+\\.[^/.]+@[^/]+\\.[^/]+$");
	private static final Pattern FIX_LINK_EMAIL3 = Pattern.compile("[.]?[^/.]+\\.[^/.]+\\.[^/.]+@[^/]+\\.[^/]+$");
	private static final Pattern URL_FIX = Pattern.compile("^([-\\p{L}\\p{N};/:@&=+$,_.!~*'()%]+(\\?[-\\p{L}\\p{N};/?:@&=+$,_.!~*'()%]*)?(#[-\\p{L}\\p{N};/?:@&=+$,_.!~*'()%]*)?)");

	private static List<Integer> acronyms(String sentence, PreProcessor preProcessor) {
		List<Integer> acronyms = new ArrayList<>();
		int previousEnd = 0;
		int index = 0;
		int begin = 0;
		int end = 0;
		while (begin != -1 && end != -1) {
			begin = sentence.indexOf(" (", previousEnd);
			if (begin != -1) {
				end = sentence.indexOf(")", begin + 2);
				if (end != -1) {
					String before = sentence.substring(previousEnd, begin).trim();
					List<String> beforeExtracted = preProcessor.extract(before);
					preProcessor.process(before, beforeExtracted); // align indexes

					String inside = sentence.substring(begin, end + 1).trim();
					List<String> insideExtracted = preProcessor.extract(inside);
					preProcessor.process(inside, insideExtracted); // align indexes

					Matcher acronymStop = ACRONYM_STOP.matcher(inside);
					if (acronymStop.find()) {
						inside = inside.substring(0, acronymStop.start()).trim();
					}

					if (inside.contains(" ") && beforeExtracted.size() > 0 && insideExtracted.size() > 0) {
						if (Common.isAcronym(beforeExtracted.get(beforeExtracted.size() - 1), inside, false)) {
							acronyms.add(index + beforeExtracted.size() - 1);
						}
					} else if (!inside.contains(" ") && beforeExtracted.size() > 1 && insideExtracted.size() > 0) {
						if (Common.isAcronym(inside, before, false)) {
							acronyms.add(-(index + beforeExtracted.size()));
						}
					}

					index += beforeExtracted.size();
					index += insideExtracted.size();
					previousEnd = sentence.indexOf(" ", end + 1);
					if (previousEnd == -1) break;
				}
			}
		}
		return acronyms;
	}

	private static Integer firstAcronymIndex(String sentence, PreProcessor preProcessor) {
		List<Integer> acronyms = acronyms(sentence, preProcessor);
		if (!acronyms.isEmpty()) {
			int acronym = acronyms.get(0); // first acronym
			if (acronym < 0) acronym = -acronym; // index
			return acronym;
		} else {
			return null;
		}
	}

	private static boolean agentTitleScore(String agentTitle, PreProcessor preProcessor, Map<String, Double> scores, Map<String, String> processedToExtracted, boolean pruned) {
		if (agentTitle.isEmpty()) {
			return false;
		}
		// agentTitle is already agentTitleExtractedString
		List<String> agentTitleProcessed = preProcessor.process(agentTitle);
		String agentTitleProcessedString = String.join(" ", agentTitleProcessed);

		Double agentTitleExistingScore = scores.get(agentTitleProcessedString);
		if (agentTitleExistingScore != null) {
			agentTitleExistingScore *= TOOL_TITLE_MULTIPLIER;
			if (agentTitleExistingScore > TOOL_TITLE_MULTIPLIER) {
				scores.put(agentTitleProcessedString, agentTitleExistingScore);
			} else {
				scores.put(agentTitleProcessedString, TOOL_TITLE_MULTIPLIER);
			}
		} else if (!pruned) {
			scores.put(agentTitleProcessedString, TOOL_TITLE_MULTIPLIER / agentTitleProcessed.size());
			processedToExtracted.put(agentTitleProcessedString, agentTitle);
		}

		if (agentTitleExistingScore != null) {
			return true;
		} else {
			return false;
		}
	}

	private static void beforeAfterScore(String key, Map<String, Double> scores, Map<String, Double> beforeAfterAdded, boolean tier1, boolean tier2, boolean tier3, boolean twice) {
		Double newBeforeAfterAdded = beforeAfterAdded.get(key);
		if (newBeforeAfterAdded == null) {
			newBeforeAfterAdded = 1.0;
		}
		double multiplier = 1;
		if (tier1) {
			multiplier = TIER_1_MULTIPLIER;
		} else if (tier2) {
			multiplier = TIER_2_MULTIPLIER;
		} else if (tier3) {
			multiplier = TIER_3_MULTIPLIER;
		}
		if (twice) {
			multiplier *= 2;
		}
		if (newBeforeAfterAdded * multiplier > BEFORE_AFTER_LIMIT) {
			multiplier = BEFORE_AFTER_LIMIT / newBeforeAfterAdded;
		}
		newBeforeAfterAdded *= multiplier;
		beforeAfterAdded.put(key, newBeforeAfterAdded);
		scores.put(key, scores.get(key) * multiplier);
	}

	private static List<String> breakLinks(List<String> links, List<String> allLinks) {
		for (int i = 0; i < links.size(); ++i) {
			String link = links.get(i);
			String linkStart = "";
			Matcher trimStart = Common.LINK_COMPARE_START.matcher(link);
			if (trimStart.find()) {
				linkStart = link.substring(0, trimStart.end());
				link = link.substring(trimStart.end());
			}
			int linkMax = 0;
			int schemaStart = 0;
			int schemaEnd = 0;
			for (String otherLink : allLinks) {
				if (!link.equals(otherLink) && link.startsWith(otherLink) && otherLink.length() > linkMax) {
					String rest = link.substring(otherLink.length());
					Matcher schemaMatcher = Common.LINK_COMPARE_SCHEMA.matcher(rest);
					if (schemaMatcher.find()) {
						linkMax = otherLink.length();
						schemaStart = schemaMatcher.start();
						schemaEnd = schemaMatcher.end();
					}
				}
			}
			if (linkMax > 0) {
				links.set(i, linkStart + link.substring(0, linkMax));
				if (linkMax + schemaEnd < link.length()) {
					links.add(i + 1, link.substring(linkMax + schemaStart));
				}
			} else {
				Matcher schemaMatcher = Common.LINK_COMPARE_SCHEMA.matcher(link);
				if (schemaMatcher.find()) {
					links.set(i, linkStart + link.substring(0, schemaMatcher.start()));
					if (schemaMatcher.end() < link.length()) {
						links.add(i + 1, link.substring(schemaMatcher.start()));
					}
				}
			}
		}
		return links;
	}

	private static String fromLink(String link, PreProcessor preProcessor, Idf queryIdf, List<String> hostIgnore) {
		int schema = link.indexOf("://");
		if (schema > -1) {
			link = link.substring(schema + 3);
		}
		link = LINK_WWW.matcher(link).replaceFirst("");

		link = LINK_END_REMOVE.matcher(link).replaceFirst("");

		int firstSlash = link.indexOf('/');

		Matcher emailEnd = LINK_EMAIL_END.matcher(link);
		if (emailEnd.find()) {
			if (firstSlash > -1) {
				Matcher emailRemove = LINK_EMAIL_REMOVE.matcher(link);
				if (emailRemove.find()) {
					link = link.substring(0, emailRemove.start());
				} else {
					return "";
				}
			} else {
				link = link.substring(0, emailEnd.start());
			}
		}

		String host = firstSlash > -1 ? link.substring(0, firstSlash) : link;
		if (host.equals("dx.doi.org") || host.equals("doi.org")
				|| host.equals("goo.gl") || host.equals("youtube.com")
				|| host.equals("proteomecentral.proteomexchange.org")) {
			return "";
		}

		int pathCount = 0;

		String bestPath = null;
		double bestPathScore = 0;

		for (int slash = link.lastIndexOf('/'); slash > -1; slash = link.lastIndexOf('/')) {
			String path = link.substring(slash + 1);
			link = link.substring(0, slash);

			if (path.isEmpty()) continue;

			++pathCount;

			if (path.startsWith("~") || path.startsWith("∼")) continue; // different UTF characters
			if (path.startsWith("%")) continue;

			if (pathCount == 1) {
				int equals = path.lastIndexOf('=');
				if (equals > -1) {
					int query = path.indexOf('?');
					if (query > -1) {
						link += "/" + path.substring(0, query);
					}
					path = path.substring(equals + 1);
					int fragment = path.indexOf('#');
					if (fragment > -1) {
						path = path.substring(fragment + 1);
					}
				} else {
					int fragment = path.indexOf('#');
					if (fragment > -1) {
						link += "/" + path.substring(0, fragment);
						path = path.substring(fragment + 1);
					}
				}
			} else {
				Matcher query = PATH_QUERY.matcher(path);
				if (query.find()) {
					continue;
				}
				if (path.startsWith("#")) continue;
			}

			path = PATH_PERIOD.matcher(path).replaceFirst("");
			if (path.endsWith(".tar")) {
				path = path.substring(0, path.length() - 4);
			}

			Matcher pathSplit = Common.PATH_SPLIT.matcher(path);
			if (pathSplit.find() && pathSplit.find()) {
				// don't use pathSplit, as its state has changed
				path = Common.PATH_SPLIT.matcher(path).replaceAll(" ").trim();
			}

			if (path.isEmpty()) continue;
			if (PATH_NUMBER.matcher(path).matches()) continue;

			double score = queryIdf.getIdf(String.join("", preProcessor.process(path)));
			if (score > bestPathScore) {
				bestPath = path;
				bestPathScore = score;
			}

			if (PATH_ONE_UPPERCASE.matcher(path).matches() || score > PATH_IDF_MIN) {
				return path;
			}
		}

		if (pathCount < 3) {
			int period = link.indexOf('.');
			if (period > -1) {
				String rest = link.substring(period + 1);
				link = link.substring(0, period);
				if (link.length() > 1
						&& !link.startsWith("bioinf") && !link.endsWith("lab")
						&& !PATH_UNI.matcher(link).matches() && !PATH_NUMBER.matcher(link).matches()
						&& !rest.equals("edu") && !rest.startsWith("edu.") && !rest.startsWith("ac.")
						&& !hostIgnore.contains(link)) {
					return link;
				}
			}
		}

		if (bestPath != null && bestPathScore > PATH_IDF_MIN_2) {
			return bestPath;
		} else {
			return "";
		}
	}

	private static boolean agentTitleMatch(String agentTitle, PreProcessor preProcessor, List<String> matchedKeys, boolean linkTwoPart, String linkProcessed, String fromLink, String link, Map<String, List<String>> links) {
		if (agentTitle.isEmpty()) {
			return false;
		}
		// agentTitle is already agentTitleExtractedString
		List<String> agentTitleProcessed = preProcessor.process(agentTitle);
		String agentTitleProcessedString = String.join(" ", agentTitleProcessed);

		String agentTitleProcessedHyphen = String.join(" ", preProcessor.process(agentTitle.replaceAll("-", " ")));
		String fromLinkProcessedHyphen = String.join(" ", preProcessor.process(fromLink.replaceAll("-", " ")));

		boolean matches = false;

		if (agentTitleProcessed.size() <= COMPOUND_WORDS && !matchedKeys.contains(agentTitleProcessedString) && !(linkTwoPart && linkProcessed.equals(agentTitleProcessedString))) {
			String agentTitleProcessedStringTrimmed = Common.TOOL_TITLE_TRIM.matcher(agentTitleProcessedHyphen).replaceFirst("");
			String fromLinkProcessedTrimmed = Common.TOOL_TITLE_TRIM.matcher(fromLinkProcessedHyphen).replaceFirst("");
			String agentTitleCompare = agentTitleProcessedStringTrimmed.replaceAll(" ", "");
			String fromLinkCompare = fromLinkProcessedTrimmed.replaceAll(" ", "");
			if (agentTitleCompare.length() < 2 || fromLinkCompare.length() < 2) return false;

			if (agentTitleCompare.startsWith(fromLinkCompare) || agentTitleCompare.endsWith(fromLinkCompare)
					|| fromLinkCompare.startsWith(agentTitleCompare) || fromLinkCompare.endsWith(agentTitleCompare)) {
				matches = true;
			}

			if (!matches) {
				String firstLetter = fromLinkCompare.substring(0, 1);
				String pattern = "(^| )" + (Common.USE_IN_PATTERN.matcher(firstLetter).matches() ? firstLetter : ".");
				for (int i = 1; i < fromLinkCompare.length(); ++i) {
					String letter = fromLinkCompare.substring(i, i + 1);
					pattern += "(.* )?" + (Common.USE_IN_PATTERN.matcher(letter).matches() ? letter : ".");
				}
				if (Pattern.compile(pattern).matcher(agentTitleProcessedStringTrimmed).find()) {
					matches = true;
				}
			}
			if (!matches) {
				String firstLetter = agentTitleCompare.substring(0, 1);
				String pattern = "(^| )" + (Common.USE_IN_PATTERN.matcher(firstLetter).matches() ? firstLetter : ".");
				for (int i = 1; i < agentTitleCompare.length(); ++i) {
					String letter = agentTitleCompare.substring(i, i + 1);
					pattern += "(.* )?" + (Common.USE_IN_PATTERN.matcher(letter).matches() ? letter : ".");
				}
				if (Pattern.compile(pattern).matcher(fromLinkProcessedTrimmed).find()) {
					matches = true;
				}
			}
			if (!matches) {
				String firstLetter = fromLinkCompare.substring(0, 1);
				String pattern = "^" + (Common.USE_IN_PATTERN.matcher(firstLetter).matches() ? firstLetter : ".");
				for (int i = 1; i < fromLinkCompare.length(); ++i) {
					String letter = fromLinkCompare.substring(i, i + 1);
					pattern += "([^ ]*|.* )" + (Common.USE_IN_PATTERN.matcher(letter).matches() ? letter : ".");
				}
				pattern += "[^ ]*$";
				if (Pattern.compile(pattern).matcher(agentTitleProcessedStringTrimmed).matches()) {
					matches = true;
				}
			}
			if (!matches) {
				String firstLetter = agentTitleCompare.substring(0, 1);
				String pattern = "^" + (Common.USE_IN_PATTERN.matcher(firstLetter).matches() ? firstLetter : ".");
				for (int i = 1; i < agentTitleCompare.length(); ++i) {
					String letter = agentTitleCompare.substring(i, i + 1);
					pattern += "([^ ]*|.* )" + (Common.USE_IN_PATTERN.matcher(letter).matches() ? letter : ".");
				}
				pattern += "[^ ]*$";
				if (Pattern.compile(pattern).matcher(fromLinkProcessedTrimmed).matches()) {
					matches = true;
				}
			}

			if (matches) {
				List<String> keyLinks = links.get(agentTitleProcessedString);
				if (keyLinks == null) {
					keyLinks = new ArrayList<>();
					links.put(agentTitleProcessedString, keyLinks);
				}
				keyLinks.add(link);
				matchedKeys.add(agentTitleProcessedString);
			}
		}

		return matches;
	}

	private static Map<String, List<String>> links(List<String> partLinks, PreProcessor preProcessor, Idf queryIdf, List<String> hostIgnore,
			Set<String> keys, Map<String, String> processedToExtracted, List<List<String>> processed, String titleWithoutLinks, String abstractWithoutLinks,
			String agentTitle, String agentTitlePruned, String agentTitleAcronym) {
		Map<String, List<String>> links = new LinkedHashMap<>();

		for (String link : partLinks) {
			String fromLink = fromLink(link, preProcessor, queryIdf, hostIgnore);
			if (fromLink.isEmpty()) {
				continue;
			}
			String fromLinkProcessed = String.join("", preProcessor.process(fromLink));
			if (fromLinkProcessed.isEmpty()) {
				continue;
			}

			List<String> matchedKeys = new ArrayList<>();

			boolean linkTwoPart = false;
			if (LINK_TWO_PART.matcher(link).matches()) {
				linkTwoPart = true;
			}
			String linkProcessed = String.join("", preProcessor.process(link));

			String longestStart = "";
			String longestEnd = "";

			for (String key : keys) {
				if (linkTwoPart && linkProcessed.equals(key)) {
					continue;
				}
				if ((fromLinkProcessed + fromLinkProcessed).equals(key)) {
					continue;
				}

				String keyCompare = key.replaceAll(" ", "");
				boolean found = false;

				if (key.equals(fromLinkProcessed)) {
					found = true;
				}

				if (!found && keyCompare.equals(fromLinkProcessed)) {
					found = true;
				}

				String keyExtracted = processedToExtracted.get(key);
				String keyExtractedLower = keyExtracted.toLowerCase(Locale.ROOT);

				if (!found && !key.contains(" ") && keyCompare.startsWith(fromLinkProcessed)) {
					int keyExtractedIndex = 0;
					for (int i = 0; i < fromLinkProcessed.length(); ++i) {
						while (keyExtractedIndex < keyExtractedLower.length() && fromLinkProcessed.charAt(i) != keyExtractedLower.charAt(keyExtractedIndex)) {
							++keyExtractedIndex;
						}
						if (keyExtractedIndex < keyExtractedLower.length()) {
							++keyExtractedIndex;
						} else {
							break;
						}
					}
					if (keyExtractedIndex > 0) --keyExtractedIndex;

					String goodStart = keyExtracted.substring(keyExtractedIndex);
					if (GOOD_START.matcher(goodStart).matches() && !(goodStart.length() == 2 && goodStart.charAt(1) == 's')) {
						found = true;
					} else if (goodStart.length() >= 2 && goodStart.charAt(1) == '-') {
						int hyphenBased = 0;
						for (List<String> sentenceProcessed : processed) {
							for (String wordProcessed : sentenceProcessed) {
								if (wordProcessed.equals(key)) {
									++hyphenBased;
								}
							}
						}
						if (hyphenBased > 1) {
							found = true;
						}
					}
				}

				if (!found && !key.contains(" ") && keyCompare.endsWith(fromLinkProcessed) && !LINK_TWO_PART.matcher(keyExtracted).matches()) {
					int keyExtractedIndex = keyExtractedLower.length() - 1;
					for (int i = fromLinkProcessed.length() - 1; i >= 0; --i) {
						while (keyExtractedIndex >= 0 && fromLinkProcessed.charAt(i) != keyExtractedLower.charAt(keyExtractedIndex)) {
							--keyExtractedIndex;
						}
						if (keyExtractedIndex >= 0) {
							--keyExtractedIndex;
						} else {
							break;
						}
					}
					if (keyExtractedIndex < keyExtractedLower.length()) ++keyExtractedIndex;
					if (keyExtractedIndex < keyExtractedLower.length()) ++keyExtractedIndex;

					String goodEnd = keyExtracted.substring(0, keyExtractedIndex);
					if (GOOD_END.matcher(goodEnd).matches()) {
						found = true;
					}
				}

				if (!found && key.contains(" ") && keyCompare.startsWith(fromLinkProcessed)) {
					if (GOOD_START_MULTI.matcher(keyExtracted).matches()) {
						String[] words = keyExtracted.split(" ");
						String patternString = "";
						for (int i = 0; i < words.length; ++i) {
							String firstLetter = words[i].substring(0, 1);
							patternString += Common.USE_IN_PATTERN.matcher(firstLetter).matches() ? firstLetter : ".";
							if (words[i].length() > 1) {
								patternString += "[^ ]*";
								String lastLetter = words[i].substring(words[i].length() - 1, words[i].length());
								patternString += Common.USE_IN_PATTERN.matcher(lastLetter).matches() ? lastLetter : ".";
							}
							if (i < words.length - 1) {
								patternString += "([ ./]+)";
							}
						}

						Pattern pattern = Pattern.compile(patternString);
						if (pattern.matcher(titleWithoutLinks).find() || pattern.matcher(abstractWithoutLinks).find()) {
							found = true;
						}
					}
				}

				if (!found && fromLinkProcessed.startsWith(keyCompare)) {
					if (TO_LINK.matcher(keyExtracted).matches() && !NOT_TO_LINK.matcher(keyExtracted).matches()) {
						if (key.length() > longestStart.length()) {
							longestStart = key;
						}
					}
				}

				if (!found && fromLinkProcessed.endsWith(keyCompare)) {
					if (TO_LINK.matcher(keyExtracted).matches() && !NOT_TO_LINK.matcher(keyExtracted).matches()) {
						if (key.length() > longestEnd.length()) {
							longestEnd = key;
						}
					}
				}

				if (!found && Common.isAcronym(fromLinkProcessed, key, true)) {
					found = true;
				}

				if (found) {
					List<String> keyLinks = links.get(key);
					if (keyLinks == null) {
						keyLinks = new ArrayList<>();
						links.put(key, keyLinks);
					}
					keyLinks.add(link);
					matchedKeys.add(key);
				}
			}

			if (agentTitle != null && !agentTitle.isEmpty() && agentTitlePruned != null && !agentTitlePruned.isEmpty()) {
				agentTitleMatch(agentTitle, preProcessor, matchedKeys, linkTwoPart, linkProcessed, fromLink, link, links);
			}
			if (agentTitleAcronym != null && !agentTitleAcronym.isEmpty()) {
				agentTitleMatch(agentTitleAcronym, preProcessor, matchedKeys, linkTwoPart, linkProcessed, fromLink, link, links);
			}

			if (longestStart.length() > 2) {
				boolean present = false;
				for (String key : matchedKeys) {
					if (key.startsWith(longestStart)) {
						present = true;
						break;
					}
				}
				if (!present) {
					List<String> keyLinks = links.get(longestStart);
					if (keyLinks == null) {
						keyLinks = new ArrayList<>();
						links.put(longestStart, keyLinks);
					}
					keyLinks.add(link);
					matchedKeys.add(longestStart);
				}
			}

			if (longestEnd.length() > 2) {
				boolean present = false;
				for (String key : matchedKeys) {
					if (key.endsWith(longestEnd)) {
						present = true;
						break;
					}
				}
				if (!present) {
					List<String> keyLinks = links.get(longestEnd);
					if (keyLinks == null) {
						keyLinks = new ArrayList<>();
						links.put(longestEnd, keyLinks);
					}
					keyLinks.add(link);
					matchedKeys.add(longestEnd);
				}
			}
		}

		return links;
	}

	private static Suggestion1 makeSuggestion(String processed, double score, String pubIdsString, Map<String, String> processedToExtracted,
			Map<String, List<String>> linksAbstract, Map<String, List<String>> linksFulltext, List<String> fromAbstractLinks, PreProcessor preProcessor) {
		Suggestion1 suggestion = new Suggestion1();
		suggestion.setScore(score);
		String suggestionExtractedOriginal = processedToExtracted.get(processed);
		String suggestionExtracted = suggestionExtractedOriginal;
		if (!BIOTOOLS_SCHEMA_NAME_PATTERN.matcher(suggestionExtracted).matches() && !suggestionExtracted.isEmpty()) {
			for (int j = 0; j < BIOTOOLS_SCHEMA_NAME_REPLACEMENTS.length; ++j) {
				suggestionExtracted = suggestionExtracted.replace(BIOTOOLS_SCHEMA_NAME_REPLACEMENTS[j][0], BIOTOOLS_SCHEMA_NAME_REPLACEMENTS[j][1]);
			}
			suggestionExtracted = BIOTOOLS_SCHEMA_NAME_APOSTROPHE_QUOTATION_SPACE.matcher(suggestionExtracted).replaceAll("$1 $2");
			suggestionExtracted = Normalizer.normalize(suggestionExtracted, Normalizer.Form.NFKD);
			suggestionExtracted = Common.WHITESPACE.matcher(suggestionExtracted).replaceAll(" ").trim();
			suggestionExtracted = BIOTOOLS_SCHEMA_NAME_INVALID_CHAR.matcher(suggestionExtracted).replaceAll("");
			suggestionExtracted = Common.INTERNAL_TRIM.matcher(suggestionExtracted).replaceAll(" ").trim();
			logger.info("Name changed from '{}' to '{}' (from pub {})", suggestionExtractedOriginal, suggestionExtracted, pubIdsString);
		}
		if (suggestionExtracted.length() < BIOTOOLS_SCHEMA_NAME_MIN) {
			suggestionExtracted = Common.fillToMin(suggestionExtracted, BIOTOOLS_SCHEMA_NAME_MIN);
			logger.info("Name filled to min from '{}' to '{}' (from pub {})", suggestionExtractedOriginal, suggestionExtracted, pubIdsString);
		}
		if (suggestionExtracted.length() > BIOTOOLS_SCHEMA_NAME_MAX) {
			suggestionExtracted = Common.pruneToMax(suggestionExtracted, BIOTOOLS_SCHEMA_NAME_MAX);
			logger.info("Name pruned to max from '{}' to '{}' (from pub {})", suggestionExtractedOriginal, suggestionExtracted, pubIdsString);
		}
		if (!BIOTOOLS_SCHEMA_NAME_LETTER.matcher(suggestionExtracted).find()) {
			return null;
		}
		suggestion.setExtracted(suggestionExtracted);
		if (suggestionExtracted.equals(suggestionExtractedOriginal)) {
			suggestion.setProcessed(processed);
		} else {
			suggestion.setProcessed(String.join(" ", preProcessor.process(suggestionExtracted)));
			suggestion.setOriginal(suggestionExtractedOriginal);
		}
		if (linksAbstract.get(processed) != null) {
			suggestion.setLinksAbstract(linksAbstract.get(processed));
		}
		if (linksFulltext.get(processed) != null) {
			suggestion.setLinksFulltext(linksFulltext.get(processed));
		}
		suggestion.setFromAbstractLink(fromAbstractLinks.contains(processed));
		return suggestion;
	}

	private static List<String> makeFixLinks(List<String> links) {
		for (int i = 0; i < links.size(); ++i) {
			String link = links.get(i);
			Matcher fixLink = FIX_LINK.matcher(link);
			if (fixLink.find()) {
				links.add(++i, link.substring(0, fixLink.start()));
				continue;
			}
			Matcher fixLinkKeep1 = FIX_LINK_KEEP1.matcher(link);
			if (fixLinkKeep1.find()) {
				links.add(++i, link.substring(0, fixLinkKeep1.start()) + fixLinkKeep1.group(1));
				continue;
			}
			Matcher fixLinkKeep2 = FIX_LINK_KEEP2.matcher(link);
			if (fixLinkKeep2.find()) {
				links.add(++i, link.substring(0, fixLinkKeep2.start()) + fixLinkKeep2.group(1));
				continue;
			}
			Matcher fixLinkEmail1 = FIX_LINK_EMAIL1.matcher(link);
			Matcher fixLinkEmail2 = FIX_LINK_EMAIL2.matcher(link);
			Matcher fixLinkEmail3 = FIX_LINK_EMAIL3.matcher(link);
			if (fixLinkEmail1.find()) {
				links.add(++i, link.substring(0, fixLinkEmail1.start()));
			}
			if (fixLinkEmail2.find()) {
				links.add(++i, link.substring(0, fixLinkEmail2.start()));
			}
			if (fixLinkEmail3.find()) {
				links.add(++i, link.substring(0, fixLinkEmail3.start()));
			}
		}
		for (int i = 0; i < links.size(); ++i) {
			String link = links.get(i);
			Matcher schemaStart = Common.SCHEMA_START.matcher(link);
			if (schemaStart.find() && !Common.KNOWN_SCHEMA_START.matcher(link).find()) {
				links.add(++i, "http://" + link.substring(schemaStart.end()));
			}
		}
		for (int i = 0; i < links.size(); ++i) {
			String link = links.get(i);
			Matcher urlFix = URL_FIX.matcher(link);
			if (urlFix.find()) {
				String linkFixed = link.substring(urlFix.start(), urlFix.end());
				if (!link.equals(linkFixed)) {
					links.set(i, linkFixed);
					logger.info("URL fixed from {} to {}", link, linkFixed);
				}
			} else {
				logger.error("URL {} not matched", link);
			}
		}
		for (int i = 0; i < links.size(); ++i) {
			String link = links.get(i);
			int firstStartParenthesis = link.indexOf('(');
			int firstEndParenthesis = link.indexOf(')');
			if (firstEndParenthesis >= 0 && (firstStartParenthesis < 0 || firstEndParenthesis < firstStartParenthesis)) {
				String linkFixed = link.substring(0, firstEndParenthesis);
				links.set(i, linkFixed);
				logger.info("URL fixed from {} to {}", link, linkFixed);
			}
		}
		return links;
	}

	private static void makeResult(List<Result1> results, Publication publication,
			String title, String titleRest,	int agentTitleSize, String agentTitleExtractedOriginal, String agentTitle, String agentTitlePruned, String agentTitleAcronym, List<String> agentTitleOthers, List<String> agentTitleProcessedOthers,
			List<String> hostIgnore, List<String> beforeTier1, List<String> beforeTier2, List<String> beforeTier3, List<String> afterTier1, List<String> afterTier2, List<String> afterTier3,
			PreProcessor preProcessor, Idf idf, String nameProvided, List<String> webpageUrlsProvided) {
		String theAbstract = publication.getAbstract().getContent();
		if (webpageUrlsProvided != null && !webpageUrlsProvided.isEmpty()) {
			theAbstract = String.join(" ", webpageUrlsProvided) + " . " + theAbstract;
		}
		if (nameProvided != null && !nameProvided.isEmpty()) {
			theAbstract = nameProvided + " . " + theAbstract;
		}

		String titleWithoutLinks = preProcessor.removeLinks(title);
		String abstractWithoutLinks = preProcessor.removeLinks(theAbstract);

		String titleRestWithoutLinks = preProcessor.removeLinks(titleRest).trim();
		List<String> titleAbstractSentences = preProcessor.sentences(titleRestWithoutLinks + (titleRestWithoutLinks.isEmpty() ? "" : ". ") + abstractWithoutLinks);

		List<List<String>> extracted = new ArrayList<>();
		List<List<String>> processed = new ArrayList<>();

		for (String sentence : titleAbstractSentences) {
			List<String> sentenceExtracted = preProcessor.extract(sentence);
			List<String> sentenceProcessed = preProcessor.process(sentence, sentenceExtracted);
			extracted.add(sentenceExtracted);
			processed.add(sentenceProcessed);
		}

		Map<String, Double> scores = new HashMap<>();
		Map<String, String> processedToExtracted = new HashMap<>();

		Map<String, Set<String>> processedToExtractedBegin = new HashMap<>();
		Map<String, List<String>> processedToExtractedWithin = new HashMap<>();

		for (int i = 0; i < processed.size(); ++i) {
			List<String> sentenceExtracted = extracted.get(i);
			List<String> sentenceProcessed = processed.get(i);
			for (int j = 0; j < COMPOUND_WORDS; ++j) {
				for (int k = 0; k < sentenceProcessed.size() - j; ++k) {
					String wordExtracted = sentenceExtracted.get(k);
					String wordProcessed = sentenceProcessed.get(k);
					for (int l = k + 1; l <= k + j; ++l) wordExtracted += " " + sentenceExtracted.get(l);
					for (int l = k + 1; l <= k + j; ++l) wordProcessed += " " + sentenceProcessed.get(l);
					Double value;
					if (j == 0) {
						value = Math.pow(idf.getIdf(sentenceProcessed.get(k)), Common.QUERY_IDF_SCALING);
					} else {
						value = scores.get(sentenceProcessed.get(k));
						for (int l = k + 1; l <= k + j; ++l) value *= scores.get(sentenceProcessed.get(l));
						value /= COMPOUND_DIVIDER;
					}
					scores.merge(wordProcessed, value, Double::sum);
					if (i == 0 || k == 0) {
						Set<String> wordsExtracted = processedToExtractedBegin.get(wordProcessed);
						if (wordsExtracted == null) {
							wordsExtracted = new LinkedHashSet<>();
							processedToExtractedBegin.put(wordProcessed, wordsExtracted);
						}
						wordsExtracted.add(wordExtracted);
					} else {
						List<String> wordsExtracted = processedToExtractedWithin.get(wordProcessed);
						if (wordsExtracted == null) {
							wordsExtracted = new ArrayList<>();
							processedToExtractedWithin.put(wordProcessed, wordsExtracted);
						}
						wordsExtracted.add(wordExtracted);
					}
				}
			}
		}

		// put within before begin so that in case of equality option from within wins (because order-preserving sets)
		Set<String> processedToExtractedKeys = new LinkedHashSet<>();
		processedToExtractedKeys.addAll(processedToExtractedWithin.keySet());
		processedToExtractedKeys.addAll(processedToExtractedBegin.keySet());
		for (String key : processedToExtractedKeys) {
			Map<String, Integer> extractedCount = new LinkedHashMap<>();
			List<String> extractedWithins = processedToExtractedWithin.get(key);
			if (extractedWithins != null) {
				for (String extractedWithin : extractedWithins) {
					extractedCount.merge(extractedWithin, 1, Integer::sum);
				}
			}
			Set<String> extractedBegins = processedToExtractedBegin.get(key);
			if (extractedBegins != null) {
				for (String extractedBegin : extractedBegins) {
					extractedCount.merge(extractedBegin, 1, Integer::sum);
				}
			}
			extractedCount = extractedCount.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> { throw new AssertionError(); }, LinkedHashMap::new));
			processedToExtracted.put(key, extractedCount.keySet().iterator().next());
		}

		if (agentTitle != null && !agentTitle.isEmpty() && agentTitlePruned != null && !agentTitlePruned.isEmpty()) {
			boolean existing = agentTitleScore(agentTitle, preProcessor, scores, processedToExtracted, false);
			if (!existing && !agentTitlePruned.equals(agentTitle)) {
				agentTitleScore(agentTitlePruned, preProcessor, scores, processedToExtracted, true);
			}
		}
		if (agentTitleAcronym != null && !agentTitleAcronym.isEmpty()) {
			agentTitleScore(agentTitleAcronym, preProcessor, scores, processedToExtracted, false);
		}

		Map<String, Double> beforeAfterAdded = new HashMap<>();
		for (int i = 0; i < processed.size(); ++i) {
			List<String> sentenceProcessed = processed.get(i);
			boolean acronymsDone = false;
			List<Integer> acronyms = null;
			for (int j = 0; j < sentenceProcessed.size(); ++j) {
				String wordProcessed = sentenceProcessed.get(j);
				boolean inBeforeTier1 = beforeTier1.contains(wordProcessed);
				boolean inBeforeTier2 = beforeTier2.contains(wordProcessed);
				boolean inBeforeTier3 = beforeTier3.contains(wordProcessed);
				if (j + 1 < sentenceProcessed.size() && (inBeforeTier1 || inBeforeTier2 || inBeforeTier3)) {
					if (!acronymsDone) {
						acronyms = acronyms(titleAbstractSentences.get(i), preProcessor);
						acronymsDone = true;
					}
					boolean acronymFound = false;
					String acronym = null;
					if (acronyms.contains(j + 1)) {
						acronym = sentenceProcessed.get(j + 1);
						acronymFound = true;
					} else if (acronyms.contains(-(j + 1))) {
						acronym = sentenceProcessed.get(j + 1);
						acronymFound = true;
					} else if (j + 2 < sentenceProcessed.size()) {
						if (acronyms.contains(j + 2)) {
							acronym = sentenceProcessed.get(j + 2);
							acronymFound = true;
						} else if (acronyms.contains(-(j + 2))) {
							acronym = sentenceProcessed.get(j + 2);
							acronymFound = true;
						}
					}
					if (acronymFound) {
						beforeAfterScore(acronym, scores, beforeAfterAdded, inBeforeTier1, inBeforeTier2, inBeforeTier3, true);
					} else {
						String nextWord = sentenceProcessed.get(j + 1);
						beforeAfterScore(nextWord, scores, beforeAfterAdded, inBeforeTier1, inBeforeTier2, inBeforeTier3, false);
						if (j + 2 < sentenceProcessed.size()) {
							acronymFound = false;
							for (int k = 1; k <= COMPOUND_WORDS && j + 2 + k < sentenceProcessed.size(); ++k) {
								if (acronyms.contains(-(j + 2 + k))) {
									String nextNextWord = sentenceProcessed.get(j + 2 + k);
									beforeAfterScore(nextNextWord, scores, beforeAfterAdded, inBeforeTier1, inBeforeTier2, inBeforeTier3, false);
									acronymFound = true;
									break;
								}
							}
							if (!acronymFound) {
								String nextNextWord = sentenceProcessed.get(j + 2);
								beforeAfterScore(nextNextWord, scores, beforeAfterAdded, inBeforeTier1, inBeforeTier2, inBeforeTier3, false);
								String nextCompoundWord = nextWord + " " + nextNextWord;
								beforeAfterScore(nextCompoundWord, scores, beforeAfterAdded, inBeforeTier1, inBeforeTier2, inBeforeTier3, false);
							}
						}
					}
				}
				boolean inAfterTier1 = afterTier1.contains(wordProcessed);
				boolean inAfterTier2 = afterTier2.contains(wordProcessed);
				boolean inAfterTier3 = afterTier3.contains(wordProcessed);
				if (j - 1 >= 0 && (inAfterTier1 || inAfterTier2 || inAfterTier3)) {
					if (!acronymsDone) {
						acronyms = acronyms(titleAbstractSentences.get(i), preProcessor);
						acronymsDone = true;
					}
					boolean acronymFound = false;
					String acronym = null;
					if (acronyms.contains(j - 1)) {
						acronym = sentenceProcessed.get(j - 1);
						acronymFound = true;
					} else if (acronyms.contains(-(j - 1))) {
						acronym = sentenceProcessed.get(j - 1);
						acronymFound = true;
					} else if (j - 2 >= 0) {
						if (acronyms.contains(j - 2)) {
							acronym = sentenceProcessed.get(j - 2);
							acronymFound = true;
						} else if (acronyms.contains(-(j - 2))) {
							acronym = sentenceProcessed.get(j - 2);
							acronymFound = true;
						}
					}
					if (acronymFound) {
						beforeAfterScore(acronym, scores, beforeAfterAdded, inAfterTier1, inAfterTier2, inAfterTier3, true);
					} else {
						String nextWord = sentenceProcessed.get(j - 1);
						beforeAfterScore(nextWord, scores, beforeAfterAdded, inAfterTier1, inAfterTier2, inAfterTier3, false);
						if (j - 2 >= 0) {
							acronymFound = false;
							for (int k = 1; k <= COMPOUND_WORDS && j - 2 - k >= 0; ++k) {
								if (acronyms.contains(-(j - 2 - k))) {
									String nextNextWord = sentenceProcessed.get(j - 2 - k);
									beforeAfterScore(nextNextWord, scores, beforeAfterAdded, inAfterTier1, inAfterTier2, inAfterTier3, false);
									acronymFound = true;
									break;
								}
							}
							if (!acronymFound) {
								String nextNextWord = sentenceProcessed.get(j - 2);
								beforeAfterScore(nextNextWord, scores, beforeAfterAdded, inAfterTier1, inAfterTier2, inAfterTier3, false);
								String nextCompoundWord = nextNextWord + " " + nextWord;
								beforeAfterScore(nextCompoundWord, scores, beforeAfterAdded, inAfterTier1, inAfterTier2, inAfterTier3, false);
							}
						}
					}
				}
			}
		}

		List<String> titleAbstractLinks = preProcessor.links(title);
		titleAbstractLinks.addAll(preProcessor.links(theAbstract));

		List<String> fulltextLinks = preProcessor.links(publication.getFulltext().getContent());

		List<String> allLinks = new ArrayList<>();
		allLinks.addAll(titleAbstractLinks.stream().map(link -> Common.LINK_COMPARE_START.matcher(link).replaceFirst("")).collect(Collectors.toList()));
		allLinks.addAll(fulltextLinks.stream().map(link -> Common.LINK_COMPARE_START.matcher(link).replaceFirst("")).collect(Collectors.toList()));

		breakLinks(titleAbstractLinks, allLinks);
		breakLinks(fulltextLinks, allLinks);

		List<String> webpageUrlsProvidedList = new ArrayList<>();
		if (webpageUrlsProvided != null) {
			webpageUrlsProvidedList.addAll(webpageUrlsProvided);
		}
		for (int i = 0; i < titleAbstractLinks.size(); ++i) {
			String titleAbstractLink = titleAbstractLinks.get(i);

			boolean provided = false;
			for (int j = 0; j < webpageUrlsProvidedList.size(); ++j) {
				if (webpageUrlsProvidedList.get(j).equals(titleAbstractLink)) {
					webpageUrlsProvidedList.remove(j);
					provided = true;
					break;
				}
			}
			if (provided) continue;

			Iterator<String> it = fulltextLinks.iterator();
			while (it.hasNext()) {
				String fulltextLink = it.next();

				if (fulltextLink.equals(titleAbstractLink)) {
					it.remove();
					break;
				}

				String start = "";
				Matcher startTitleAbstractLink = Common.LINK_COMPARE_START.matcher(titleAbstractLink);
				if (startTitleAbstractLink.find()) {
					start = titleAbstractLink.substring(0, startTitleAbstractLink.end());
					titleAbstractLink = titleAbstractLink.substring(startTitleAbstractLink.end());
				}
				Matcher startFulltextLink = Common.LINK_COMPARE_START.matcher(fulltextLink);
				if (startFulltextLink.find()) {
					String startFulltext = fulltextLink.substring(0, startFulltextLink.end());
					if (startFulltext.length() > start.length()) {
						start = startFulltext;
					}
					fulltextLink = fulltextLink.substring(startFulltextLink.end());
				}

				if (fulltextLink.equals(titleAbstractLink)) {
					titleAbstractLinks.set(i, start + titleAbstractLink);
					it.remove();
					break;
				}

				if (titleAbstractLink.startsWith(fulltextLink) && !fulltextLink.isEmpty()) {
					String rest = titleAbstractLink.substring(fulltextLink.length() - 1);
					if (Common.LINK_COMPARE_REST.matcher(rest).matches()) {
						titleAbstractLinks.set(i, start + fulltextLink);
						it.remove();
						break;
					}
				}
			}
		}

		Map<String, List<String>> linksAbstract = links(titleAbstractLinks, preProcessor, idf, hostIgnore,
			scores.keySet(), processedToExtracted, processed, titleWithoutLinks, abstractWithoutLinks,
			agentTitle, agentTitlePruned, agentTitleAcronym);

		Map<String, List<String>> linksFulltext = links(fulltextLinks, preProcessor, idf, hostIgnore,
			scores.keySet(), processedToExtracted, processed, titleWithoutLinks, abstractWithoutLinks,
			agentTitle, agentTitlePruned, agentTitleAcronym);

		for (Map.Entry<String, List<String>> linkEntry : linksAbstract.entrySet()) {
			double score = scores.get(linkEntry.getKey()) * LINK_MULTIPLIER_ABSTRACT * linkEntry.getValue().size();
			if (score > LINK_MULTIPLIER_ABSTRACT_MINIMUM) {
				scores.put(linkEntry.getKey(), score);
			} else {
				scores.put(linkEntry.getKey(), LINK_MULTIPLIER_ABSTRACT_MINIMUM);
			}
		}

		List<String> fromAbstractLinks = new ArrayList<>();
		boolean genericLinkAugmentation = linksAbstract.isEmpty();
		for (String link : titleAbstractLinks) {
			if (LINK_TWO_PART.matcher(link).matches()) continue;
			boolean present = false;
			for (Map.Entry<String, List<String>> linkEntry : linksAbstract.entrySet()) {
				if (linkEntry.getValue().contains(link) && !fromAbstractLinks.contains(linkEntry.getKey())) {
					present = true;
					break;
				}
			}
			if (!present) {
				if (genericLinkAugmentation) {
					for (Map.Entry<String, Double> scoreEntry : scores.entrySet()) {
						scores.put(scoreEntry.getKey(), scoreEntry.getValue() * LINK_MULTIPLIER_ABSTRACT_AUGMENTATION);
					}
					genericLinkAugmentation = false;
				}

				String fromLink = fromLink(link, preProcessor, idf, hostIgnore);
				if (!fromLink.isEmpty()) {
					List<String> fromLinkExtracted = preProcessor.extract(fromLink);
					List<String> fromLinkProcessed = preProcessor.process(fromLink, fromLinkExtracted);
					String fromLinkExtractedString = String.join(" ", fromLinkExtracted);
					String fromLinkProcessedString = String.join(" ", fromLinkProcessed);
					if (!fromLinkProcessedString.isEmpty()) {
						if (!scores.containsKey(fromLinkProcessedString)) {
							fromAbstractLinks.add(fromLinkProcessedString);
							processedToExtracted.put(fromLinkProcessedString, fromLinkExtractedString);
						}
						scores.merge(fromLinkProcessedString, LINK_MULTIPLIER_ABSTRACT_NEW / fromLinkProcessed.size(), (d1, d2) -> d1 * d2);
						List<String> linkAbstract = linksAbstract.get(fromLinkProcessedString);
						if (linkAbstract == null) {
							linkAbstract = new ArrayList<>();
							linksAbstract.put(fromLinkProcessedString, linkAbstract);
						}
						linkAbstract.add(link);
					}
				}
			}
		}
		if (!fromAbstractLinks.isEmpty()) {
			for (String link : fulltextLinks) {
				String fromLink = fromLink(link, preProcessor, idf, hostIgnore);
				if (!fromLink.isEmpty()) {
					List<String> fromLinkProcessed = preProcessor.process(fromLink);
					String fromLinkProcessedString = String.join(" ", fromLinkProcessed);
					if (!fromLinkProcessedString.isEmpty()) {
						for (String fromAbstractLink : fromAbstractLinks) {
							if (fromAbstractLink.equals(fromLinkProcessedString)) {
								List<String> linkFulltext = linksFulltext.get(fromLinkProcessedString);
								if (linkFulltext == null) {
									linkFulltext = new ArrayList<>();
									linksFulltext.put(fromLinkProcessedString, linkFulltext);
								}
								linkFulltext.add(link);
							}
						}
					}
				}
			}
		}

		for (Map.Entry<String, List<String>> linkEntry : linksFulltext.entrySet()) {
			long multiplier = linkEntry.getValue().stream().filter(link -> !LINK_TWO_PART.matcher(link).matches()).count();
			if (multiplier > 0) {
				if (multiplier > 2) {
					multiplier = 2;
				}
				scores.put(linkEntry.getKey(), scores.get(linkEntry.getKey()) * LINK_MULTIPLIER_FULLTEXT * multiplier);
			}
		}

		Map<String, Double> sortedScores = scores.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> { throw new AssertionError(); }, LinkedHashMap::new));

		String pmid = publication.getPmid().getContent();
		String pmcid = publication.getPmcid().getContent();
		String doi = publication.getDoi().getContent();
		if (!pmid.isEmpty() && !BIOTOOLS_SCHEMA_PMID_PATTERN.matcher(pmid).matches()) {
			logger.warn("Discarded invalid publication PMID '{}' (PMCID is '{}', DOI is '{}')", pmid, pmcid, doi);
			pmid = "";
		}
		if (!pmcid.isEmpty() && !BIOTOOLS_SCHEMA_PMCID_PATTERN.matcher(pmcid).matches()) {
			logger.warn("Discarded invalid publication PMCID '{}' (PMID is '{}', DOI is '{}')", pmcid, pmid, doi);
			pmcid = "";
		}
		if (!doi.isEmpty() && !BIOTOOLS_SCHEMA_DOI_PATTERN.matcher(doi).matches()) {
			logger.warn("Discarded invalid publication DOI '{}' (PMID is '{}', PMCID is '{}')", doi, pmid, pmcid);
			doi = "";
		}

		Result1 result = new Result1();

		PublicationIds publicationIds = new PublicationIds(pmid, pmcid, doi, publication.getPmid().getUrl(), publication.getPmcid().getUrl(), publication.getDoi().getUrl());
		PubIds pubIds = new PubIds(publicationIds.getPmid(), publicationIds.getPmcid(), publicationIds.getDoi());
		result.setPubIds(pubIds);

		result.setTitle(title);
		result.setAgentTitleOthers(agentTitleOthers != null ? agentTitleOthers : new ArrayList<>());
		result.setAgentTitleExtractedOriginal(agentTitleExtractedOriginal != null ? agentTitleExtractedOriginal : "");
		result.setAgentTitle(agentTitle != null ? agentTitle : "");
		result.setAgentTitlePruned(agentTitlePruned != null ? agentTitlePruned : "");
		result.setAgentTitleAcronym(agentTitleAcronym != null ? agentTitleAcronym : "");
		result.setAbstractSentences(preProcessor.sentences(publication.getAbstract().getContent()));
		result.setOa(publication.isOA());
		result.setPreprint(publication.isPreprint());
		result.setJournalTitle(publication.getJournalTitle());
		result.setPubDate(publication.getPubDate());
		result.setPubDateHuman(publication.getPubDateHuman());
		result.setCitationsCount(publication.getCitationsCount());
		result.setCitationsTimestamp(publication.getCitationsTimestamp());
		result.setCitationsTimestampHuman(publication.getCitationsTimestampHuman());

		List<CorrespAuthor> correspAuthor = publication.getCorrespAuthor();
		for (CorrespAuthor ca : correspAuthor) {
			String name = Common.WHITESPACE.matcher(ca.getName()).replaceAll(" ").trim();
			if (name.length() < BIOTOOLS_SCHEMA_CREDIT_NAME_MIN && !name.isEmpty()) {
				name = Common.fillToMin(name, BIOTOOLS_SCHEMA_CREDIT_NAME_MIN);
				logger.warn("Credit name filled to min from '{}' to '{}' (from pub {})", ca.getName(), name, result.getPubIds().toString());
			}
			if (name.length() > BIOTOOLS_SCHEMA_CREDIT_NAME_MAX) {
				name = Common.pruneToMax(name, BIOTOOLS_SCHEMA_CREDIT_NAME_MAX);
				logger.warn("Credit name pruned to max from '{}' to '{}' (from pub {})", ca.getName(), name, result.getPubIds().toString());
			}
			ca.setName(name);
			// TODO possibly don't require https for orcidid
			if (ca.getOrcid().startsWith("http://")) {
				String httpsOrcid = "https://" + ca.getOrcid().substring("http://".length());
				logger.debug("Changed credit orcidid to HTTPS (from {} to {})", ca.getOrcid(), httpsOrcid);
				ca.setOrcid(httpsOrcid);
			}
			if (!ca.getOrcid().isEmpty() && !BIOTOOLS_SCHEMA_CREDIT_ORCIDID_PATTERN.matcher(ca.getOrcid()).matches()) {
				logger.warn("Discarded invalid credit orcidid '{}' (from pub {})", ca.getOrcid(), result.getPubIds().toString());
				ca.setOrcid("");
			}
			if (!ca.getEmail().isEmpty() && !BIOTOOLS_SCHEMA_CREDIT_EMAIL_PATTERN.matcher(ca.getEmail()).matches()) {
				String email = ca.getEmail();
				if (email.charAt(email.length() - 1) == '.') {
					email = email.substring(0, email.length() - 1);
				}
				boolean valid = false;
				for (String emailPart : email.split(" ")) {
					if (BIOTOOLS_SCHEMA_CREDIT_EMAIL_PATTERN.matcher(emailPart).matches()) {
						email = emailPart;
						valid = true;
						break;
					}
				}
				if (valid) {
					logger.warn("Credit email changed from '{}' to '{}' (from pub {})", ca.getEmail(), email, result.getPubIds().toString());
					ca.setEmail(email);
				} else {
					logger.warn("Discarded invalid credit email '{}' (from pub {})", ca.getEmail(), result.getPubIds().toString());
					ca.setEmail("");
				}
			}
			if (!ca.getUri().isEmpty() && !Common.BIOTOOLS_SCHEMA_URL_PATTERN.matcher(ca.getUri()).matches()) {
				logger.warn("Discarded invalid credit url '{}' (from pub {})", ca.getUri(), result.getPubIds().toString());
				ca.setUri("");
			}
		}
		for (Iterator<CorrespAuthor> it = correspAuthor.iterator(); it.hasNext(); ) {
			CorrespAuthor ca = it.next();
			if (ca.getName().isEmpty() && ca.getEmail().isEmpty() && ca.getUri().isEmpty()) {
				logger.warn("Discarded empty credit (from pub {})", result.getPubIds().toString());
				it.remove();
			}
		}
		result.setCorrespAuthor(correspAuthor);

		if (nameProvided != null && !nameProvided.isEmpty()) {
			String nameProvidedExtracted = String.join(" ", preProcessor.extract(nameProvided));
			String nameProvidedProcessed = String.join(" ", preProcessor.process(nameProvided));
			processedToExtracted.put(nameProvidedProcessed, nameProvidedExtracted);
		}

		Iterator<Map.Entry<String, Double>> sortedScoresIterator = sortedScores.entrySet().iterator();
		double topScore = 0;
		for (int i = 0; i < SUGGESTION_LIMIT && sortedScoresIterator.hasNext(); ++i) {
			Map.Entry<String, Double> entry = sortedScoresIterator.next();
			if (agentTitleProcessedOthers != null && agentTitleProcessedOthers.contains(entry.getKey())) {
				--i;
				continue;
			}
			if (i == 0) {
				topScore = entry.getValue();
			} else {
				if (entry.getValue() * TOP_SCORE_LIMIT < topScore) break;
			}
			Suggestion1 suggestion = makeSuggestion(entry.getKey(), entry.getValue(), result.getPubIds().toString(), processedToExtracted, linksAbstract, linksFulltext, fromAbstractLinks, preProcessor);
			if (suggestion == null) {
				--i;
				continue;
			}
			result.addSuggestion(suggestion);
		}

		if (nameProvided != null && !nameProvided.isEmpty()) {
			String nameProvidedProcessed = String.join(" ", preProcessor.process(nameProvided));
			boolean added = false;
			for (Suggestion1 suggestion : result.getSuggestions()) {
				if (suggestion.getProcessed().equals(nameProvidedProcessed)) {
					added = true;
					break;
				}
			}
			if (!added) {
				for (Map.Entry<String, Double> entry : sortedScores.entrySet()) {
					if (entry.getKey().equals(nameProvidedProcessed)) {
						Suggestion1 suggestion = makeSuggestion(entry.getKey(), entry.getValue(), result.getPubIds().toString(), processedToExtracted, linksAbstract, linksFulltext, fromAbstractLinks, preProcessor);
						if (suggestion != null) {
							result.addSuggestion(suggestion);
						}
						added = true;
						break;
					}
				}
			}
			if (!added) {
				Suggestion1 suggestion = makeSuggestion(nameProvidedProcessed, 0, result.getPubIds().toString(), processedToExtracted, linksAbstract, linksFulltext, fromAbstractLinks, preProcessor);
				if (suggestion != null) {
					result.addSuggestion(suggestion);
				}
			}
		}

		result.setLeftoverLinksAbstract(titleAbstractLinks);
		result.setLeftoverLinksFulltext(fulltextLinks);

		List<String> leftoverLinksAbstractCompare = new ArrayList<>();
		TreeSet<Integer> leftoverLinksAbstractRemove = new TreeSet<>();
		for (String leftoverLinkAbstract : result.getLeftoverLinksAbstract()) {
			leftoverLinksAbstractCompare.add(String.join("", preProcessor.process(leftoverLinkAbstract)));
		}
		List<String> titleAbstractLinksCompare = new ArrayList<>();
		for (String titleAbstractLink : titleAbstractLinks) {
			titleAbstractLinksCompare.add(String.join("", preProcessor.process(titleAbstractLink)));
		}
		for (Suggestion1 suggestion : result.getSuggestions()) {
			String suggestionCompare = Common.BIOTOOLS_PROCESSED_VERSION_TRIM.matcher(suggestion.getProcessed()).replaceFirst("").replaceAll(" ", "");
			if (suggestionCompare.length() < 2) continue;
			for (int i = 0; i < result.getLeftoverLinksAbstract().size(); ++i) {
				String leftoverLinkAbstractCompare = leftoverLinksAbstractCompare.get(i);
				if (leftoverLinkAbstractCompare.contains(suggestionCompare)) {
					suggestion.addLinkAbstract(result.getLeftoverLinksAbstract().get(i));
					leftoverLinksAbstractRemove.add(i);
				}
			}
			for (int i = 0; i < titleAbstractLinks.size(); ++i) {
				String link = titleAbstractLinks.get(i);
				if (!suggestion.getLinksAbstract().contains(link) && !result.getLeftoverLinksAbstract().contains(link)) {
					String linkCompare = titleAbstractLinksCompare.get(i);
					if (linkCompare.contains(suggestionCompare)) {
						suggestion.addLinkAbstract(link);
					}
				}
			}
		}
		for (Iterator<Integer> it = leftoverLinksAbstractRemove.descendingIterator(); it.hasNext(); ) {
			result.getLeftoverLinksAbstract().remove(it.next().intValue());
		}

		leftoverLinksAbstractRemove = new TreeSet<>();
		for (Suggestion1 suggestion : result.getSuggestions()) {
			String abstractWithoutSuggestionLinks = theAbstract;
			for (String suggestionLink : suggestion.getLinksAbstract()) {
				String suggestionLinkTrimmed = Common.LINK_COMPARE_END.matcher(Common.LINK_COMPARE_START.matcher(suggestionLink).replaceFirst(".")).replaceFirst("");
				String pattern = "(?i)";
				for (int i = 0; i < suggestionLinkTrimmed.length(); ++i) {
					String letter = suggestionLinkTrimmed.substring(i, i + 1);
					pattern += (Common.USE_IN_PATTERN.matcher(letter).matches() ? letter : ".");
				}
				abstractWithoutSuggestionLinks = Pattern.compile(pattern).matcher(abstractWithoutSuggestionLinks).replaceAll("");
			}
			for (int i = 0; i < result.getLeftoverLinksAbstract().size(); ++i) {
				String leftoverLink = result.getLeftoverLinksAbstract().get(i);
				if (LEFTOVER_EXCLUDE.matcher(leftoverLink).find()) {
					continue;
				}
				try {
					String host = new URL(Common.prependHttp(leftoverLink)).getHost();
					String hostPattern = "";
					for (int j = 0; j < host.length(); ++j) {
						String letter = host.substring(j, j + 1);
						hostPattern += (Common.USE_IN_PATTERN.matcher(letter).matches() ? letter : ".");
					}
					if (Pattern.compile("(?i)" + LEFTOVER_AVAILABLE + "[^.?]*[^ ]*" + hostPattern).matcher(abstractWithoutSuggestionLinks).find() ||
							Pattern.compile("(?i)" + hostPattern + "[^ ]*[^.?]*" + LEFTOVER_AVAILABLE).matcher(abstractWithoutSuggestionLinks).find()) {
						suggestion.addLinkAbstract(leftoverLink);
						leftoverLinksAbstractRemove.add(i);
					}
				} catch (MalformedURLException e) {
				}
			}
		}
		for (Iterator<Integer> it = leftoverLinksAbstractRemove.descendingIterator(); it.hasNext(); ) {
			result.getLeftoverLinksAbstract().remove(it.next().intValue());
		}

		if (nameProvided != null && !nameProvided.isEmpty() && webpageUrlsProvided != null && !webpageUrlsProvided.isEmpty()) {
			String nameProvidedProcessed = String.join(" ", preProcessor.process(nameProvided));
			for (Suggestion1 suggestion : result.getSuggestions()) {
				if (suggestion.getProcessed().equals(nameProvidedProcessed)) {
					List<String> webpageUrlsProvidedNotAdded = new ArrayList<>();
					for (String webpageUrlProvided : webpageUrlsProvided) {
						boolean added = false;
						for (String abstractLink : suggestion.getLinksAbstract()) {
							if (Common.trimUrl(abstractLink).equals(Common.trimUrl(webpageUrlProvided))) {
								added = true;
								break;
							}
						}
						if (!added) {
							webpageUrlsProvidedNotAdded.add(webpageUrlProvided);
						}
					}
					suggestion.addLinksAbstract(webpageUrlsProvidedNotAdded);
					break;
				}
			}
		}

		for (Suggestion1 suggestion : result.getSuggestions()) {
			makeFixLinks(suggestion.getLinksAbstract());
			makeFixLinks(suggestion.getLinksFulltext());
		}

		results.add(result);
	}

	private static void writeLinks(Writer writer, List<String> links) throws IOException {
		for (String link : links) {
			if (!Common.SCHEMA_START.matcher(link).find()) {
				writer.write("http://");
			}
			writer.write(link);
			writer.write("\n");
		}
	}

	public static void run(Path outputPath, PreProcessor preProcessor, String logPrefix, Idf idfProvided, List<Publication> publicationsProvided, String nameProvided, List<String> webpageUrlsProvided) throws IOException {
		Marker mainMarker = MarkerManager.getMarker(Common.MAIN_MARKER);

		List<String> hostIgnore = PubFetcher.getResource(Pass1.class, "pass1/host_ignore.txt");
		List<String> beforeTier1 = PubFetcher.getResource(Pass1.class, "pass1/before_tier1.txt");
		List<String> beforeTier2 = PubFetcher.getResource(Pass1.class, "pass1/before_tier2.txt");
		List<String> beforeTier3 = PubFetcher.getResource(Pass1.class, "pass1/before_tier3.txt");
		List<String> afterTier1 = PubFetcher.getResource(Pass1.class, "pass1/after_tier1.txt");
		List<String> afterTier2 = PubFetcher.getResource(Pass1.class, "pass1/after_tier2.txt");
		List<String> afterTier3 = PubFetcher.getResource(Pass1.class, "pass1/after_tier3.txt");

		Idf idf;
		if (idfProvided != null) {
			idf = idfProvided;
		} else {
			String idfFile = outputPath.resolve(Common.IDF_FILE).toString();
			logger.info(mainMarker, "{}Loading IDF from {}", logPrefix, idfFile);
			idf = new Idf(idfFile);
		}

		Set<Publication> publications;
		if (publicationsProvided != null) {
			publications = new LinkedHashSet<>(publicationsProvided);
		} else {
			String pubFile = outputPath.resolve(Common.PUB_FILE).toString();
			String dbFile = outputPath.resolve(Common.DB_FILE).toString();
			logger.info(mainMarker, "{}Loading publications from {} for IDs found in {}", logPrefix, dbFile, pubFile);
			publications = new LinkedHashSet<>(PubFetcher.getPublications(dbFile, Collections.singletonList(pubFile), Common.PUB_FILE));
		}

		Path webPath = PubFetcher.outputPath(outputPath.resolve(Common.WEB_FILE).toString());
		Path docPath = PubFetcher.outputPath(outputPath.resolve(Common.DOC_FILE).toString());
		Path pass1Path = PubFetcher.outputPath(outputPath.resolve(Common.PASS1_FILE).toString());

		List<Result1> results = new ArrayList<>();

		logger.info(mainMarker, "{}Making results from {} publications", logPrefix, publications.size());

		CharsetEncoder webEncoder = StandardCharsets.UTF_8.newEncoder();
		webEncoder.onMalformedInput(CodingErrorAction.REPLACE);
		webEncoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

		CharsetEncoder docEncoder = StandardCharsets.UTF_8.newEncoder();
		docEncoder.onMalformedInput(CodingErrorAction.REPLACE);
		docEncoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

		CharsetEncoder pass1Encoder = StandardCharsets.UTF_8.newEncoder();
		pass1Encoder.onMalformedInput(CodingErrorAction.REPLACE);
		pass1Encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

		try (BufferedWriter webWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(webPath), webEncoder));
				BufferedWriter docWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(docPath), docEncoder));
				BufferedWriter pass1Writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(pass1Path), pass1Encoder))) {

			int publicationIndex = 0;
			long start = System.currentTimeMillis();
			for (Publication publication : publications) {
				++publicationIndex;
				System.err.print(PubFetcher.progress(publicationIndex, publications.size(), start) + "  \r");

				if (publication.getAbstract().getSize() > ABSTRACT_MAX_LENGTH && publicationsProvided == null) {
					logger.info("Skipping publication {}, as length of abstract ({}) is larger than allowed ({})", publication.toStringId(), publication.getAbstract().getSize(), ABSTRACT_MAX_LENGTH);
					continue;
				}
				if (publication.getFulltext().getSize() > FULLTEXT_MAX_LENGTH && publicationsProvided == null) {
					logger.info("Skipping publication {}, as length of fulltext ({}) is larger than allowed ({})", publication.toStringId(), publication.getFulltext().getSize(), FULLTEXT_MAX_LENGTH);
					continue;
				}

				List<String> agentTitleExtractedOriginal = new ArrayList<>();
				List<String> agentTitle = new ArrayList<>();
				List<String> agentTitlePruned = new ArrayList<>();
				String agentTitleAcronym = null;
				long agentTitleWordsTotal = 0;

				String title = publication.getTitle().getContent();
				String titleRest = title;

				int from = 0;
				Matcher matcher = Common.TITLE_SEPARATOR.matcher(title);

				while (from < title.length() && matcher.find(from)) {
					String currentAgentTitleString = title.substring(from, matcher.start()).trim();
					if (TOOL_TITLE_INVALID.matcher(currentAgentTitleString).matches()) {
						from = matcher.end();
						continue;
					}

					List<String> currentAgentTitleExtractedOriginal = new ArrayList<>();
					List<String> currentAgentTitle = new ArrayList<>();
					List<String> currentAgentTitlePruned = new ArrayList<>();
					String currentAgentTitleAcronym = null;
					long currentAgentTitleWordsTotal = 0;

					Integer firstAcronymIndex = firstAcronymIndex(currentAgentTitleString, preProcessor);
					if (!TOOL_TITLE_SEPARATOR.matcher(currentAgentTitleString).find() || firstAcronymIndex != null || currentAgentTitleString.split(" ").length > TOOL_TITLE_SEPARATOR_MAX_WORDS) {
						List<String> currentAgentTitleExtracted = preProcessor.extract(currentAgentTitleString);
						currentAgentTitleExtractedOriginal.add(String.join(" ", currentAgentTitleExtracted));
						preProcessor.process(currentAgentTitleString, currentAgentTitleExtracted); // align indexes
						if (firstAcronymIndex != null) {
							currentAgentTitleAcronym = currentAgentTitleExtracted.remove(firstAcronymIndex.intValue());
						}
						currentAgentTitle.add(String.join(" ", currentAgentTitleExtracted));
						currentAgentTitlePruned.add(Common.agentTitlePrune(currentAgentTitleExtracted));
						currentAgentTitleWordsTotal += currentAgentTitleExtracted.size();
					} else {
						for (String currentAgentTitleStringPart : TOOL_TITLE_SEPARATOR_ALL.split(currentAgentTitleString)) {
							List<String> currentAgentTitleExtracted = preProcessor.extract(currentAgentTitleStringPart);
							currentAgentTitleExtractedOriginal.add(String.join(" ", currentAgentTitleExtracted));
							preProcessor.process(currentAgentTitleStringPart, currentAgentTitleExtracted); // align indexes
							currentAgentTitle.add(String.join(" ", currentAgentTitleExtracted));
							currentAgentTitlePruned.add(Common.agentTitlePrune(currentAgentTitleExtracted));
							currentAgentTitleWordsTotal += currentAgentTitleExtracted.size();
						}
					}

					if (currentAgentTitleWordsTotal < agentTitleWordsTotal || agentTitle.isEmpty()) {
						agentTitleExtractedOriginal = currentAgentTitleExtractedOriginal;
						agentTitle = currentAgentTitle;
						agentTitlePruned = currentAgentTitlePruned;
						agentTitleAcronym = currentAgentTitleAcronym;
						agentTitleWordsTotal = currentAgentTitleWordsTotal;
						titleRest = title.substring(0, from).trim() + " " + title.substring(matcher.start()).trim();
					}

					from = matcher.end();
				}

				if (from == 0) {
					List<String> agentTitleExtractedStandalone = preProcessor.extract(title);
					String agentTitleExtractedOriginalStandalone = String.join(" ", agentTitleExtractedStandalone);
					preProcessor.process(title, agentTitleExtractedStandalone); // align indexes
					String agentTitlePrunedStandalone = Common.agentTitlePrune(agentTitleExtractedStandalone);
					if (agentTitlePrunedStandalone.length() <= TOOL_TITLE_STANDALONE_MAX_CHARS) {
						agentTitleExtractedOriginal.add(agentTitleExtractedOriginalStandalone);
						agentTitle.add(String.join(" ", agentTitleExtractedStandalone));
						agentTitlePruned.add(agentTitlePrunedStandalone);
						titleRest = "";
					}
				}

				if (!agentTitle.isEmpty()) {
					List<String> agentTitleProcessed = new ArrayList<>();
					for (int i = 0; i < agentTitle.size(); ++i) {
						agentTitleProcessed.add(String.join(" ", preProcessor.process(agentTitle.get(i))));
					}
					List<String> agentTitleProcessedDone = new ArrayList<>();
					for (int i = 0; i < agentTitleProcessed.size(); ++i) {
						if (!agentTitleProcessedDone.contains(agentTitleProcessed.get(i))) {
							List<String> agentTitleOthers = new ArrayList<>();
							for (int j = 0; j < agentTitle.size(); ++j) {
								if (!agentTitle.get(i).equals(agentTitle.get(j))) {
									agentTitleOthers.add(agentTitle.get(j));
								}
							}
							List<String> agentTitleProcessedOthers = new ArrayList<>();
							for (int j = 0; j < agentTitleProcessed.size(); ++j) {
								if (!agentTitleProcessed.get(i).equals(agentTitleProcessed.get(j))) {
									agentTitleProcessedOthers.add(agentTitleProcessed.get(j));
								}
							}
							makeResult(results, publication,
								title, titleRest, agentTitle.size(), agentTitleExtractedOriginal.get(i), agentTitle.get(i), agentTitlePruned.get(i), agentTitleAcronym, agentTitleOthers, agentTitleProcessedOthers,
								hostIgnore, beforeTier1, beforeTier2, beforeTier3, afterTier1, afterTier2, afterTier3,
								preProcessor, idf, nameProvided, webpageUrlsProvided);
							agentTitleProcessedDone.add(agentTitleProcessed.get(i));
						}
					}
				} else {
					makeResult(results, publication,
						title, titleRest, 0, null, null, null, null, null, null,
						hostIgnore, beforeTier1, beforeTier2, beforeTier3, afterTier1, afterTier2, afterTier3,
						preProcessor, idf, nameProvided, webpageUrlsProvided);
				}
			}

			logger.info(mainMarker, "{}Made {} results from {} publications", logPrefix, results.size(), publications.size());

			logger.info(mainMarker, "{}Sorting results", logPrefix);
			Collections.sort(results);

			List<String> webpages = new ArrayList<>();
			List<String> docs = new ArrayList<>();

			logger.info(mainMarker, "{}Dividing links to webpages and docs", logPrefix);
			for (Result1 result : results) {
				for (Suggestion1 suggestion : result.getSuggestions()) {
					List<BioagentsLink<LinkType>> linkLinks = new ArrayList<>();
					List<BioagentsLink<DownloadType>> downloadLinks = new ArrayList<>();
					List<BioagentsLink<DocumentationType>> documentationLinks = new ArrayList<>();
					Common.makeBioagentsLinks(suggestion.getLinksAbstract(), linkLinks, downloadLinks, documentationLinks);
					Common.makeBioagentsLinks(suggestion.getLinksFulltext(), linkLinks, downloadLinks, documentationLinks);
					linkLinks.forEach(link -> webpages.add(link.getUrl()));
					downloadLinks.forEach(link -> webpages.add(link.getUrl()));
					documentationLinks.forEach(link -> docs.add(link.getUrl()));
				}
			}
			if (webpageUrlsProvided != null && !webpageUrlsProvided.isEmpty()) {
				List<String> webpageUrlsProvidedNotAdded = new ArrayList<>();
				for (String webpageUrlProvided : webpageUrlsProvided) {
					boolean added = false;
					for (String webpage : webpages) {
						if (Common.trimUrl(webpage).equals(Common.trimUrl(webpageUrlProvided))) {
							added = true;
							break;
						}
					}
					for (String doc : docs) {
						if (Common.trimUrl(doc).equals(Common.trimUrl(webpageUrlProvided))) {
							added = true;
							break;
						}
					}
					if (!added) {
						if (!Common.SCHEMA_START.matcher(webpageUrlProvided).find()) {
							webpageUrlProvided = "http://" + webpageUrlProvided;
						}
						webpageUrlsProvidedNotAdded.add(webpageUrlProvided);
					}
				}
				if (!webpageUrlsProvidedNotAdded.isEmpty()) {
					List<BioagentsLink<LinkType>> linkLinks = new ArrayList<>();
					List<BioagentsLink<DownloadType>> downloadLinks = new ArrayList<>();
					List<BioagentsLink<DocumentationType>> documentationLinks = new ArrayList<>();
					Common.makeBioagentsLinks(webpageUrlsProvidedNotAdded, linkLinks, downloadLinks, documentationLinks);
					linkLinks.forEach(link -> webpages.add(link.getUrl()));
					downloadLinks.forEach(link -> webpages.add(link.getUrl()));
					documentationLinks.forEach(link -> docs.add(link.getUrl()));
				}
			}

			logger.info(mainMarker, "{}Writing {} webpage URLs to {}", logPrefix, webpages.size(), webPath.toString());
			writeLinks(webWriter, webpages);
			logger.info(mainMarker, "{}Writing {} doc URLs to {}", logPrefix, docs.size(), docPath.toString());
			writeLinks(docWriter, docs);

			logger.info(mainMarker, "{}Writing {} pass1 results to {}", logPrefix, results.size(), pass1Path.toString());
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.enable(SerializationFeature.CLOSE_CLOSEABLE);
			mapper.writeValue(pass1Writer, results);
		}
	}
}
