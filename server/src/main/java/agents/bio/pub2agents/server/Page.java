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

import java.io.IOException;
import java.io.StringWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edamontology.edammap.core.args.CoreArgs;
import org.edamontology.edammap.core.output.Params;
import org.edamontology.edammap.core.query.Query;

public final class Page {

	private static final Logger logger = LogManager.getLogger();

	private static final String PUB2TOOLS_RESULTS = "pub2agents-results";
	private static final String TO_BIOTOOLS = "to-bioagents";

	static String get(CoreArgs args) {
		StringWriter writer = new StringWriter();

		writer.write("<!DOCTYPE html>\n");
		writer.write("<html lang=\"en\">\n\n");

		writer.write("<head>\n");
		writer.write("\t<meta charset=\"utf-8\">\n");
		writer.write("\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		writer.write("\t<meta name=\"author\" content=\"Erik Jaaniso\">\n");
		writer.write("\t<meta name=\"description\" content=\"A agent that constructs entry candidates for the bio.agents software registry from suitable publications.\">\n");
		writer.write("\t<title>" + Server.version.getName() + " " + Server.version.getVersion() + "</title>\n");
		writer.write("\t<link rel=\"stylesheet\" href=\"" + Server.args.getServerPrivateArgs().getPath() + "/style.css\">\n");
		writer.write("</head>\n\n");

		writer.write("<body>\n\n");

		writer.write("<header>\n\n");

		writer.write("<h1>Pub2Agents " + Server.version.getVersion() + "</h1>\n\n");

		writer.write("<p><a href=\"https://github.com/bio-agents/pub2agents\">https://github.com/bio-agents/pub2agents</a></p>\n\n");

		writer.write("</header>\n\n");

		writer.write("<main>\n\n");

		writer.write("<section id=\"input\">\n\n");

		writer.write("<article>\n");
		writer.write("\t<h2 class=\"step\">1.</h2>\n");

		writer.write("\t<section id=\"" + Query.PUBLICATION_IDS + "-section\">\n");
		writer.write("\t\t<h3>Publications</h3>\n");
		writer.write("\t\t<div>\n");
		writer.write("\t\t\t<div class=\"input\">\n");
		writer.write("\t\t\t\t<textarea id=\"" + Query.PUBLICATION_IDS + "\" name=\"" + Query.PUBLICATION_IDS + "\" rows=\"3\" onblur=\"check('" + Query.PUBLICATION_IDS + "','" + Server.args.getServerPrivateArgs().getPath() + "/api/pub')\" required maxlength=\"" + Resource.MAX_PUBLICATION_IDS_LENGTH + "\" placeholder=\"PMID/PMCID/DOI of journal article\"></textarea>\n");
		writer.write("\t\t\t\t<span class=\"more\" tabindex=\"0\"></span>\n");
		writer.write("\t\t\t\t<div class=\"more-box\" tabindex=\"0\">\n");
		writer.write("\t\t\t\t\tPMID/PMCID/DOI of journal article<br><span class=\"ex\">Ex:</span> <span class=\"example\">17478515<br>PMC3125778<br>10.1093/nar/gkw199</span>\n");
		writer.write("\t\t\t\t</div>\n");
		writer.write("\t\t\t</div>\n");
		writer.write("\t\t\t<div id=\"" + Query.PUBLICATION_IDS + "-output\" class=\"output\"></div>\n");
		writer.write("\t\t</div>\n");
		writer.write("\t</section>\n");

		writer.write("\t<section id=\"" + Query.NAME + "-section\">\n");
		writer.write("\t\t<h3>Name</h3>\n");
		writer.write("\t\t<div>\n");
		writer.write("\t\t\t<div class=\"input\">\n");
		writer.write("\t\t\t\t<input type=\"text\" id=\"" + Query.NAME + "\" name=\"" + Query.NAME + "\" maxlength=\"" + Resource.MAX_NAME_LENGTH + "\" placeholder=\"Name of agent or service (not mandatory)\">\n");
		writer.write("\t\t\t\t<span class=\"more\" tabindex=\"0\"></span>\n");
		writer.write("\t\t\t\t<div class=\"more-box\" tabindex=\"0\">\n");
		writer.write("\t\t\t\t\tName of agent or service (not mandatory)<br><span class=\"ex\">Ex:</span> <span class=\"example\">g:Profiler</span>\n");
		writer.write("\t\t\t\t</div>\n");
		writer.write("\t\t\t</div>\n");
		writer.write("\t\t</div>\n");
		writer.write("\t</section>\n");

		writer.write("\t<section id=\"" + Query.WEBPAGE_URLS + "-section\">\n");
		writer.write("\t\t<h3>Links</h3>\n");
		writer.write("\t\t<div>\n");
		writer.write("\t\t\t<div class=\"input\">\n");
		writer.write("\t\t\t\t<textarea id=\"" + Query.WEBPAGE_URLS + "\" name=\"" + Query.WEBPAGE_URLS + "\" rows=\"3\" onblur=\"check('" + Query.WEBPAGE_URLS + "','" + Server.args.getServerPrivateArgs().getPath() + "/api/web')\" maxlength=\"" + Resource.MAX_LINKS_LENGTH + "\" placeholder=\"URLs of homepage, etc (not mandatory)\"></textarea>\n");
		writer.write("\t\t\t\t<span class=\"more\" tabindex=\"0\"></span>\n");
		writer.write("\t\t\t\t<div class=\"more-box\" tabindex=\"0\">\n");
		writer.write("\t\t\t\t\tURLs of homepage, etc (not mandatory)<br><span class=\"ex\">Ex:</span> <span class=\"example\">https://biit.cs.ut.ee/gprofiler/</span>\n");
		writer.write("\t\t\t\t</div>\n");
		writer.write("\t\t\t</div>\n");
		writer.write("\t\t\t<div id=\"" + Query.WEBPAGE_URLS + "-output\" class=\"output\"></div>\n");
		writer.write("\t\t</div>\n");
		writer.write("\t</section>\n");

		writer.write("\t<div id=\"withoutmap\" class=\"button\"><div><input type=\"submit\" value=\"Run Pub2Agents\" onclick=\"run('withoutmap','" + Server.args.getServerPrivateArgs().getPath() + "/api')\"></div><div id=\"withoutmap-output\" class=\"button-output\"></div></div>");
		writer.write("<div id=\"all\" class=\"button\"><div><input type=\"submit\" value=\"Run all\" onclick=\"run('all','" + Server.args.getServerPrivateArgs().getPath() + "/api')\"></div><div id=\"all-output\" class=\"button-output\"></div></div>\n");

		writer.write("</article>\n\n");

		writer.write("<article>\n");
		writer.write("\t<h2 class=\"step\">2.</h2>\n");

		writer.write("\t<section id=\"" + PUB2TOOLS_RESULTS + "-section\">\n");
		writer.write("\t\t<h3>Pub2Agents results</h3>\n");
		writer.write("\t\t<div>\n");
		writer.write("\t\t\t<div class=\"input\">\n");
		writer.write("\t\t\t\t<textarea id=\"" + PUB2TOOLS_RESULTS + "\" name=\"" + PUB2TOOLS_RESULTS + "\" rows=\"12\"></textarea>\n");
		writer.write("\t\t\t\t<span class=\"more\" tabindex=\"0\"></span>\n");
		writer.write("\t\t\t\t<div class=\"more-box\" tabindex=\"0\">\n");
		writer.write("\t\t\t\t\tOutput of Pub2Agents<br>Can be edited before being sent to EDAMmap for final bio.agents entry generation\n");
		writer.write("\t\t\t\t</div>\n");
		writer.write("\t\t\t</div>\n");
		writer.write("\t\t\t<div id=\"" + PUB2TOOLS_RESULTS + "-output\" class=\"output output-bad\"></div>\n");
		writer.write("\t\t</div>\n");
		writer.write("\t</section>\n");

		writer.write("\t<div id=\"map\" class=\"button\"><div><input type=\"submit\" value=\"Run EDAMmap\" onclick=\"run('map','" + Server.args.getServerPrivateArgs().getPath() + "/api')\"></div><div id=\"map-output\" class=\"button-output\"></div></div>\n");

		writer.write("</article>\n\n");

		writer.write("</section>\n");
		writer.write("<section id=\"output\">\n\n");

		writer.write("<article>\n");
		writer.write("\t<h2 class=\"step\">3.</h2>\n");

		writer.write("\t<section id=\"" + TO_BIOTOOLS + "-section\">\n");
		writer.write("\t\t<h3 onclick=\"document.getElementById('to-bioagents-output').focus(); document.getElementById('to-bioagents-output').select()\" style=\"cursor: pointer;\">To bio.agents</h3>\n");
		writer.write("\t\t<div>\n");
		writer.write("\t\t\t<textarea id=\"" + TO_BIOTOOLS + "-output\" class=\"output\" readonly></textarea>\n");
		writer.write("\t\t</div>\n");
		writer.write("\t</section>\n");

		writer.write("</article>\n\n");

		writer.write("</section>\n\n");

		writer.write("</main>\n\n");

		writer.write("<footer>\n\n");

		writer.write("<h2 id=\"parameters\" onclick=\"if (this.classList.contains('opened')) { this.classList.remove('opened'); document.getElementById('tabs').style.display = 'none' } else { this.classList.add('opened'); document.getElementById('tabs').style.display = 'block' }\" style=\"cursor: pointer;\">Parameters</h2>\n\n");

		writer.write("<section id=\"tabs\" style=\"display: none;\">\n");
		writer.write("\n");
		try {
			Params.writeMain(Server.getArgsMain(true), writer);
			Params.writeProcessing(args.getProcessorArgs(), writer);
			Params.writePreProcessing(args.getPreProcessorArgs(), writer, true);
			Params.writeFetching(args.getFetcherArgs(), writer, false, true);
			Params.writeMapping(args.getMapperArgs(), writer, true);
			Params.writeCountsEdamOnly(writer, Server.concepts);
		} catch (IOException e) {
			logger.error("Exception!", e);
			// should not happen, as writing only to StringWriter
		}
		writer.write("</section>\n\n");

		writer.write("</footer>\n\n");

		writer.write("<script src=\"" + Server.args.getServerPrivateArgs().getPath() + "/script.js\"></script>\n\n");

		writer.write("</body>\n\n");

		writer.write("</html>\n");

		return writer.toString();
	}
}
