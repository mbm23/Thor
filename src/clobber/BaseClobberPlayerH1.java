package clobber;
import game.*;

public abstract class BaseClobberPlayerH1 extends GamePlayer{
	public static int ROWS = ClobberState.ROWS;
	public static int COLS = ClobberState.COLS;
	public static final int MAX_SCORE = ROWS*COLS*100 +1;
	public static char HOME = ClobberState.homeSym;
	public static char AWAY = ClobberState.awaySym;
	
	public BaseClobberPlayerH1(String nickname, boolean isDeterministic) {
		super(nickname, new ClobberState(), isDeterministic);
	}
	public static boolean posOK(int r, int c)
	{ return Util.inrange(r, 0, ROWS-1) && Util.inrange(c, 0, COLS-1); }
	
	/*
	 * Counts number of your pieces that are isolated.  The higher the number the worse it is 
	 * for you.  However in eval board we pass the opponent so although it looks normal we are getting the 
	 * correct calculation to run minimax and alpha beta on
	 * Note you are passing in the opponent here so if away is past in 
	 */
	private static int countIsolated(ClobberState brd, int r, int c, char opp, boolean[][] visited){
		if( brd.board[r][c] == opp){
			return -1;
		}
		if(visited[r][c]){
			return 0;
		}
		int count = 1;
		visited[r][c] = true;
		for (int i = -1; i<2; i+=2){
			int tmpRow = r + i;
			int tmpCol = c + i;
			if(posOK(tmpRow,c)) {
				if (brd.board[tmpRow][c] == opp) {
					return -1;
				}
				else if (brd.board[tmpRow][c] == ClobberState.emptySym){
					
				}
				else {
					int neighborScore = countIsolated(brd, tmpRow, c, opp, visited);
					if (neighborScore == -1){
						return -1;
					}
					count += neighborScore;
				}
			}
			if(posOK(r,tmpCol)) {
				if (brd.board[r][tmpCol] == opp) {
					return -1;
				}
				else if (brd.board[r][tmpCol] == ClobberState.emptySym){
					
				}
				else {
					int neighborScore = countIsolated(brd, r, tmpCol, opp, visited);
					if (neighborScore == -1){
						return -1;
					}
					count += neighborScore;
				}
			}
		}
	
		return count;
	}
	
	private static int isolatedLoop(ClobberState brd, char opp, char player){
		boolean [][] visited = new boolean [ROWS][COLS];
		int isolated = 0;
		for(int i = 0; i < ROWS; i++) {
			for ( int j = 0; j < COLS; j++){
				if (!visited[i][j] && brd.board[i][j]==player){
					int temp = countIsolated(brd, i, j, opp, visited);
					if (temp > 0){
						isolated+=temp;
					}
				}
			}
		}
		return isolated;
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
