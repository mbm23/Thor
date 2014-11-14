package clobber;

public class NHPair {
	public int count;
	public int sum;
	public float factor;
	public boolean sign; // true is positive (isolated) false is negative(group)
	public NHPair(){
		count=0;
		sign=true;
		sum=0;
		factor = 1;
	}

}
