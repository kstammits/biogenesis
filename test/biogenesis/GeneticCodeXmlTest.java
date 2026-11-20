/* Copyright (C) 2006-2010  Joan Queralt Molina
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package biogenesis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * .bgg XML round-trip plus the XXE guard: external entities must never
 * resolve, whatever the payload claims.
 */
public class GeneticCodeXmlTest {
	static GeneticCode sampleCode() {
		List<Gene> genes = new ArrayList<>();
		genes.add(new Gene(4.0, 0.0, Color.RED));
		genes.add(new Gene(6.0, 1.2, Color.GREEN));
		genes.add(new Gene(3.0, 2.5, Color.BLUE));
		return new GeneticCode(genes, 2, 0, false);
	}

	static GeneticCode parseBytes(byte[] xml) throws Exception {
		File f = File.createTempFile("code", ".bgg");
		try (FileWriter w = new FileWriter(f, StandardCharsets.UTF_8)) {
			w.write(new String(xml, StandardCharsets.UTF_8));
		}
		try {
			return new BioXMLParser().parseGeneticCode(f);
		} finally {
			f.delete();
		}
	}

	@Test
	public void testWriteParseRoundTrip() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (PrintStream ps = new PrintStream(bytes, false, StandardCharsets.UTF_8)) {
			BioXMLParser.writeGeneticCode(ps, sampleCode());
		}
		GeneticCode back = parseBytes(bytes.toByteArray());
		assertEquals(2, back.getSymmetry());
		assertEquals(0, back.getMirror());
		assertEquals(false, back.getDisperseChildren());
		assertEquals(3, back.getNGenes());
		assertEquals(Color.RED, back.getGene(0).getColor());
		assertEquals(6.0, back.getGene(1).getLength(), 0.0);
		assertEquals(2.5, back.getGene(2).getTheta(), 0.0);
	}

	@Test
	public void testExternalEntityBlocked() throws Exception {
		// Internal entities are part of our own file format and must keep
		// working; external ones must never resolve.
		String ok = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<!DOCTYPE genetic_code [ <!ENTITY half \"2.\"> "
				+ "<!ELEMENT genetic_code (gene+)> "
				+ "<!ATTLIST genetic_code symmetry (1|2|3|4|5|6|7|8) #REQUIRED> "
				+ "<!ATTLIST genetic_code mirror (yes|no) #REQUIRED> "
				+ "<!ATTLIST genetic_code disperse (yes|no) #REQUIRED> "
				+ "<!ELEMENT gene EMPTY> "
				+ "<!ATTLIST gene length CDATA #REQUIRED> "
				+ "<!ATTLIST gene theta CDATA #REQUIRED> "
				+ "<!ATTLIST gene color (red|green|blue|cyan|yellow|gray|white) #REQUIRED> ]>\n"
				+ "<genetic_code symmetry=\"1\" mirror=\"no\" disperse=\"no\">\n"
				+ "<gene length=\"&half;5\" theta=\"0\" color=\"red\" />\n"
				+ "</genetic_code>\n";
		assertEquals(2.5, parseBytes(ok.getBytes(StandardCharsets.UTF_8)).getGene(0).getLength(), 0.0);

		String evil = ok.replace("<!ENTITY half \"2.\">",
				"<!ENTITY xxe SYSTEM \"file:///etc/hostname\">").replace("&half;5", "&xxe;");
		try {
			parseBytes(evil.getBytes(StandardCharsets.UTF_8));
			fail("expected SAXException for external entity");
		} catch (SAXException expected) {
			// Blocked at the parser: file content never reaches the sim.
		}
	}
}
