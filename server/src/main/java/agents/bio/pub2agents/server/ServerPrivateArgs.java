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

import com.beust.jcommander.Parameter;

import org.edamontology.edammap.server.ServerPrivateArgsBase;

import org.edamontology.pubfetcher.core.common.Arg;

public class ServerPrivateArgs extends ServerPrivateArgsBase {

	private static final String pathId = "path";
	private static final String pathDescription = "Path where the server will be deployed (only one single path segment supported, prepend with '/')";
	private static final String pathDefault = "/pub2agents";
	@Parameter(names = { "-p", "--" + pathId }, description = pathDescription)
	private String path = pathDefault;

	@Override
	protected void addArgs() {
		super.addArgs();
		args.add(new Arg<>(this::getPath, null, pathDefault, pathId, "", pathDescription, null));
	}

	@Override
	public String getLabel() {
		return "Pub2Agents-Server (private)";
	}

	public String getPath() {
		return path;
	}
}
