package threeChess.agents;

import java.io.*;
import java.text.BreakIterator;
import java.util.*;
import threeChess.*;

/**
 * An interface for AI bots to implement.
 * They are simply given a Board object indicating the positions of all pieces, 
 * the history of the game and whose turn it is, and they respond with a move, 
 * expressed as a pair of positions.
 * **/ 
public class MCRandAgent extends Agent{
  
    private static final String name = "MCRand";
    private static final Random random = new Random();

    /**
     * A no argument constructor, 
     * required for tournament management.
     * **/
    public MCRandAgent(){
    }

    /**
     * @return the Agent's name, for annotating game description.
     * **/ 
    public String toString(){return name;}

    /**
     * Displays the final board position to the agent, 
     * if required for learning purposes. 
     * Other a default implementation may be given.
     * @param finalBoard the end position of the board
     * **/
    public void finalBoard(Board finalBoard){}

    /**
     * plays a random move
     * @param board
     * @return
     */
    public static Position[] playRand(Board board){
        Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
        Position start = pieces[0];
        Position end = pieces[0]; //dummy illegal move
        while (!board.isLegalMove(start, end)){
        start = pieces[random.nextInt(pieces.length)];
        Piece mover = board.getPiece(start);
        Direction[][] steps = mover.getType().getSteps();
        Direction[] step = steps[random.nextInt(steps.length)];
        int reps = 1 + random.nextInt(mover.getType().getStepReps());
        end = start;
        try{
            for(int i = 0; i<reps; i++)
            end = board.step(mover, step, end, start.getColour()!=end.getColour());
        }catch(ImpossiblePositionException e){}
        }
        return new Position[] {start,end};
    }

    /**
     * Converts an move into an Integer
     * for use as a key in a hashtable
     * @param move
     * @return
     */
    public static Integer movToInt(Position[] move){
        int value = 0;
        Position posOne = move[0];
        Position posTwo = move[1];
        switch (posOne.getColour()) {
            case BLUE:  value += 100000;
                break;
            case GREEN: value += 200000;
                break;
            case RED:   value += 300000;
                break;
        }
        value += (posOne.getRow() * 10000);
        value += (posOne.getColumn() * 1000);
        switch (posTwo.getColour()) {
            case BLUE:  value += 100;
                break;
            case GREEN: value += 200;
                break;
            case RED:   value += 300;
                break;
        }
        value += (posTwo.getRow() * 10);
        value += (posTwo.getColumn() * 1);
        return value;
    }

    /**
     * class to store nextMovestats
     * try to store a node in the hashmap to fix the null problem
     * 
     * presently this isnt working with the test class
     */
    public class MoveStats extends MCRandAgent{

        int moveCode;
        Position start;
        Position end;
        int gamesPlayed = 0;
        int gamesWon = 0;
        double utility = 0;

        public void updateUtility(){
            this.utility = gamesPlayed/gamesWon;
        }

        public MoveStats(){
        }

        public MoveStats(int moveCode){
            this.moveCode = moveCode;
            Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
            this.start = pieces[0];
            this.end = pieces[0];
        }

    }


    /**
     * Play a move in the game using a Monte Carlo Tree Search. 
     * The agent is given a Board Object representing the position of all pieces, 
     * the history of the game and whose turn it is. 
     * They respond with a move represented by a pair (two element array) of positions: 
     * the start and the end position of the move.
     * @param board The representation of the game state.
     * @return a two element array of Position objects, where the first element is the 
     * current position of the piece to be moved, and the second element is the 
     * position to move that piece to.
     * 
     * This agent performs a Monte Carlo Tree Search informed by random moves
     * Start with one random playout per piece... see how it goes
     * 
     * **/
    public Position[] playMove(Board board){

        // a calculation can be done to adjust time left
        int timeLeft = board.getTimeLeft(board.getTurn());

        int simCount = 100;
        HashMap<Integer, int[]> results = new HashMap<Integer, int[]>();
        HashMap<Integer, Position[]> moves = new HashMap<Integer, Position[]>();
        Position[] firstMove = new Position[2];
        Position[] move = new Position[2];

        // main loop for simulating move playouts
        for (int i = 0; i<simCount; i++) {

            // clone board and store agents colour
            Board boardClone = new Board(300);
            try { 
                boardClone = (Board) board.clone();
            }catch(CloneNotSupportedException e){}
            Colour myColour = boardClone.getTurn();

            // select and store first move
            firstMove = playRand(boardClone);
            try{ boardClone.move(firstMove[0], firstMove[1], 1); }
            catch(ImpossiblePositionException e){System.out.println(e);}

            // play out the rest of the game with random moves
            while(!boardClone.gameOver()) {
                move = playRand(boardClone);
                try{ boardClone.move(move[0], move[1], 1); }
                catch(ImpossiblePositionException e){System.out.println(e);}
            }
            //store outcome of this game
            int[] outcome = {0,0,0};
            outcome[boardClone.getWinner().ordinal()] = 1;
            outcome[boardClone.getLoser().ordinal()] = -1;

            // convert move to hashable Integer
            Integer thisMoveAsInt = movToInt(firstMove);

            // add firstmove and outcome to results hashmap
            // if this move has been played before
            if (results.containsKey(thisMoveAsInt)){
                //contains++;
                int[] tempResultStats = (results.get(thisMoveAsInt));
                tempResultStats[0] += outcome[myColour.ordinal()];  // update my games won
                tempResultStats[1] += 1;                            // update my games played
                // store in hashmaps
                results.put(thisMoveAsInt, tempResultStats);
                moves.put(thisMoveAsInt, firstMove);
            } 
            // if this move has NOT been played before
            else if (!results.containsKey(thisMoveAsInt)) {
                //notContains++;
                int[] newResultStats = {0, 0};
                newResultStats[0] += outcome[myColour.ordinal()];   // update my games won
                newResultStats[1] += 1;                             // update my games played
                // store in hashmaps
                results.put(thisMoveAsInt, newResultStats);
                moves.put(thisMoveAsInt, firstMove);
            }
            // neither! shouldn't reach this
            else {System.out.print("ERROR");}
        }

        // search results for best move
        int bestMove = -1000;
        double bestAverage = -1000;
        for (int i : results.keySet()) {
            int[] temp = new int[2];
            temp = results.get(i).clone();
            if (bestAverage <= (temp[0]/temp[1])){ 
                bestAverage = (temp[0]/temp[1]);
                bestMove = i;
            }
        }
    
        return new Position[] {moves.get(bestMove)[0],moves.get(bestMove)[1]};
    
    }

}


