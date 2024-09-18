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

import java.io.File;

import org.edamontology.edammap.server.ServerArgsBase;
import org.edamontology.pubfetcher.core.common.Arg;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class ServerArgs extends ServerArgsBase {

	static final String bioagentsId = "bioagents";
	private static final String bioagentsDescription = "Path of the bio.agents existing content file in JSON format; will be automatically fetched and periodically updated";
	private static final String bioagentsDefault = null;
	@Parameter(names = { "--" + bioagentsId }, required = true, description = bioagentsDescription)
	private String bioagents;

	@ParametersDelegate
	private ServerPrivateArgs serverPrivateArgs = new ServerPrivateArgs();

	@Override
	protected void addArgs() {
		super.addArgs();
		args.add(new Arg<>(this::getBioagentsFilename, null, bioagentsDefault, bioagentsId, "bio.agents file", bioagentsDescription, null, "https://bio.agents"));
	}

	@Override
	public String getLabel() {
		return "Pub2Agents-Server";
	}

	public String getBioagents() {
		return bioagents;
	}
	public String getBioagentsFilename() {
		return new File(bioagents).getName();
	}

	public ServerPrivateArgs getServerPrivateArgs() {
		return serverPrivateArgs;
	}
}
