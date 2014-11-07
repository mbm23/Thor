package clobber;
import game.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AlphaBetaClobberPlayerThreaded extends BaseClobberPlayer implements Runnable {
	public final int MAX_DEPTH = 30;
	public int[] depthArray = new int[MAX_DEPTH];
	public double[][] bestScore = new double[ROWS][COLS];
	protected ScoredClobberMove[][] bestMoves = new ScoredClobberMove[ROWS][COLS];
	public int depthLimit;
	ArrayList<Coords> orderedMoves = new ArrayList<Coords>(30);
	// protected ScoredClobberMove [] mvStack;
	int movesMade = 0;
	ClobberState board;
	double currBest;
	int moveIndex = 0;
	boolean topToMax;
	ScoredClobberMove currBestMove;
	
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
	
	public AlphaBetaClobberPlayerThreaded(String name, int d){
		super(name, false);
		depthLimit = d;
		for (int i =0; i < MAX_DEPTH; i++) {
			int temp = 4;
			if (i > 4){
				temp = (i-1)*(i-1);
			}
			depthArray[i] = temp; 
		}
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
		for (int i = 0; i < ROWS; i++){
			for (int j = 0; j < COLS; j++){
				Coords temp = new Coords(rows[i],cols[j]);
				orderedMoves.add(temp);
			}
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
	
	
	public AlphaBetaClobberPlayerThreaded(String n) 
	{
		super(n, false);
		for (int i =0; i < MAX_DEPTH; i++) {
			depthArray[i] = ((int)i/4) * 4;
		}
	}
	
	public void init() {
		/*mvStack = new ScoredClobberMove [MAX_DEPTH];
		for (int i = 0; i < MAX_DEPTH; i++) {
			mvStack[i] = new ScoredClobberMove(0);
		}*/
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
	public void performMove(ClobberState brd, ClobberMove mv, char Player, char OPP, int currDepth, double alpha, double beta, ScoredClobberMove[] mvStack){
		ScoredClobberMove tempMv = new ScoredClobberMove(mv, 0);
		GameState.Who currTurn = brd.getWho();
		//Perform move and recurse
		brd.makeMove(tempMv);
		alphaBeta(brd, currDepth + 1, alpha, beta, mvStack);
		
		//Undo
		brd.board[mv.row1][mv.col1] = Player;
		brd.board[mv.row2][mv.col2] = OPP;
		brd.status = GameState.Status.GAME_ON;
		brd.numMoves--;
		brd.who = currTurn;
		return;
	}
	
	private double alphaBeta (ClobberState brd, int currDepth, double alpha, double beta, ScoredClobberMove[] mvStack) {
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
			boolean toMaximize = (brd.getWho() == GameState.Who.HOME);
			if (topToMax){
				alpha = Math.max(alpha, currBest);
			}
			else{
				beta = Math.min(beta, currBest);
			}
			ScoredClobberMove bestMove = mvStack[currDepth];
			ScoredClobberMove nextMove = mvStack[currDepth +1];
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
					if (topToMax){
						alpha = Math.max(alpha, currBest);
					}
					else{
						beta = Math.min(beta, currBest);
					}
					int c = cols[j];
					if(brd.board[r][c] == PLAYER){
						if(posOK(r-1,c)){
							if (brd.board[r-1][c]==OPP){
								ClobberMove mv = new ClobberMove(r,c,r-1,c);
								performMove(brd, mv, PLAYER, OPP, currDepth, alpha, beta, mvStack);
								if(toMaximize && nextMove.score > bestMove.score) {
									bestMove.set(mv, nextMove.score);
									bestScore = nextMove.score;
								}
								else if (!toMaximize && nextMove.score < bestMove.score) {
									bestMove.set(mv, nextMove.score);
									bestScore = nextMove.score;
								}
								if(toMaximize) {
									alpha = Math.max(bestMove.score, alpha);
									if (bestMove.score >= beta || bestMove.score == MAX_SCORE) {
										return bestScore;
									}
								}
								else {
									beta = Math.min(bestMove.score,  beta);
									if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
										return bestScore;
									}
								}
							}
						}
						if(posOK(r+1,c)){
							if (brd.board[r+1][c]==OPP){
								ClobberMove mv = new ClobberMove(r,c,r+1,c);
								performMove(brd, mv, PLAYER, OPP, currDepth, alpha, beta, mvStack);
								if(toMaximize && nextMove.score > bestMove.score) {
									bestMove.set(mv, nextMove.score);
									bestScore = nextMove.score;
								}
								else if (!toMaximize && nextMove.score < bestMove.score) {
									bestMove.set(mv, nextMove.score);
									bestScore = nextMove.score;
								}
								if(toMaximize) {
									alpha = Math.max(bestMove.score, alpha);
									if (bestMove.score >= beta || bestMove.score ==MAX_SCORE) {
										return bestScore;
									}
								}
								else {
									beta = Math.min(bestMove.score,  beta);
									if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
										return bestScore;
									}
								}
							}
						}
						if(posOK(r,c-1)){
							if (brd.board[r][c-1]==OPP){
								ClobberMove mv = new ClobberMove(r,c,r,c-1);
								performMove(brd, mv, PLAYER, OPP, currDepth, alpha, beta, mvStack);
								if(toMaximize && nextMove.score > bestMove.score) {
									bestMove.set(mv, nextMove.score);
									bestScore = nextMove.score;
								}
								else if (!toMaximize && nextMove.score < bestMove.score) {
									bestMove.set(mv, nextMove.score);
									bestScore = nextMove.score;
								}
								if(toMaximize) {
									alpha = Math.max(bestMove.score, alpha);
									if (bestMove.score >= beta || bestMove.score ==MAX_SCORE) {
										return bestScore;
									}
								}
								else {
									beta = Math.min(bestMove.score,  beta);
									if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
										return bestScore;
									}
								}
							}
						}
						if(posOK(r,c+1)){
							if (brd.board[r][c+1]==OPP){
								ClobberMove mv = new ClobberMove(r,c,r,c+1);
								performMove(brd, mv, PLAYER, OPP, currDepth, alpha, beta, mvStack);
								if(toMaximize && nextMove.score > bestMove.score) {
									bestMove.set(mv, nextMove.score);
									bestScore = nextMove.score;
								}
								else if (!toMaximize && nextMove.score < bestMove.score) {
									bestMove.set(mv, nextMove.score);
									bestScore = nextMove.score;
								}
							}
							if(toMaximize) {
								alpha = Math.max(bestMove.score, alpha);
								if (bestMove.score >= beta || bestMove.score ==MAX_SCORE) {
									return bestScore;
								}
							}
							else {
								beta = Math.min(bestMove.score,  beta);
								if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
									return bestScore;
								}
							}
						}
					}
				}
			}
			
			return bestScore;
		}
		
	}
	public GameMove getMove(GameState state, String lastMove)
	{
		moveIndex = 0;
		ExecutorService executor = Executors.newFixedThreadPool(4);
		board = (ClobberState)state;
		System.out.println(board);
		long startTime = System.nanoTime();
		if(board.numMoves < 2){
			movesMade = 0;
		}
		/*int [] rows = new int[ROWS];
		int [] cols = new int[COLS];
		for (int i = 0; i < ROWS; i++){
			rows[i] = i;
		}
		for (int i = 0; i < COLS; i++){
			cols[i] = i;
		}
		shuffle(rows);
		shuffle(cols);*/
		currBest = MAX_SCORE + 100;
		boolean toMaximize = (board.getWho() == GameState.Who.HOME);
		topToMax = toMaximize;
		if (toMaximize){
			currBest = -MAX_SCORE -100;
		}
		
		for (int i = 0; i < COLS; i++) {
			//int c = cols[i];
			//Thread[] threads = new Thread[6];
			for(int j = 0; j < ROWS; j++){
				//int r = rows[j];
				//String threadName = "" + r + "," + c;
				String threadName = "Thor Hammer";
				Thread thrd = new Thread(this, threadName);
				executor.execute(thrd);
			}
			/*for(int j = 0; j < threads.length; j++){
				/*try {
					threads[j].join();
				} catch (InterruptedException e) {
					System.out.println("thread errors :(");
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int r = rows[j];
				/*if (toMaximize) {
					synchronized(this){
						if (bestScore[r][c] > currBest){
							currBest = bestScore[r][c];
							currBestMove = bestMoves[r][c];
						}
					}
				} else {
					synchronized(this){
						if (bestScore[r][c] < currBest){
							currBest = bestScore[r][c];
							currBestMove = bestMoves[r][c];
						}
					}
				}
			}*/
		}
		executor.shutdown();
        while (!executor.isTerminated()) {
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		long endTime = System.nanoTime();
		System.out.println("bestScore: " + currBest);
		long time = (endTime-startTime)/10000000;
		System.out.println("movesMade: " + movesMade + "Turn time: " + time);
		movesMade +=2;
		System.out.println(currBestMove);
		System.out.println(board);
		for (int i = 0; i < COLS; i++) {
			for(int j = 0; j < ROWS; j++){
				orderedMoves.set(j*COLS+i, new Coords(j,i,bestMoves[j][i].score));
			}
		}
		Collections.sort(orderedMoves);
		if (toMaximize){
			Collections.reverse(orderedMoves);
		}
		
		return currBestMove;
		
	}
	public static void main(String [] args)
	{
		int depth = 4;
		GamePlayer p = new AlphaBetaClobberPlayerThreaded("AlphaThread " + depth, depth);
		p.compete(args, 1);
	}

	@Override
	public void run() {
		int moveNum;
		synchronized(this){
			moveNum = moveIndex;
			moveIndex++;
		}
		ClobberState threadBoard = (ClobberState) board.clone();
		/*Thread t = Thread.currentThread();
	    String name = t.getName();
	    String[] pieces = name.split(",");
	    int row = Integer.parseInt(pieces[0]);
	    int col = Integer.parseInt(pieces[1]);*/
		int row = orderedMoves.get(moveNum).row;
		int col = orderedMoves.get(moveNum).col;
	    ScoredClobberMove[] thrdMvStack = new ScoredClobberMove [MAX_DEPTH];
		for (int i = 0; i < MAX_DEPTH; i++) {
			thrdMvStack[i] = new ScoredClobberMove(0);
		}
	    boolean toMaximize = (board.getWho() == GameState.Who.HOME);
	    double temp;
	    if (toMaximize){
	    	temp = topAlphaBeta(threadBoard,0, currBest, 10000000, row, col, thrdMvStack);
	    }
	    else{
	    	temp = topAlphaBeta(threadBoard,0, -10000000, currBest, row, col, thrdMvStack);
	    }
		bestScore[row][col]=temp;
		bestMoves[row][col]= thrdMvStack[0];
		if (toMaximize) {
			synchronized(this){
				if (bestScore[row][col] > currBest){
					currBest = bestScore[row][col];
					currBestMove = bestMoves[row][col];
				}
			}
		} else {
			synchronized(this){
				if (bestScore[row][col] < currBest){
					currBest = bestScore[row][col];
					currBestMove = bestMoves[row][col];
				}
			}
		}
	}
	private double topAlphaBeta (ClobberState brd, int currDepth, double alpha, double beta, int r, int c, ScoredClobberMove[] mvStack) {
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
			boolean toMaximize = (brd.getWho() == GameState.Who.HOME);
			ScoredClobberMove bestMove = mvStack[currDepth];
			ScoredClobberMove nextMove = mvStack[currDepth +1];
			bestMove.set(new ClobberMove(0,0,0,1), bestScore);
			boolean indic = brd.board[r][c] == PLAYER;
			System.out.println(r+ "," + c + " : " + indic);
			if(brd.board[r][c] == PLAYER){
				if(posOK(r-1,c)){
					if (brd.board[r-1][c]==OPP){
						ClobberMove mv = new ClobberMove(r,c,r-1,c);
						performMove(brd, mv, PLAYER, OPP, currDepth, alpha, beta, mvStack);
						if(toMaximize && nextMove.score > bestMove.score) {
							bestMove.set(mv, nextMove.score);
							bestScore = nextMove.score;
						}
						else if (!toMaximize && nextMove.score < bestMove.score) {
							bestMove.set(mv, nextMove.score);
							bestScore = nextMove.score;
						}
						if(toMaximize) {
							alpha = Math.max(bestMove.score, alpha);
							if (bestMove.score >= beta || bestMove.score == MAX_SCORE) {
								return bestScore;
							}
						}
						else {
							beta = Math.min(bestMove.score,  beta);
							if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
								return bestScore;
							}
						}
					}
				}
				if(posOK(r+1,c)){
					if (brd.board[r+1][c]==OPP){
						ClobberMove mv = new ClobberMove(r,c,r+1,c);
						performMove(brd, mv, PLAYER, OPP, currDepth, alpha, beta, mvStack);
						if(toMaximize && nextMove.score > bestMove.score) {
							bestMove.set(mv, nextMove.score);
							bestScore = nextMove.score;
						}
						else if (!toMaximize && nextMove.score < bestMove.score) {
							bestMove.set(mv, nextMove.score);
							bestScore = nextMove.score;
						}
						if(toMaximize) {
							alpha = Math.max(bestMove.score, alpha);
							if (bestMove.score >= beta || bestMove.score ==MAX_SCORE) {
								return bestScore;
							}
						}
						else {
							beta = Math.min(bestMove.score,  beta);
							if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
								return bestScore;
							}
						}
					}
				}
				if(posOK(r,c-1)){
					if (brd.board[r][c-1]==OPP){
						ClobberMove mv = new ClobberMove(r,c,r,c-1);
						performMove(brd, mv, PLAYER, OPP, currDepth, alpha, beta, mvStack);
						if(toMaximize && nextMove.score > bestMove.score) {
							bestMove.set(mv, nextMove.score);
							bestScore = nextMove.score;
						}
						else if (!toMaximize && nextMove.score < bestMove.score) {
							bestMove.set(mv, nextMove.score);
							bestScore = nextMove.score;
						}
						if(toMaximize) {
							alpha = Math.max(bestMove.score, alpha);
							if (bestMove.score >= beta || bestMove.score ==MAX_SCORE) {
								return bestScore;
							}
						}
						else {
							beta = Math.min(bestMove.score,  beta);
							if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
								return bestScore;
							}
						}
					}
				}
				if(posOK(r,c+1)){
					if (brd.board[r][c+1]==OPP){
						ClobberMove mv = new ClobberMove(r,c,r,c+1);
						performMove(brd, mv, PLAYER, OPP, currDepth, alpha, beta, mvStack);
						if(toMaximize && nextMove.score > bestMove.score) {
							bestMove.set(mv, nextMove.score);
							bestScore = nextMove.score;
						}
						else if (!toMaximize && nextMove.score < bestMove.score) {
							bestMove.set(mv, nextMove.score);
							bestScore = nextMove.score;
						}
					}
					if(toMaximize) {
						alpha = Math.max(bestMove.score, alpha);
						if (bestMove.score >= beta || bestMove.score ==MAX_SCORE) {
							return bestScore;
						}
					}
					else {
						beta = Math.min(bestMove.score,  beta);
						if (bestMove.score <= alpha || bestMove.score == -MAX_SCORE) {
							return bestScore;
						}
					}
				}
			}
			return bestScore;
		}
	}
}
