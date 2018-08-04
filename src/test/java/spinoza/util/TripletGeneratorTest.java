package spinoza.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static spinoza.util.TripletGenerator.extractDBpediaTypes;
import static spinoza.util.TripletGenerator.queryDBpediaTypes;
import static spinoza.util.TripletGenerator.queryOntology;
import static spinoza.util.TripletGenerator.abbr;
import static spinoza.util.TripletGenerator.sanitize;
import it.uniroma1.lcl.babelnet.BabelSense;
import it.uniroma1.lcl.babelnet.data.BabelSenseSource;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.jlt.util.Language;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import edu.mit.jwi.item.POS;

public class TripletGeneratorTest {

	@Test
	public void testSparql() {
		List<String> types = queryDBpediaTypes(
				"http://dbpedia.org/resource/The_Hague", "http://dbpedia.org/sparql");
		for (String type : types) {
			assertTrue(type.startsWith("http://dbpedia.org/ontology/"));
		}
	}
	
	@Test
	public void testQueryOntology() {
		List<String[]> pairs = queryOntology("http://dbpedia.org/sparql", 5, 0);
		assertEquals(5, pairs.size());
	}
	
	@Test
	public void testSanitize() {
		assertEquals("http://dbpedia.org/resource/Irwin_%22Ike%22_H._Hoover", 
				sanitize("http://dbpedia.org/resource/Irwin_\"Ike\"_H._Hoover"));
		assertEquals("http://dbpedia.org/resource/Jang_%60Ali",
				sanitize("http://dbpedia.org/resource/Jang_`Ali"));
	}
	
	private static String captureOutput(Runnable runnable) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream stdout = System.out;
		System.setOut(new PrintStream(out));
		runnable.run();
		System.setOut(stdout);
		String output = new String(out.toByteArray(), Charset.forName("UTF-8"));
		return output;
	}
	
}
