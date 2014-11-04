package clobber;
import game.*;

public abstract class BaseClobberPlayerH2 extends GamePlayer{
	public static int ROWS = ClobberState.ROWS;
	public static int COLS = ClobberState.COLS;
	public static final int MAX_SCORE = ROWS*COLS*5 +1;
	public static char HOME = ClobberState.homeSym;
	public static char AWAY = ClobberState.awaySym;
	public int [][] weights;
	
	
	public BaseClobberPlayerH2(String nickname, boolean isDeterministic) {
		super(nickname, new ClobberState(), isDeterministic);
		weights = new int[ROWS][COLS];
		for (int i = 0; i < ROWS; i++){
			for (int j = 0; j < COLS; j++){
				int temp = Math.min(Math.min(Math.min(i+1, j+1), ROWS - i), COLS-j);
				weights[i][j]=temp;
			}
		}
	}
	public static boolean posOK(int r, int c)
	{ return Util.inrange(r, 0, ROWS-1) && Util.inrange(c, 0, COLS-1); }
	
	/*
	 * Counts number of your pieces that are isolated.  The higher the number the worse it is 
	 * for you.  However in eval board we pass the opponent so although it looks normal we are getting the 
	 * correct calculation to run minimax and alpha beta on
	 * Note you are passing in the opponent here so if away is past in 
	 */
	private static Pair countIsolated(ClobberState brd, int r, int c, char opp, boolean[][] visited){
		/*
		if( brd.board[r][c] == opp){
			System.out.print("!!!!!!!");
			return -1;
		}*/
		Pair p = new Pair();
		if(visited[r][c]){
			return p;
		}
		p.count = 1;
		visited[r][c] = true;
		for (int i = -1; i<2; i+=2){
			int tmpRow = r + i;
			int tmpCol = c + i;
			if(posOK(tmpRow,c)) {
				if (brd.board[tmpRow][c] == opp) {
					p.sign = false;
				}
				else if (brd.board[tmpRow][c] == ClobberState.emptySym){
					
				}
				else {
					Pair neighborPair = countIsolated(brd, tmpRow, c, opp, visited);
					if (!neighborPair.sign){
						p.sign = false;
					}
					p.count += neighborPair.count;
				}
			}
			if(posOK(r,tmpCol)) {
				if (brd.board[r][tmpCol] == opp) {
					p.sign = false;
				}
				else if (brd.board[r][tmpCol] == ClobberState.emptySym){
					
				}
				else {
					Pair neighborPair = countIsolated(brd, r, tmpCol, opp, visited);
					if (!neighborPair.sign){
						p.sign = false;
					}
					p.count += neighborPair.count;
				}
			}
		}
	
		return p;
	}
	
	private static int isolatedLoop(ClobberState brd, char opp, char player){
		boolean [][] visited = new boolean [ROWS][COLS];
		int isolated = 0;
		int largestGroup = 0;
		int secondGroup = 0;
		for(int i = 0; i < ROWS; i++) {
			for ( int j = 0; j < COLS; j++){
				if (!visited[i][j] && brd.board[i][j]==player){
					Pair temp = countIsolated(brd, i, j, opp, visited);
					if (temp.sign){
						isolated+=temp.count;
					}
					else{
						if (temp.count > largestGroup) {
							secondGroup = largestGroup;
							largestGroup = temp.count;
						}
						else if (temp.count > secondGroup){
							secondGroup = temp.count;
						}
						
					}
				}
			}
		}
		return 5*isolated - 3*largestGroup - 2*secondGroup; 
	}
	/*
	 * Returns score of board
	 * Note there is a bit of a double negative situation going on here
	 * We are passing the opponent so it looks like they are in the wrong order
	 * 
	 * The heuristic however is counting a bad characteristic so you want it in reverse
	 * order
	 */
	public static int evalBoard(ClobberState brd){
		int tempHome = isolatedLoop(brd,HOME, AWAY) ;
				int away = isolatedLoop(brd,AWAY, HOME);
		return tempHome - away;
		
	}
}
