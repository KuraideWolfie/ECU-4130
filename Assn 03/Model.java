/**
 * File:    Model.java
 * Author:  Matthew Morgan
 * Date:    28 March 2018
 * Version: 2.0
 * Description:
 * Model is a representation of a vector space model. Every document vector in
 * the model is represented by a Duet that contains two values:
 * - a boolean specifying whether the vector has been normalized
 * - a hashtable of components, where tokens are keys and weights are doubles
 * 
 * The format of a vector space model file is as follows:
 * -----------------------------------------------------------------------------
 * <DOC CNT>
 * <ID> <NORMALIZED> <COMP CNT> <COMP> <WEIGHT> <COMP> <WEIGHT> ...
 * ...
 * -----------------------------------------------------------------------------
 * 
 * ~~~ VERSION HISTORY ~~~
 * Version 1.0 (22 February 2018)
 * Version 2.0 (28 March 2018)
 * - Complete revamp of the vsm system - merged DocumentVector.java and
 *   VectorSpaceModel.java into a single file: Model.java
 * - Vectors are no longer represented by a class, but by a Hashtable of doubles
 *   and a boolean for normalization status
 * 
 * ~~~ BUCKETLIST ~~~
 * - Perform overwrite confirmation if VSM files already exist
 */

// Import statements
import java.util.Hashtable;
import java.util.Collections;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public class Model {
  // model is the vsm; each Duet represents a single document vector
  private Hashtable<Integer,Duet<Boolean,Hashtable<String,Double>>> model;

  public Model() { model = new Hashtable<>(); }

  /**
   * Returns the size - that is, the number of document vectors - in the model
   * @return The number of documents stored in the vsm
   */
  public int size() { return model.size(); }

  /**
   * Attempts to retrieve a document vector from the model. If it doesn't exist,
   * then an error is printed to the screen
   * @param id The ID of the document to fetch from the model
   * @return The document vector corresponding to the ID given, if it exists
   */
  public Hashtable<String,Double> getDoc(int id) {
    if (!model.containsKey(id)) {
      System.err.println("ERR: The vsm doesn't contain a vector for doc "+id);
      System.exit(1);
    }

    return model.get(id).dataB;
  }

  /**
   * Gets the value of a component, in a document vector, in the model. An error
   * is displayed if the document vector for 'id' doesn't have the component
   * in its list of values
   * @param id The ID of the document that should contain the component
   * @param component The component to get the value of
   */
  public double getDocComponent(int id, String component) {
    Double val = getDoc(id).get(component);
    
    if (val == null) {
      System.err.printf("ERR: The vsm doesn't contain component '%s' for the "+
        "doc '%d'\n", component, id);
      System.exit(1);
    }

    return val.doubleValue();
  }

  /**
   * Sets a component of the specified document vector to the value given, or
   * removes it if the value to set it to is 0
   * @param id The ID of the document that contains the component
   * @param component The component - that is, the token - to set the value of
   * @param value The value to set the component as
   */
  public void setDocComponent(int id, String component, double value) {
    if (value == 0.0)
      getDoc(id).remove(component);
    else
      getDoc(id).put(component, value);
  }

  /**
   * Adds a document vector to the vsm if it doesn't already exist. If it does,
   * an error message is printed
   * @param id The ID of the document, as an integer, to be added to the vsm
   * @return A reference to the document vector stored
   */
  public Duet<Boolean,Hashtable<String,Double>> addDoc(int id) {
    if (model.containsKey(id)) {
      System.err.println("ERR: The vsm already has a vector for doc "+id);
      System.exit(1);
    }

    model.put(id, new Duet<>(false, new Hashtable<>()));
    return model.get(id);
  }

  /**
   * Removes a document vector from the vsm
   * @param id The ID of the document vector to be removed
   */
  public void delDoc(int id) { model.remove(id); }

  /** Normalizes any document vectors in the vsm that haven't been already */
  public void normalize() {
    for(Duet<Boolean,Hashtable<String,Double>> duet : Collections.list(model.elements())) {
      if (!duet.dataA) {
        // Flag that this vector has been normalized
        duet.dataA = true;
        normalize(duet.dataB);
      }
    }
  }

  /**
   * Prints a document vector to the screen contained within the current model
   * @param id The ID of the document to print the vector of
   * @param cpl The number of components to print, per line
   */
  public void printVector(int id, int cpl) {
    Hashtable<String,Double> vector = getDoc(id);

    System.out.printf("Vector %d: %d components, %s\n", id, vector.size(),
      (model.get(id).dataA ? "normalized" : "not normalized"));

    printVector(vector, cpl);
  }

  /**
   * Attempts to load a prior-saved vector space model from the disk
   * @param loc The filename that should contain the vector space model
   */
  public void load(String loc) {
    try {
      BufferedReader r = new BufferedReader(new FileReader(verify(loc, true)));

      // For every document contained in the vector space model...
      for(int i=Integer.parseInt(r.readLine()); i>0; i--) {
        String[] line = r.readLine().split(" ");
        Duet<Boolean,Hashtable<String,Double>> doc =
          this.addDoc(Integer.parseInt(line[0]));

        // Normalization status
        doc.dataA = Boolean.parseBoolean(line[1]);
        
        // For every component in the document vector, add the component back to
        // the document vector
        for(int k=Integer.parseInt(line[2])-1; k>=0; k--)
          doc.dataB.put(line[3+(2*k)], Double.parseDouble(line[4+(2*k)]));
      }

      r.close();
    }
    catch(IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Attempts to save the vector space model to the disk in the file specified
   * by 'loc,' or throws an error if one occurs.
   * @param loc The location of the file to save the vsm to
   */
  public void save(String loc) {
    try {
      BufferedWriter w = new BufferedWriter(new FileWriter(verify(loc, false)));

      // Number of document vectors
      w.write(model.size()+"\n");

      // For every document vector in the model, write its data
      for(int key : Collections.list(model.keys())) {
        Duet<Boolean,Hashtable<String,Double>> doc = model.get(key);

        // Vector ID, normalization status, and component count
        w.write(key+" "+doc.dataA+" "+doc.dataB.size()+" ");

        // For every component in the vector, write the component and its weight
        for(String docKey : Collections.list(doc.dataB.keys()))
          w.write(docKey+" "+doc.dataB.get(docKey)+" ");
        
        w.write("\n");
      }

      w.close();
    }
    catch(IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Verifies that the location specified for loading/saving a vsm from is
   * valid. If invalid, an error is printed and the program exits
   * @param loc The location of the file
   * @param isLoad Whether the program is loading (true) or saving (false) a vsm
   */
  private File verify(String loc, boolean isLoad) {
    File f = new File(loc);

    if ((!f.exists() && isLoad) || f.isDirectory()) {
      System.err.printf("ERR: Cannot %s vsm %s file %s\n",
        (isLoad?"load":"save"), (isLoad?"from":"to"), loc);
      System.exit(1);
    }

    return f;
  }

  /**
   * Computes the cosine similarity of a document vector with a query vector.
   * Both of these vectors should be normalized prior to this computation
   * @param doc The document vector to compare with the query
   * @param query The query vector to compare against the document
   * @return The similarity between the two vectors
   */
  public static double cosineSim(Hashtable<String,Double> doc,
    Hashtable<String,Double> query) {
    double res = 0.0;
    for(String c : Collections.list(query.keys()))
      res += (doc.containsKey(c) ? doc.get(c) : 0.0) * query.get(c);
    return res;
  }

  /** Normalizes a specific document vector by computing the euclidean length
   *  @param vector The vector to be normalized */
  public static void normalize(Hashtable<String,Double> vector) {
    // Compute the Euclidean length for the vector
    double euc = 0.0;
    for(double w : Collections.list(vector.elements())) { euc += w*w; }
    euc = Math.sqrt(euc);

    // Normalize the vector components, removing any components that end up 0.0
    for(String key : Collections.list(vector.keys())) {
      vector.put(key, vector.get(key)/euc);
      if (vector.get(key) == 0.0) { vector.remove(key); }
    }
  }

  /**
   * Prints any given document vector to the screen.
   * @param vector The document vector to print the components of
   * @param cpl The number of components, per line
   */
  public static void printVector(Hashtable<String,Double> vector, int cpl) {
    int cplCur = 0;

    for(String key : Collections.list(vector.keys())) {
      System.out.printf("%20s : %-10f ", key, vector.get(key));
      cplCur++;
      if (cplCur == cpl) { System.out.print("\n"); cplCur = 0; }
    }

    // Print a linebreak if one hasn't already been printed for the line
    if (cplCur < cpl) { System.out.println(); }
  }
  public static void printVector(Hashtable<String,Double> vector) {
    printVector(vector, 2);
  }

}