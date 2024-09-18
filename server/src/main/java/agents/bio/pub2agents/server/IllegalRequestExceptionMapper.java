/*
 * Copyright © 2023 Erik Jaaniso
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

package agents.bio.pub2agents.server;

import jakarta.ws.rs.ext.Provider;

import org.edamontology.edammap.server.IllegalRequestExceptionMapperBase;

@Provider
public class IllegalRequestExceptionMapper extends IllegalRequestExceptionMapperBase {

	@Override
	protected String getServerName() {
		return Server.version.getName() + " " + Server.version.getVersion();
	}
}
