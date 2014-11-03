package clobber;
import game.*;

import java.util.*;


public class MiniMaxClobberPlayer extends BaseClobberPlayer {
	public final int MAX_DEPTH = 30;
	public int[] depthArray = new int[MAX_DEPTH];
	public int depthLimit;
	protected ScoredClobberMove [] mvStack;
	int movesMade = 0;
	
	protected class ScoredClobberMove extends ClobberMove {
		public double score;
		public ScoredClobberMove(ClobberMove c, double s) {
			super(c);
			score=s;
		}
		public ScoredClobberMove(double s) {
			super();
			score=s;
		}
		public void set(ClobberMove c, double s){
			row1 = c.row1;
			row2 = c.row2;
			col1 = c.col1;
			col2 = c.col2;
			score = s;
		}
	}
	
	public MiniMaxClobberPlayer(String name, int d){
		super(name, false);
		depthLimit = d;
		for (int i =0; i < MAX_DEPTH; i++) {
			int temp = 1;
			if (i > 8){
				temp = i-7;
			}
			depthArray[i] = temp; 
		}
	}
	
	protected static void shuffle (int [] ary) {
		int len = ary.length;
		for (int i = 0; i < len; i++){
			int spot = Util.randInt(i, len-1);
			int tmp = ary[i];
			ary[i] = ary[spot];
			ary[spot] = tmp;
		}
	}
	
	
	public MiniMaxClobberPlayer(String n) 
	{
		super(n, false);
		for (int i =0; i < MAX_DEPTH; i++) {
			depthArray[i] = ((int)i/4) * 4;
		}
	}
	
	public void init() {
		mvStack = new ScoredClobberMove [MAX_DEPTH];
		for (int i = 0; i < MAX_DEPTH; i++) {
			mvStack[i] = new ScoredClobberMove(0);
		}
	}
	
	protected boolean terminalValue(GameState brd, ScoredClobberMove mv){
		GameState.Status status = brd.getStatus();
		boolean isTerminal = true;
		if (status == GameState.Status.HOME_WIN) {
			mv.set( new ClobberMove(), MAX_SCORE);
		} else if ( status == GameState.Status.AWAY_WIN) {
			mv.set(new ClobberMove(), -MAX_SCORE);
		} else {
			isTerminal = false;
		}
		return isTerminal;
	}
	// The move is set to the score in the recusion of minimax
	public void performMove(ClobberState brd, ClobberMove mv, char Player, char OPP, int currDepth){
		ScoredClobberMove tempMv = new ScoredClobberMove(mv, 0);
		GameState.Who currTurn = brd.getWho();
		//Perform move and recurse
		brd.makeMove(tempMv);
		minimax(brd, currDepth + 1);
		
		//Undo
		brd.board[mv.row1][mv.col1] = Player;
		brd.board[mv.row2][mv.col2] = OPP;
		brd.status = GameState.Status.GAME_ON;
		brd.numMoves--;
		brd.who = currTurn;
		return;
	}
	
	public double tryMoves(ClobberState brd, int r, int c, char PLAYER, char OPP, int currDepth, double bestScore) {
		boolean toMaximize = (brd.getWho() == GameState.Who.HOME);
		ScoredClobberMove bestMove = mvStack[currDepth];
		ScoredClobberMove nextMove = mvStack[currDepth +1];
		if(posOK(r-1,c)){
			if (brd.board[r-1][c]==OPP){
				ClobberMove mv = new ClobberMove(r,c,r-1,c);
				performMove(brd, mv, PLAYER, OPP, currDepth);
				if(toMaximize && nextMove.score > bestMove.score) {
					bestMove.set(mv, nextMove.score);
					bestScore = nextMove.score;
				}
				else if (!toMaximize && nextMove.score < bestMove.score) {
					bestMove.set(mv, nextMove.score);
					bestScore = nextMove.score;
				}
			}
		}
		if(posOK(r+1,c)){
			if (brd.board[r+1][c]==OPP){
				ClobberMove mv = new ClobberMove(r,c,r+1,c);
				performMove(brd, mv, PLAYER, OPP, currDepth);
				if(toMaximize && nextMove.score > bestMove.score) {
					bestMove.set(mv, nextMove.score);
					bestScore = nextMove.score;
				}
				else if (!toMaximize && nextMove.score < bestMove.score) {
					bestMove.set(mv, nextMove.score);
					bestScore = nextMove.score;
				}
			}
		}
		if(posOK(r,c-1)){
			if (brd.board[r][c-1]==OPP){
				ClobberMove mv = new ClobberMove(r,c,r,c-1);
				performMove(brd, mv, PLAYER, OPP, currDepth);
				if(toMaximize && nextMove.score > bestMove.score) {
					bestMove.set(mv, nextMove.score);
					bestScore = nextMove.score;
				}
				else if (!toMaximize && nextMove.score < bestMove.score) {
					bestMove.set(mv, nextMove.score);
					bestScore = nextMove.score;
				}
			}
		}
		if(posOK(r,c+1)){
			if (brd.board[r][c+1]==OPP){
				ClobberMove mv = new ClobberMove(r,c,r,c+1);
				performMove(brd, mv, PLAYER, OPP, currDepth);
				if(toMaximize && nextMove.score > bestMove.score) {
					bestMove.set(mv, nextMove.score);
					bestScore = nextMove.score;
				}
				else if (!toMaximize && nextMove.score < bestMove.score) {
					bestMove.set(mv, nextMove.score);
					bestScore = nextMove.score;
				}
			}
		}
		return bestScore;
	}
	
	private double minimax (ClobberState brd, int currDepth) {
		boolean isTerminal = terminalValue(brd, mvStack[currDepth]);
		if (isTerminal){
			return 0;
		} else if (currDepth == depthArray[movesMade]) {
			mvStack[currDepth].set( new ClobberMove(0,0,0,1), evalBoard(brd));
			return evalBoard(brd);
		} else {
			GameState.Who currTurn = brd.getWho();
			char PLAYER = currTurn == GameState.Who.HOME ? HOME : AWAY;
			char OPP = currTurn == GameState.Who.HOME ? AWAY : HOME;
			double bestScore = (brd.getWho() == GameState.Who.HOME ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
			ScoredClobberMove bestMove = mvStack[currDepth];
			bestMove.set(new ClobberMove(0,0,0,1), bestScore);
			int [] rows = new int[ROWS];
			int [] cols = new int[COLS];
			for (int i = 0; i < ROWS; i++){
				rows[i] = i;
			}
			for (int i = 0; i < COLS; i++){
				cols[i] = i;
			}
			shuffle(rows);
			shuffle(cols);
			for (int i = 0; i < ROWS; i++) {
				int r = rows[i];
				for(int j = 0; j < COLS; j++){
					int c = cols[j];
					if(brd.board[r][c] == PLAYER){
						bestScore = tryMoves(brd, r, c, PLAYER, OPP, currDepth, bestScore);
					}
				}
			}
			
			return bestScore;
		}
		
	}
	public GameMove getMove(GameState state, String lastMove)
	{
		ClobberState board = (ClobberState)state;
		if(board.numMoves < 2){
			movesMade = 0;
		}
		double temp = minimax(board,0);
		System.out.println("bestScore: " + temp);
		movesMade +=2;
		return mvStack[0];
		
	}
	public static void main(String [] args)
	{
		int depth = 4;
		GamePlayer p = new MiniMaxClobberPlayer("MiniMax " + depth, depth);
		p.compete(args, 1);
	}
}
