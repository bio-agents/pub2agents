/*
 * Copyright © 2018 Erik Jaaniso
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

public class BioagentsLink<T> {

	private final String url;

	private final String urlTrimmed;

	private final T type;

	public BioagentsLink(String url, T type) {
		this.url = Common.prependHttp(url);
		this.urlTrimmed = Common.trimUrl(url);
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public String getUrlTrimmed() {
		return urlTrimmed;
	}

	public T getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof BioagentsLink)) return false;
		BioagentsLink<?> other = (BioagentsLink<?>) obj;
		if (urlTrimmed == null) {
			if (other.urlTrimmed != null) return false;
		} else if (!urlTrimmed.equals(other.urlTrimmed)) return false;
		if (type == null) {
			if (other.type != null) return false;
		} else if (!type.equals(other.type)) return false;
		return other.canEqual(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((urlTrimmed == null) ? 0 : urlTrimmed.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	public boolean canEqual(Object other) {
		return (other instanceof BioagentsLink);
	}
}
