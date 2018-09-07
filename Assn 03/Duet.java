/**
 * File:    Duet.java
 * Author:  Matthew Morgan
 * Date:    28 March 2018
 * Version: 1.0
 * Description:
 * Duet is a simple data structure that stores two pieces of data.
 * 
 * ~~~ VERSION HISTORY ~~~
 * Version 1.0 (28 March 2018)
 */

public class Duet<A,B> {
  public A dataA;
  public B dataB;
  public Duet(A a, B b) { dataA = a; dataB = b; }
  public Duet() { dataA = null; dataB = null; }
}