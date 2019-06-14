package spinoza.blookup;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSense;
import it.uniroma1.lcl.babelnet.data.BabelSenseSource;
import it.uniroma1.lcl.babelnet.data.BabelPointer;
import it.uniroma1.lcl.babelnet.BabelNetQuery;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetRelation;
import it.uniroma1.lcl.babelnet.resources.WikipediaID;
import it.uniroma1.lcl.babelnet.WordNetSynsetID;
import it.uniroma1.lcl.babelnet.BabelSynsetID;
import it.uniroma1.lcl.jlt.util.Language;
import com.babelscape.util.UniversalPOS;
import it.uniroma1.lcl.babelnet.WordNetSense;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class BabelNetRequestHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(BabelNetRequestHandler.class);

    private static final Pattern TEXT_REQUESTS = Pattern
            .compile("^/text/([^/]+)/([^/]+)/([^/]+)?$");
    private static final Pattern TEXT_NON_REDIRECT_REQUESTS = Pattern
            .compile("^/textnr/([^/]+)/([^/]+)/([^/]+)$");
    private static final Pattern WORDNET_REQUESTS = Pattern
            .compile("^/wordnet/([^/]+)$");
    private static final Pattern WIKIPEDIA_REQUESTS = Pattern
            .compile("^/wikipedia/([^/]+)(?:/([^/]))$");
    private static final Pattern SYNSET_TYPE_REQUESTS = Pattern
            .compile("^/synset/([^/]+)/type$");
    private static final Pattern RELATED_SYNSET_REQUESTS = Pattern
            .compile("^/synset/([^/]+)/related$");
    private static final Pattern SENSES_REQUESTS = Pattern
            .compile("^/synset/([^/]+)/senses(?:/(\\w+))$");
    private static final Pattern DBPEDIA_URI_REQUESTS = Pattern
            .compile("^/synset/([^/]+)/dbpedia_uri(?:/([^/]+))?$");
    private static final Pattern SYNSET_TO_WORDNET_REQUESTS = Pattern
            .compile("^/synset/([^/]+)/wn$");

    private BabelNet bn = BabelNet.getInstance();

    public synchronized void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        LOGGER.debug("Target: " + target);
        response.setContentType("text/plain");
        boolean handled = handleWordNetRequest(target, response) || 
        		handleTextRequest(target, response) ||
        		handleTextNonRedirectRequest(target, response) ||
                handleWikipediaRequest(target, response) || 
                handleRelatedSynsetRequest(target, response) ||
                handleSensesRequest(target, response) ||
                handleDBpediaRequest(target, response) ||
                handleSynsetTypeRequest(target, response) ||
                handleSynsetToWordnetRequest(target, response);
        baseRequest.setHandled(handled);
        // response.sendError(404);
    }

    private boolean handleWikipediaRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = WIKIPEDIA_REQUESTS.matcher(target);
        if (matcher.find()) {
            String title = matcher.group(1);
            UniversalPOS pos = UniversalPOS.valueOf(matcher.group(2).charAt(0));
            LOGGER.debug("Wikipedia title: " + title + ", POS: " + pos);
            List<BabelSynset> synsets = bn.getSynsets(new WikipediaID(title, Language.EN, pos));
            if (synsets == null || synsets.isEmpty()) {
                    BabelNetQuery q = new BabelNetQuery.Builder(title).from(Language.EN).POS(pos).build();
	            synsets = bn.getSynsets(q);
            }
            if (synsets != null && !synsets.isEmpty()) {
                for (BabelSynset synset : synsets) {
                    response.getWriter().println(synset.getId());
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleWordNetRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = WORDNET_REQUESTS.matcher(target);
        if (matcher.find()) {
            String offset = matcher.group(1);
            LOGGER.debug("WordNet offset: " + offset);
            System.out.println(bn.getSynsets(new WordNetSynsetID(offset)));
	    List<BabelSynset> synsets = bn.getSynsets(new WordNetSynsetID(offset));
            if (synsets != null && !synsets.isEmpty()) {
                for (BabelSynset synset : synsets) {
                    response.getWriter().println(synset.getId());
                }
                return true;
            }
        }
        return false;
    }


    private boolean handleTextRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = TEXT_REQUESTS.matcher(target);
        if (matcher.find()) {
            String langId = matcher.group(1);
            String query = matcher.group(2);
            String posStr = matcher.group(3);
            Language lang = Language.valueOf(langId.toUpperCase());
            List<BabelSynset> synsets;
            if (posStr == null) {
            	synsets	= bn.getSynsets(query, lang);
            } else {
            	UniversalPOS pos = null;
            	try {
            		pos = UniversalPOS.valueOf(posStr.toUpperCase());
            	} catch (IllegalArgumentException ex) {
                	if (posStr.length() == 1) {
                		pos = UniversalPOS.valueOf(posStr.charAt(0));
                	}
            	}
            	synsets	= bn.getSynsets(query, lang, pos);
            }
            if (synsets != null && !synsets.isEmpty()) {
                for (BabelSynset synset : synsets) {
                    response.getWriter().println(synset.getId());
                }
                return true;
            }
        }
        return false;
    }


    private boolean handleTextNonRedirectRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = TEXT_NON_REDIRECT_REQUESTS.matcher(target);
        if (matcher.find()) {
            String langId = matcher.group(1);
            String posId = matcher.group(2);
            String query = matcher.group(3);
            Language lang = Language.valueOf(langId.toUpperCase());
            UniversalPOS pos = UniversalPOS.valueOf(posId.charAt(0));
            BabelNetQuery q = new BabelNetQuery.Builder(query).from(lang).POS(pos).normalized(false).build();
	    List<BabelSynset> synsets = bn.getSynsets(q);
            if (synsets != null && !synsets.isEmpty()) {
                for (BabelSynset synset : synsets) {
                    response.getWriter().println(synset.getId());
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleRelatedSynsetRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = RELATED_SYNSET_REQUESTS.matcher(target);
        if (matcher.find()) {
            String id = matcher.group(1);
            LOGGER.debug("BabelNet ID: " + id);
            BabelSynset synset = bn.getSynset(new BabelSynsetID(id));
            if (synset != null) {
		for (BabelSynsetRelation rel : synset.getOutgoingEdges()) {
		    BabelPointer pointer = rel.getPointer();
                    response.getWriter().format("%s\t%s\n",
                            pointer.getSymbol(), rel.getTarget());
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleSynsetTypeRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = SYNSET_TYPE_REQUESTS.matcher(target);
        if (matcher.find()) {
            String id = matcher.group(1);
            LOGGER.debug("BabelNet ID: " + id);
            BabelSynset synset = bn.getSynset(new BabelSynsetID(id));
            if (synset != null) {
            	response.getWriter().write(synset.getSynsetType().name());
                return true;
            }
        }
        return false;
    }

    private boolean handleSensesRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = SENSES_REQUESTS.matcher(target);
        if (matcher.find()) {
            String id = matcher.group(1);
            String langId = matcher.group(2);
            LOGGER.debug("BabelNet ID: " + id);
            BabelSynset synset = bn.getSynset(new BabelSynsetID(id));
            if (synset != null) {
            	List<BabelSense> senses;
            	if (langId == null || langId.isEmpty()) {
					senses = synset.getSenses();
				} else {
					senses = synset.getSenses(Language.valueOf(langId.toUpperCase()));
				}
                for (BabelSense sense : senses) {
                    response.getWriter().format("%s\t%s\t%s\t%s\n",
                            sense.getFullLemma(), sense.getPOS(),
                            sense.getLanguage(), sense.getSource());
                }
                return true;
            }
        }
        return false;
    }
    
    private boolean handleDBpediaRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = DBPEDIA_URI_REQUESTS.matcher(target);
        if (matcher.find()) {
            String id = matcher.group(1);
            String langId = matcher.group(2);
            LOGGER.debug("BabelNet ID: " + id);
            BabelSynset synset = bn.getSynset(new BabelSynsetID(id));
            if (synset != null) {
            	List<String> uris;
            	if (langId == null || langId.isEmpty()) {
					uris = synset.getDBPediaURIs();
				} else {
					uris = synset.getDBPediaURIs(Language.valueOf(langId.toUpperCase()));
				}
                for (String uri : uris) {
                    response.getWriter().println(uri);
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleSynsetToWordnetRequest(String target,
            HttpServletResponse response) throws IOException {
        Matcher matcher = SYNSET_TO_WORDNET_REQUESTS.matcher(target);
        if (matcher.find()) {
            String id = matcher.group(1);
            LOGGER.debug("BabelNet ID: " + id);
            BabelSynset synset = bn.getSynset(new BabelSynsetID(id));
            List<WordNetSynsetID> wnSynsets = synset.getWordNetOffsets();
            boolean found = false;
            for (WordNetSynsetID wnSynset : wnSynsets) {
                response.getWriter().println(wnSynset.toString());
                found = true;
            }
            return found;
        }
        return false;
    }
}
