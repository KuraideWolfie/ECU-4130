/**
 * File:    Token.java
 * Author:  Matthew Morgan
 * Date:    28 March 2018
 * Version: 3.0
 * Description:
 * Token represents a single Token that can be parsed in any corpus, containing
 * data regarding unstemmed variants, postings of the token in different
 * documents, and the stemmed version of the token. Frequency data can be
 * inferred from the postings list of the token
 * 
 * ~~~ VERSION HISTORY ~~~
 * Version 1.0 (26 January 2018)
 * Version 2.0 (22 February 2018)
 * Version 3.0 (28 March 2018)
 * - The TokenDictionary hierarchy was eliminated in favor of only having the
 *   Token class exist. (Index.java, TokenDictionary.java, and
 *   IndexDocument.java no longer exist)
 * - Token no longer stores frequencies, as this information can be inferred
 * - Token no longer saves its own information - instead, toString() has been
 *   overriden, and a read function provided for reloading token data
 */

// Import statements
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Collections;
import java.lang.Math;

class Token {
  // stem is the stemmed version of all the variants
  // variants is a set of all unstemmed words that the stem represents
  // postings is a hashtable of lists for document postings
  public String stem;
  public HashSet<String> variants;
  public Hashtable<Integer,ArrayList<Integer>> postings;

  /** Instantiates a token, adding the provided string as a variant and setting
   *  its stem */
  public Token(String txt) {
    variants = new HashSet<>();
    postings = new Hashtable<>();
    stem = stemToken(txt);
    variants.add(txt);
  }

  /**
   * Takes a base token - that is, a single word - and stems it using the
   * Porter Stemmer algorithm. The following, supplementary rules are
   * applied:
   * <ol><li>If the last character, after stemming, is a hyphen, the
   * token is restemmed</ol><br />
   * @param token The token - a single word - to be stemmed
   * @return A string representing the stemmed token
   */
  public static String stemToken(String token) {
    Stemmer s = new Stemmer();
    String stemmed;

    s.add(token.toCharArray(), token.length());
    s.stem();
    stemmed = s.toString();

    // Re-stem the token if the last character is a hyphen
    if (stemmed.charAt(stemmed.length()-1) == '-')
        return stemToken(stemmed.replace("-",""));
    else
        return stemmed;
  }

  /**
   * Computes the IDF weight of the token
   * @param docs The number of documents in the index/corpus
   * @return The IDF weight
   */
  public double weightIdf(int docs) {
    if (this.postings.size() == 0) {
      System.err.printf("ERR: The token '%s' has no postings to compute IDF!\n",
        this.stem);
      System.exit(1);
    }

    return Math.log10((1.0*docs)/this.postings.size());
  }

  /**
   * Computes the TF weight of the token
   * @param doc The document to compute the TF weight for
   * @return The TF weight
   */
  public double weightTf(int doc) {
    int freq = this.getFreq(doc);
    if (freq == 0) { return 0; }
    else
      return 1 + Math.log10(freq);
  }

  /**
   * Returns the total frequency of the token
   * @return The token's total frequency throughout the corpus
   */
  public int getFreq() {
    int freq = 0;
    for(int key : Collections.list(postings.keys()))
      freq += postings.get(key).size();
    return freq;
  }

  /**
   * Returns the frequency of the token in a given document
   * @param doc The ID of the document
   * @return The document frequency of the token
   */
  public int getFreq(int doc) { return postings.get(doc).size(); }

  /**
   * Converts information about this token into a string; this string can then
   * be saved to a file or other location, and read back in as a token using
   * the read function.
   * 
   * Token data is stored as follows:
   * <TOK> [ <VAR> . <VAR> ] { <DOCS> [ <ID> <FRQ> <POST> . <GAP> ] [ . ] }
   * 
   * <TOK> is the token's stemmed version, with <VAR> being the variants. The
   * number of documents in its posting list is <DOCS>, with each document
   * containing the id of the document, <ID>, frequency, <FRQ>, and postings
   * in the form of gaps. (IDs are also stored in gap notation.)
   * 
   * @return The token as a string
   */
  @Override
  public String toString() {
    // prevDoc is the previous document that was written
    // prevPos is the previous posting that was written
    // result is the final string that contains the token's data
    // docList is a sorted list of the document IDs in the postings list
    int prevDoc = 0, prevPos = 0;
    String result = stem+" [ ";
    ArrayList<Integer> docList = Collections.list(postings.keys());
    Collections.sort(docList);
    
    // Token variants
    for(String variant : variants)
      result += variant + " ";

    result += "] " + postings.size() + " {";

    // For every document in the token's postings list, write down the list of
    // postings after the document id and frequency (as gaps)    
    for(int doc : docList) {
      prevPos = 0;

      result += " [ " + (doc-prevDoc) + " " + postings.get(doc).size() + " ";
      prevDoc = doc;
      for(int pos : postings.get(doc)) {
        result += (pos-prevPos) + " ";
        prevPos = pos;
      }
      result += "]";
    }

    result += " }";
    return result;
  }

  /**
   * Reads a token's data in from the string provided. The string must contain
   * valid data, or else errors will occur
   * @param tData The string containing the token's data to be loaded
   */
  public void read(String tData) {
    // data contains the String, split at spaces
    // pos is the current position in the array of data
    // doc is the current document being read in
    // post is the current position in the document being read in
    // L is the posting list currently being worked with/read in
    String[] data = tData.split(" ");
    int pos, doc=0, post;
    ArrayList<Integer> L;

    // Reset current token data
    stem = "";
    variants.clear();
    postings.clear();

    // Stem and variants
    stem = data[0];
    for(pos=2; !data[pos].equals("]"); pos++)
      variants.add(data[pos]);
    pos += 4;
    
    // Document postings lists
    for(int i=Integer.parseInt(data[pos-3]); i>0; i--) {
      post = 0;
      
      // Add the document to the postings list
      postings.put((doc += Integer.parseInt(data[pos])), new ArrayList<Integer>());
      L = postings.get(doc);

      // Read in each posting
      for(int k=Integer.parseInt(data[++pos]); k>0; k--)
        L.add((post += Integer.parseInt(data[++pos])));
      
      pos += 3;
    }
  }
}