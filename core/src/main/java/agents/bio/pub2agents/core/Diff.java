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

import org.edamontology.pubfetcher.core.db.publication.CorrespAuthor;

import org.edamontology.edammap.core.input.json.DocumentationType;
import org.edamontology.edammap.core.input.json.DownloadType;
import org.edamontology.edammap.core.input.json.LinkType;

public class Diff {

	private double scoreScore2 = -1;

	private Set<Integer> possiblyRelated = null;

	private int existing = -1;

	private Set<PubIds> modifyPublications = new LinkedHashSet<>();

	private Collection<PubIds> addPublications = null;

	private String modifyName = null;

	private String modifyHomepage = null;

	private Set<BioagentsLink<LinkType>> addLinks = new LinkedHashSet<>();

	private Set<BioagentsLink<DownloadType>> addDownloads = new LinkedHashSet<>();

	private Set<BioagentsLink<DocumentationType>> addDocumentations = new LinkedHashSet<>();

	private Provenance modifyLicense = null;

	private Set<Provenance> addLanguages = new LinkedHashSet<>();

	private List<CorrespAuthor> modifyCredits = new ArrayList<>();

	private List<CorrespAuthor> addCredits = new ArrayList<>();

	public boolean include() {
		return possiblyRelated != null && !possiblyRelated.isEmpty()
			|| !modifyPublications.isEmpty() || addPublications != null && !addPublications.isEmpty()
			|| modifyName != null && !modifyName.isEmpty() || modifyHomepage != null && !modifyHomepage.isEmpty()
			|| !addLinks.isEmpty() || !addDownloads.isEmpty() || !addDocumentations.isEmpty()
			|| modifyLicense != null && !modifyLicense.isEmpty() || !addLanguages.isEmpty()
			|| !modifyCredits.isEmpty() || !addCredits.isEmpty();
	}

	public double getScoreScore2() {
		return scoreScore2;
	}
	public void setScoreScore2(double scoreScore2) {
		this.scoreScore2 = scoreScore2;
	}

	public Set<Integer> getPossiblyRelated() {
		return possiblyRelated;
	}
	public void setPossiblyRelated(Set<Integer> possiblyRelated) {
		this.possiblyRelated = possiblyRelated;
	}

	public int getExisting() {
		return existing;
	}
	public void setExisting(int existing) {
		this.existing = existing;
	}

	public Set<PubIds> getModifyPublications() {
		return modifyPublications;
	}
	public void addModifyPublication(PubIds publication) {
		modifyPublications.add(publication);
	}

	public Collection<PubIds> getAddPublications() {
		return addPublications;
	}
	public void setAddPublications(Collection<PubIds> addPublications) {
		this.addPublications = addPublications;
	}

	public String getModifyName() {
		return modifyName;
	}
	public void setModifyName(String modifyName) {
		this.modifyName = modifyName;
	}

	public String getModifyHomepage() {
		return modifyHomepage;
	}
	public void setModifyHomepage(String modifyHomepage) {
		this.modifyHomepage = modifyHomepage;
	}

	public Set<BioagentsLink<LinkType>> getAddLinks() {
		return addLinks;
	}
	public void addAddLink(BioagentsLink<LinkType> link) {
		addLinks.add(link);
	}

	public Set<BioagentsLink<DownloadType>> getAddDownloads() {
		return addDownloads;
	}
	public void addAddDownload(BioagentsLink<DownloadType> download) {
		addDownloads.add(download);
	}

	public Set<BioagentsLink<DocumentationType>> getAddDocumentations() {
		return addDocumentations;
	}
	public void addAddDocumentation(BioagentsLink<DocumentationType> documentation) {
		addDocumentations.add(documentation);
	}

	public Provenance getModifyLicense() {
		return modifyLicense;
	}
	public void setModifyLicense(Provenance modifyLicense) {
		this.modifyLicense = modifyLicense;
	}

	public Set<Provenance> getAddLanguages() {
		return addLanguages;
	}
	public void addAddLanguage(Provenance language) {
		addLanguages.add(language);
	}

	public List<CorrespAuthor> getModifyCredits() {
		return modifyCredits;
	}
	public void addModifyCredit(CorrespAuthor credit) {
		modifyCredits.add(credit);
	}

	public List<CorrespAuthor> getAddCredits() {
		return addCredits;
	}
	public void addAddCredit(CorrespAuthor credit) {
		addCredits.add(credit);
	}
}
