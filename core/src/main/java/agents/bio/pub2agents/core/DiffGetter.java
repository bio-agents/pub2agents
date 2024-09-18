/*
 * Copyright © 2019 Erik Jaaniso
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.edamontology.pubfetcher.core.common.PubFetcher;
import org.edamontology.pubfetcher.core.db.Database;
import org.edamontology.pubfetcher.core.db.publication.CorrespAuthor;
import org.edamontology.pubfetcher.core.db.webpage.Webpage;

import org.edamontology.edammap.core.input.json.Credit;
import org.edamontology.edammap.core.input.json.DocumentationType;
import org.edamontology.edammap.core.input.json.DownloadType;
import org.edamontology.edammap.core.input.json.Link;
import org.edamontology.edammap.core.input.json.LinkDownload;
import org.edamontology.edammap.core.input.json.LinkType;
import org.edamontology.edammap.core.input.json.Agent;

public final class DiffGetter {

	private static boolean linksEqual(String addLink, String addLinkTrimmed, String bioagentsLink, Database db, boolean addLinkDoc, boolean bioagentsLinkDoc) {
		String bioagentsLinkTrimmed = Common.trimUrl(bioagentsLink);
		if (addLinkTrimmed.equals(bioagentsLinkTrimmed)) {
			return true;
		}
		String addLinkFinalTrimmed = null;
		Webpage addLinkWebpage = null;
		if (addLinkDoc) {
			addLinkWebpage = db.getDoc(addLink, false);
		} else {
			addLinkWebpage = db.getWebpage(addLink, false);
		}
		if (addLinkWebpage != null) {
			addLinkFinalTrimmed = Common.trimUrl(addLinkWebpage.getFinalUrl());
		}
		String bioagentsLinkFinalTrimmed = null;
		Webpage bioagentsLinkWebpage = null;
		if (bioagentsLinkDoc) {
			bioagentsLinkWebpage = db.getDoc(bioagentsLink, false);
		} else {
			bioagentsLinkWebpage = db.getWebpage(bioagentsLink, false);
		}
		if (bioagentsLinkWebpage != null) {
			bioagentsLinkFinalTrimmed = Common.trimUrl(bioagentsLinkWebpage.getFinalUrl());
		}
		if (addLinkFinalTrimmed != null && !addLinkFinalTrimmed.isEmpty() && bioagentsLinkFinalTrimmed != null && !bioagentsLinkFinalTrimmed.isEmpty()) {
			if (addLinkFinalTrimmed.equals(bioagentsLinkFinalTrimmed)) {
				return true;
			}
		} else {
			if (addLinkFinalTrimmed != null && !addLinkFinalTrimmed.isEmpty()) {
				if (addLinkFinalTrimmed.equals(bioagentsLinkTrimmed)) {
					return true;
				}
			}
			if (bioagentsLinkFinalTrimmed != null && !bioagentsLinkFinalTrimmed.isEmpty()) {
				if (addLinkTrimmed.equals(bioagentsLinkFinalTrimmed)) {
					return true;
				}
			}
		}
		return false;
	}

	private static void addHomepageToLinks(List<BioagentsLink<LinkType>> linkLinks, List<BioagentsLink<DownloadType>> downloadLinks, List<BioagentsLink<DocumentationType>> documentationLinks, Set<BioagentsLink<LinkType>> links, Set<BioagentsLink<DownloadType>> downloads, Set<BioagentsLink<DocumentationType>> documentations, Database db, boolean bioagentsHomepage) {
		boolean found = false;
		if (!linkLinks.isEmpty()) {
			for (BioagentsLink<LinkType> link : links) {
				if (linksEqual(link.getUrl(), link.getUrlTrimmed(), linkLinks.get(0).getUrl(), db, false, false)) {
					found = true;
					break;
				}
			}
			if (!found) {
				links.add(linkLinks.get(0));
			}
		} else if (!downloadLinks.isEmpty()) {
			for (BioagentsLink<DownloadType> download : downloads) {
				if (linksEqual(download.getUrl(), download.getUrlTrimmed(), downloadLinks.get(0).getUrl(), db, false, false)) {
					found = true;
					break;
				}
			}
			if (!found) {
				downloads.add(downloadLinks.get(0));
			}
		} else if (!documentationLinks.isEmpty()) {
			for (BioagentsLink<DocumentationType> documentation : documentations) {
				if (linksEqual(documentation.getUrl(), documentation.getUrlTrimmed(), documentationLinks.get(0).getUrl(), db, true, !bioagentsHomepage)) {
					found = true;
					break;
				}
			}
			if (!found) {
				documentations.add(documentationLinks.get(0));
			}
		}
	}

	static Diff makeDiff(double scoreScore2, Set<Integer> possiblyRelated, List<Agent> bioagents, int existing, List<PubIds> publications, Collection<PubIds> addPublications, String modifyName, String homepage, Set<BioagentsLink<LinkType>> links, Set<BioagentsLink<DownloadType>> downloads, Set<BioagentsLink<DocumentationType>> documentations, Provenance license, List<Provenance> languages, List<CorrespAuthor> credits, Database db) {
		Diff diff = new Diff();

		diff.setScoreScore2(scoreScore2);
		diff.setPossiblyRelated(possiblyRelated);

		Agent bioagent = bioagents.get(existing);

		diff.setExisting(existing);
		diff.setAddPublications(addPublications);
		diff.setModifyName(modifyName);

		if (publications != null) {
			for (PubIds pubIds : publications) {
				if (bioagent.getPublication() != null) {
					for (org.edamontology.edammap.core.input.json.Publication publicationIds : bioagent.getPublication()) {
						if ((!pubIds.getPmid().isEmpty() && publicationIds.getPmid() != null && publicationIds.getPmid().trim().equals(pubIds.getPmid())
								|| !pubIds.getPmcid().isEmpty() && publicationIds.getPmcid() != null && publicationIds.getPmcid().trim().equals(pubIds.getPmcid())
								|| !pubIds.getDoi().isEmpty() && publicationIds.getDoi() != null && PubFetcher.normaliseDoi(publicationIds.getDoi().trim()).equals(pubIds.getDoi()))
							&& (!pubIds.getPmid().isEmpty() && publicationIds.getPmid() != null && !publicationIds.getPmid().isEmpty() && !publicationIds.getPmid().trim().equals(pubIds.getPmid())
								|| !pubIds.getPmcid().isEmpty() && publicationIds.getPmcid() != null && !publicationIds.getPmcid().isEmpty() && !publicationIds.getPmcid().trim().equals(pubIds.getPmcid())
								|| !pubIds.getDoi().isEmpty() && publicationIds.getDoi() != null && !publicationIds.getDoi().isEmpty() && !PubFetcher.normaliseDoi(publicationIds.getDoi().trim()).equals(pubIds.getDoi()))) {
							diff.addModifyPublication(pubIds);
						}
					}
				}
			}
		}

		Set<BioagentsLink<LinkType>> linksLocal = new LinkedHashSet<>();
		if (links != null) {
			linksLocal.addAll(links);
		}
		Set<BioagentsLink<DownloadType>> downloadsLocal = new LinkedHashSet<>();
		if (downloads != null) {
			downloadsLocal.addAll(downloads);
		}
		Set<BioagentsLink<DocumentationType>> documentationsLocal = new LinkedHashSet<>();
		if (documentations != null) {
			documentationsLocal.addAll(documentations);
		}

		if (homepage != null && !homepage.isEmpty()) {
			String homepageTrimmed = Common.trimUrl(homepage);
			if (!linksEqual(homepage, homepageTrimmed, bioagent.getHomepage(), db, false, false)
					&& !linksEqual(homepage, homepageTrimmed, bioagent.getHomepage(), db, true, false)) {
				Webpage webpage = db.getWebpage(bioagent.getHomepage(), false);
				List<String> homepageLinks = new ArrayList<>();
				homepageLinks.add(homepage);
				List<BioagentsLink<LinkType>> linkLinks = new ArrayList<>();
				List<BioagentsLink<DownloadType>> downloadLinks = new ArrayList<>();
				List<BioagentsLink<DocumentationType>> documentationLinks = new ArrayList<>();
				Common.makeBioagentsLinks(homepageLinks, linkLinks, downloadLinks, documentationLinks);
				if (bioagent.getHomepage_status() != 0 && (webpage == null || webpage.isBroken())) {
					diff.setModifyHomepage(homepage);
				} else if (!linkLinks.isEmpty() && linkLinks.get(0).getType() == LinkType.OTHER) {
					diff.setModifyHomepage(homepage);
					List<String> bioagentsHomepageLinks = new ArrayList<>();
					bioagentsHomepageLinks.add(bioagent.getHomepage());
					List<BioagentsLink<LinkType>> bioagentsLinkLinks = new ArrayList<>();
					List<BioagentsLink<DownloadType>> bioagentsDownloadLinks = new ArrayList<>();
					List<BioagentsLink<DocumentationType>> bioagentsDocumentationLinks = new ArrayList<>();
					Common.makeBioagentsLinks(bioagentsHomepageLinks, bioagentsLinkLinks, bioagentsDownloadLinks, bioagentsDocumentationLinks);
					addHomepageToLinks(bioagentsLinkLinks, bioagentsDownloadLinks, bioagentsDocumentationLinks, linksLocal, downloadsLocal, documentationsLocal, db, true);
				} else {
					addHomepageToLinks(linkLinks, downloadLinks, documentationLinks, linksLocal, downloadsLocal, documentationsLocal, db, false);
				}
			}
		}

		for (BioagentsLink<LinkType> link : linksLocal) {
			if (bioagent.getLink() == null) {
				diff.addAddLink(link);
			} else {
				boolean found = false;
				for (Link<LinkType> linkBioagents : bioagent.getLink()) {
					if (linksEqual(link.getUrl(), link.getUrlTrimmed(), linkBioagents.getUrl(), db, false, false)) {
						found = true;
						break;
					}
				}
				if (link.getType().equals(LinkType.OTHER)) {
					if (!found) {
						if (linksEqual(link.getUrl(), link.getUrlTrimmed(), bioagent.getHomepage(), db, false, false) && (diff.getModifyHomepage() == null || diff.getModifyHomepage().isEmpty())) {
							found = true;
						}
					}
					if (!found) {
						for (LinkDownload downloadBioagents : bioagent.getDownload()) {
							if (linksEqual(link.getUrl(), link.getUrlTrimmed(), downloadBioagents.getUrl(), db, false, false)) {
								found = true;
								break;
							}
						}
					}
					if (!found) {
						for (Link<DocumentationType> documentationBioagents : bioagent.getDocumentation()) {
							if (linksEqual(link.getUrl(), link.getUrlTrimmed(), documentationBioagents.getUrl(), db, false, true)) {
								found = true;
								break;
							}
						}
					}
				}
				if (!found) {
					diff.addAddLink(link);
				}
			}
		}

		for (BioagentsLink<DownloadType> download : downloadsLocal) {
			if (bioagent.getDownload() == null) {
				diff.addAddDownload(download);
			} else {
				boolean found = false;
				for (LinkDownload downloadBioagents : bioagent.getDownload()) {
					if (linksEqual(download.getUrl(), download.getUrlTrimmed(), downloadBioagents.getUrl(), db, false, false)) {
						found = true;
						break;
					}
				}
				if (!found) {
					diff.addAddDownload(download);
				}
			}
		}

		for (BioagentsLink<DocumentationType> documentation : documentationsLocal) {
			if (bioagent.getDocumentation() == null) {
				diff.addAddDocumentation(documentation);
			} else {
				boolean found = false;
				for (Link<DocumentationType> documentationBioagents : bioagent.getDocumentation()) {
					if (linksEqual(documentation.getUrl(), documentation.getUrlTrimmed(), documentationBioagents.getUrl(), db, true, true)) {
						found = true;
						break;
					}
				}
				if (!found) {
					diff.addAddDocumentation(documentation);
				}
			}
		}

		if (license != null) {
			if (!license.isEmpty() && (bioagent.getLicense() == null || !bioagent.getLicense().equals(license.getObject()))) {
				diff.setModifyLicense(license);
			}
		}

		for (Provenance language : languages) {
			if (!language.isEmpty() && (bioagent.getLanguage() == null || !bioagent.getLanguage().contains(language.getObject()))) {
				diff.addAddLanguage(language);
			}
		}

		for (CorrespAuthor credit : credits) {
			if (bioagent.getCredit() == null) {
				diff.addAddCredit(credit);
			} else {
				boolean found = false;
				boolean foundModify = false;
				for (Credit creditBioagents : bioagent.getCredit()) {
					if ((credit.getName().isEmpty() || credit.getName().equals(creditBioagents.getName()))
							&& (credit.getOrcid().isEmpty() || credit.getOrcid().equals(creditBioagents.getOrcidid()))
							&& (credit.getEmail().isEmpty() || credit.getEmail().equals(creditBioagents.getEmail()))) {
						found = true;
						break;
					}
					if (creditBioagents.getName() != null && Common.creditNameEqual(credit.getName(), creditBioagents.getName())
							|| creditBioagents.getOrcidid() != null && Common.creditOrcidEqual(credit.getOrcid(), creditBioagents.getOrcidid())
							|| creditBioagents.getEmail() != null && Common.creditEmailEqual(credit.getEmail(), creditBioagents.getEmail())) {
						foundModify = true;
					}
				}
				if (!found) {
					if (!foundModify) {
						diff.addAddCredit(credit);
					} else {
						diff.addModifyCredit(credit);
					}
				}
			}
		}

		return diff;
	}

	static void addDiff(List<Diff> diffs, Diff diff) {
		boolean added = false;
		for (int i = diffs.size() - 1; i >= 0; --i) {
			if (diff.getExisting() == diffs.get(i).getExisting() && diff.include() && diffs.get(i).include()) {
				diffs.add(i + 1, diff);
				added = true;
				break;
			}
		}
		if (!added) {
			diffs.add(diff);
		}
	}
}
