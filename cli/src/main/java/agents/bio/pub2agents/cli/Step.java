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

package agents.bio.pub2agents.cli;

public enum Step {
	none("None"),
	fetchPub("-fetch-pub"),
	pass1("-pass1"),
	fetchWeb("-fetch-web"),
	pass2("-pass2"),
	map("-map"),
	done("Done");

	private final String name;

	private Step(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public static Step fromString(String name) {
		for (Step step : Step.values()) {
			if (step.name.equalsIgnoreCase(name)) {
				return step;
			}
		}
		return Step.none;
	}
}
