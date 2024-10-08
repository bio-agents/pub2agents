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

import java.util.LinkedHashSet;
import java.util.Set;

public class Provenance {

	private final String object;

	private final Set<String> provenances;

	public Provenance() {
		object = "";
		provenances = null;
	}

	public Provenance(String object, String provenance) {
		this.object = object;
		this.provenances = new LinkedHashSet<>();
		this.provenances.add(provenance);
	}

	public Provenance(String object, Set<String> provenances) {
		this.object = object;
		this.provenances = new LinkedHashSet<>();
		this.provenances.addAll(provenances);
	}

	public boolean isEmpty() {
		return object == null || object.isEmpty();
	}

	public String getObject() {
		return object;
	}

	public Set<String> getProvenances() {
		return provenances;
	}
	public void addProvenance(String provenance) {
		provenances.add(provenance);
	}
	public void addProvenances(Set<String> provenances) {
		this.provenances.addAll(provenances);
	}

	@Override
	public String toString() {
		if (!isEmpty()) {
			return object + " (" + String.join(", ", provenances) + ")";
		} else {
			return "";
		}
	}
}
