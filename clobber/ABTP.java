package clobber;
import game.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;


public class ABTP extends BaseClobberPlayerTest implements Runnable {
	public final int MAX_DEPTH = 30;
	public int[] depthArray = new int[MAX_DEPTH];
	public double[][] bestScore = new double[ROWS][COLS];
	public ArrayList<Coords> playableSpots = new ArrayList<Coords>(30);
	protected ScoredClobberMove[][] bestMoves = new ScoredClobberMove[ROWS][COLS];
	boolean fixed = true;
	public int depthLimit;
	ArrayList<Coords> orderedMoves = new ArrayList<Coords>(30);
	// protected ScoredClobberMove [] mvStack;
	int movesMade = 0;
	int fixedDepth = 6;
	ClobberState board;
	double currBest;
	int moveIndex = 0;
	boolean topToMax;
	double timeRemaining = 300;
	ScoredClobberMove currBestMove;
	private GameFrame frame;
	private ClobberMove move = new ClobberMove(0,0,0,0);
	
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
	/*How layers deep to go at different durns*/
	
	public ABTP(String name, int d){
		super(name, false);
		depthLimit = d;
		for (int i =0; i < MAX_DEPTH; i++) {
			int temp = 6;
			if (i > 3){
				temp = (5)*(5);
			}
			depthArray[i] = temp; 
		}
		frame = new GameFrame(nickname, new ClobberCanvas());
		frame.setVisible(true);
		gameState = new ClobberState();
		
	}
	/*Called at the beginning of each game*/
	public void resetOrderedMoves(){
		orderedMoves = new ArrayList<Coords>(30);
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
				playableSpots.add(temp);
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
	
	/*
	public AlphaBetaClobberPlayerThreaded(String n) 
	{
		super(n, false);
		for (int i =0; i < MAX_DEPTH; i++) {
			depthArray[i] = ((int)i/4) * 4;
		}
	}
	*/
	
	
	/*With Opening book*/
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
		if(Thread.interrupted()){
			Thread.currentThread().stop();
			System.out.println("Manual Stop");
		}
		boolean isTerminal = terminalValue(brd, mvStack[currDepth]);
		if (isTerminal){
			return 0;
		} else if (currDepth == (fixed ? fixedDepth : depthArray[movesMade])) {
			double tempScore = evalBoard(brd);
			mvStack[currDepth].set( new ClobberMove(0,0,0,1), tempScore);
			return tempScore;
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
			/*int [] rows = new int[ROWS];
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
				int r = rows[i];*/
				for(int j = 0; j < orderedMoves.size(); j++){
					int index = j;
					if(toMaximize != topToMax){
						index = orderedMoves.size() -1 - j;
					}
					
					if (topToMax){
						alpha = Math.max(alpha, currBest);
					}
					else{
						beta = Math.min(beta, currBest);
					}
					Coords toTest = orderedMoves.get(index);
					int r = toTest.row;
					int c = toTest.col;
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
			return bestScore;
		}
		
	}
	/* Method called to get the move - removes extra moves*/
	public GameMove getMove(GameState state, String lastMove)
	{
		char ch = side == GameState.Who.HOME ? ClobberState.homeSym : ClobberState.awaySym;
		frame.setTitle("My move (" + ch + ")");
		if (!lastMove.equals("--") && frame.canvas.move != null) {
			((ClobberMove)frame.canvas.move).parseMove(lastMove);
		}

		boolean OK;

		frame.canvas.setBoard(state);
		frame.canvas.repaint();
		frame.canvas.getMove(move, state, this);
		board = (ClobberState)state;
		for(int i = 0; i < orderedMoves.size(); i++){
			if(board.board[orderedMoves.get(i).row][orderedMoves.get(i).col] == board.emptySym){
				orderedMoves.remove(i);
			}
		}
		if(board.numMoves < 2){
			movesMade = board.numMoves;
			resetOrderedMoves();
			timeRemaining = 300;
		}
		double maxTime = Math.min(210, Math.max(2,timeRemaining*.75 - 10));
		maxTime =5;
		ScoredClobberMove tempBestMove = getInitMove(state, lastMove);
		/*Lets Threads no what move place to start at in move ordering*/
		moveIndex = 0;
		ExecutorService executor = Executors.newFixedThreadPool(3);
		//system.out.println(board);
		long startTime = System.nanoTime();
		
		/* Clean up Move OrderList*/
		
		currBest = MAX_SCORE + 100;
		boolean toMaximize = (board.getWho() == GameState.Who.HOME);
		topToMax = toMaximize;
		if (toMaximize){
			currBest = -MAX_SCORE -100;
		}
		//system.out.println("currBest start: " + currBest);
			for(int j = 0; j < orderedMoves.size(); j++){
				String threadName = "Thor Hammer";
				Thread thrd = new Thread(this, threadName);
				executor.execute(thrd);
			}
		executor.shutdown();
		boolean print = true;
        while (!executor.isTerminated()) {
        	try {
        		long currTime = System.nanoTime();
        		double timeTaken = (currTime-startTime)/1000000000.0;
        		if (timeTaken > maxTime){
        			executor.shutdownNow();
        			if(print){
	        			System.out.println("Move Calc over time");
	        			System.out.println(timeTaken);
	        			print = false;
        			}
        		}
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		long endTime = System.nanoTime();
		boolean useOrig = false;
		if(toMaximize){
			if(tempBestMove.score > currBestMove.score){
				useOrig = true;
			}
		} else {
			if(tempBestMove.score < currBestMove.score){
				useOrig = true;
			}
		}
		if(useOrig){
			currBest = tempBestMove.score;
			currBestMove = tempBestMove;
		} else{
			for(int i = 0; i < orderedMoves.size(); i++){
				Coords temp = orderedMoves.get(i);
				temp.score = bestMoves[temp.row][temp.col].score;
				orderedMoves.set(i, temp);
			}
			Collections.sort(orderedMoves);
			if (toMaximize){
				Collections.reverse(orderedMoves);
			}
		}
		System.out.println("bestScore: " + currBestMove.score);
		double time = (endTime-startTime)/1000000000.0;
		timeRemaining -= time;
		//system.out.println("movesMade: " + movesMade + "Turn time: " + time);
		movesMade +=2;
		//system.out.println(currBestMove);
		//system.out.println(board);
		
		state.makeMove(currBestMove);
		frame.canvas.repaint();
		frame.setTitle("Waiting");
		return currBestMove;
	}
	/* Method called to get the move - removes extra moves*/
	public ScoredClobberMove getInitMove(GameState state, String lastMove)
	{
		
		/*Lets Threads no what move place to start at in move ordering*/
		moveIndex = 0;
		ExecutorService executor = Executors.newFixedThreadPool(3);
		fixed = true;
		currBest = MAX_SCORE + 100;
		boolean toMaximize = (board.getWho() == GameState.Who.HOME);
		topToMax = toMaximize;
		if (toMaximize){
			currBest = -MAX_SCORE -100;
		}
		//system.out.println("currBest start: " + currBest);
			for(int j = 0; j < orderedMoves.size(); j++){
				String threadName = "Thor Hammer";
				Thread thrd = new Thread(this, threadName);
				executor.execute(thrd);
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
		for(int i = 0; i < orderedMoves.size(); i++){
				Coords temp = orderedMoves.get(i);
				temp.score = bestMoves[temp.row][temp.col].score;
				orderedMoves.set(i, temp);
		}
		Collections.sort(orderedMoves);
		if (toMaximize){
			Collections.reverse(orderedMoves);
		}
		fixed = false;
		return currBestMove;
	}
	
	public static void main(String [] args)
	{
		int depth = 4;
		GamePlayer p = new ABTP("Picture " + depth, depth);
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
	    //system.out.println(temp);
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
			double tempScore = evalBoard(brd);
			mvStack[currDepth].set( new ClobberMove(0,0,0,1), tempScore);
			return tempScore;
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
			//system.out.println(r+ "," + c + " : " + indic);
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
