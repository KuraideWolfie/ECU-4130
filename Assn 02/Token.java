/*
 * Assignment 02
 * File:    Token.java
 * Date:    26 January 2018
 * Author:  Matthew Morgan
 * Description:
 * Token contains two classes - Token and TokenIndexDocument - which are used
 * when generating the positional index for a corpus. Token contains functions
 * for accessing the token's frequency and index, and TokenIndexDocument contains
 * functions for checking information about the token it belongs to inside of
 * the document represented by the TID.
 */

 // Import statements
import java.util.ArrayList;
import java.util.Hashtable;

/*
 * Token represents a single, stemmed word to be put into a TokenDictionary.
 * A token is classified by the total number of occurrences in a corpus, and
 * also contains a collection of documents in which it occurs.
 */

class Token {
    // A string representing the token itself, a hashtable of TIDocuments to
    // store the positional listings for documents (hashed by document ID),
    // and the total number of times this word occurs in the corpus
    private String token;
    private Hashtable<Integer,TokenIndexDocument> index;
    private int ttlFreq;

    // Constructor
    public Token(String name) {
        token = name;
        index = new Hashtable<>();
        ttlFreq = 0;
    }

    // Accessors
    public String getToken() { return token; }
    public Hashtable<Integer,TokenIndexDocument> getIndex() { return index; }
    public int getFreq() { return ttlFreq; }

    /**
     * Gets, and returns, a document with the specified ID in the index
     * @param docID The numerical ID of the document to fetch
     * @return The TokenIndexDocment object representing the document ID
     */
    public TokenIndexDocument getDoc(int docID) { return index.get(docID); }

    /**
     * Returns whether or not the token occurs in the document with the given ID
     * @param docID The numerical ID of the document to check the token's occurrence within
     * @return True if the token occurs in the document, or false if not
     */
    public boolean isInDoc(int docID) { return index.containsKey(docID); }

    /**
     * Returns whether, or not, the token occurs at a given location within a document
     * @param docID The numerical ID of the document to check occurrence within
     * @param loc The specific location to check for the token at
     * @return True if the token exists in the document at the location, or false if not
     */
    public boolean isInDocatLoc(int docID, int loc) {
        TokenIndexDocument doc = index.get(docID);
        if (doc != null) { return doc.isAtLocation(loc); } else { return false; }
    }

    // Mutators
    public void setToken(String t) { token = t; }
    public void clrIndex() { index = new Hashtable<>(); ttlFreq = 0; }

    /**
     * Adds a document to the index with the specified ID
     * @param docID The numerical ID of the new document
     */
    public void addDoc(int docID) {
        if (index.putIfAbsent(docID, new TokenIndexDocument(docID)) != null) {
            System.err.printf("SYS: Error on document add: A document with ID '%d' already exists in the index!\n", docID);

            System.exit(1);
        }
    }

    /**
     * Remove a document from the index with the specified ID
     * @param docID The numerical ID of the document to remove
     */
    public void remDoc(int docID) {
        TokenIndexDocument doc = index.get(docID);
        if (doc != null) {
            if (doc.getFreq() > 0)
                ttlFreq -= doc.getFreq();
            index.remove(docID);
            return;
        }
        // Error : The document specified isn't in the index
        System.err.printf("SYS: Error on document removal: Document '%d' cannot be found!\n", docID);
        System.exit(1);
    }

    /**
     * Adds a location to a document in the index
     * @param docID The numerical ID of the document to append the location to
     * @param loc The location to add to the document
     */
    public void addDocLocation(int docID, int loc) {
        TokenIndexDocument doc = index.get(docID);
        if (doc != null) {
            doc.addLocation(loc); ttlFreq++; return;
        }
        // Error: the document to add the location to doesn't exist in the index
        System.err.printf("SYS: Error on document location add: A document with ID '%d' doesn't exist in the index!\n", docID);
        System.exit(1);
    }

    /**
     * Stems the token provided, as a string
     * @param token The token to be stemmed
     * @return The token, as a string, after stemming
     */
    public static String stemToken(String token) {
        Stemmer stem = new Stemmer();
        stem.add(token.toCharArray(), token.length());
        stem.stem();
        return stem.toString();
    }
}

/******************************************************************************/

/*
 * TokenIndexDocument represents a single document in a collection of documents that
 * a token occurs within. It contains the frequency of the token in the document as
 * well as a list of locations of the token in the document.
 */

class TokenIndexDocument {
    // Frequency of the token in this document, the list of positions of the
    // token in the document, and the document's ID
    private int docFreq, docID;
    private ArrayList<Integer> locations;

    // Constructor
    public TokenIndexDocument(int id) {
        locations = new ArrayList<>();
        docFreq = 0;
        docID = id;
    }

    // Accessors
    public int getFreq() { return docFreq; }
    public boolean isAtLocation(int ind) { return locations.contains(ind); }
    public ArrayList<Integer> getLocations() { return locations; }
    public int getID() { return docID; }

    // Mutators
    public void clrLocations() { locations = new ArrayList<>(); docFreq = 0; }
    public void addLocation(int loc) { this.addLocationInOrder(loc); docFreq++; }
    public void setID(int newID) { docID = newID; }

    /**
     * Adds a location to the location index for this token in this document
     * @param loc The location where the token was found
     */
    private void addLocationInOrder(int loc) {
        for(int i=0; i<locations.size(); i++)
            if (locations.get(i) > loc) {
                locations.add(i-1, loc);
                return;
            }
        locations.add(loc);
    }
}