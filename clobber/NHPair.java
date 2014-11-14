package clobber;
/*
 * Essentially just a struct
 */
public class NHPair {
	public int count;
	public int sum;
	public int oppCount;
	public boolean sign; // true is positive (isolated) false is negative(group)
	public NHPair(){
		count=0;
		sign=true;
		sum=0;
		oppCount = 0;
	}

}
