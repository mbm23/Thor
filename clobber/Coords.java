package clobber;

public class Coords implements Comparable{
	public int row;
	public int col;
	double score;
	public Coords(int r, int c){
		row = r;
		col = c;
		score = 0;
	}
	public Coords(int r, int c, double s){
		row = r;
		col = c;
		score = s;
	}
	
	public int compareTo(Object obj){
		if(!(obj instanceof Coords)){
			System.out.println("Expected Coords objects");
			return 0;
		}
		else {
			double otherScore = ((Coords) obj).score;
			int diff = (int) ( score - otherScore);
			return diff;
		}
		
		
	}
}
