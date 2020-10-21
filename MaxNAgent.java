package threeChess.agents;

import java.util.*;
import threeChess.*;

/**
 * An AI interface that implements the MaxN algorithm for an agent to use in
 * threeChess
 * 
 **/
public class MaxNAgent extends Agent {

    private static String name = "MaxN";
    private int moveLimit = 2;
    private Colour myColour;
    private boolean grudgeMode = false;
    private static final Random random = new Random();
    private int earlyMoveLimit = 15;
    private int lateTimeLimit = 1000;

    /**
     * A no argument constructor, required for tournament management.
     **/
    public MaxNAgent() {
    }

    /**
     * Constructor for altering variables used in testing
     * 
     * @param grudgeMode true turns on one player focus
     **/
    public MaxNAgent(Integer earlyMoveLimit, boolean grudgeMode) {
        if (grudgeMode == true) {
            this.grudgeMode = true;
            name += "G";
        }
        if (earlyMoveLimit != null) {
            this.earlyMoveLimit = earlyMoveLimit;
            name = name + Integer.toString(earlyMoveLimit);
        }

    }

    /**
     * Play a move in the game using a MaxN algorithm
     * 
     * @param board The representation of the game state.
     * @return Position array representing suggested move
     **/
    public Position[] playMove(Board board) {

        // early game strategy
        // if (board.getMoveCount() < earlyMoveLimit) {
        //     moveLimit = 1;
        //     //System.out.println("MaxN: early game strategy active");
        // }

        // // late game strategy
        if (board.getTimeLeft(board.getTurn()) < lateTimeLimit) {
            moveLimit = 1;
            //System.out.println("MaxN: late game strategy active");
        }

        // System.out.println((board.getMoveCount()) + "/" + earlyMoveLimit);

        int bestMoveInt = 0;
        int[] initialUtility;
        if (grudgeMode == true)
            initialUtility = Arrays.copyOf(myGrudgeUtility(board), 3);
        else
            initialUtility = Arrays.copyOf(myPiecesUtility(board), 3);
        int[] bestUtility = { -100, -100, -100 };
        myColour = board.getTurn();

        // collect possible moves
        HashMap<Integer, Position[]> nextMoves = getNextMoves(board);
        // cycle through next moves
        for (Integer moveInt : nextMoves.keySet()) {
            // clone board
            Board boardClone = new Board(300);
            try {
                boardClone = (Board) board.clone();
            } catch (CloneNotSupportedException e) {
                System.out.println(e);
            }

            // ERROR DEBUGGING:
            // check if game is over
            if (boardClone.gameOver() == true) continue;

            // try this move
            try {
                boardClone.move(nextMoves.get(moveInt)[0], nextMoves.get(moveInt)[1], 1);
            } catch (ImpossiblePositionException e) {
                System.out.println(e);
            }
            // call maxN and initiate recursion
            int[] utility = Arrays.copyOf(maxN(boardClone, 0), 3);
            // assess for best utililty
            if (utility[myColour.ordinal()] > bestUtility[myColour.ordinal()]) {
                bestUtility = Arrays.copyOf(utility, 3);
                bestMoveInt = moveInt;
            }
        }
        // deploy alternate strategy if no utility improvement
        if (bestUtility[myColour.ordinal()] == initialUtility[myColour.ordinal()]) {
            // System.out.println("MaxN no best found");
            if (grudgeMode == true) {
                return playGrudgeMove(board);
            } else {
                return playBestMove(board);
            }
        }
        // return best outcome move
        return new Position[] { nextMoves.get(bestMoveInt)[0], nextMoves.get(bestMoveInt)[1] };
    }

    public int[] maxN(Board board, int moveCount) {

        int[] bestUtility = { -100, -100, -100 };
        moveCount++;
        // if limit reached return utility[] of board
        if (moveCount <= moveLimit) {
            // collect possible moves
            HashMap<Integer, Position[]> nextMoves = getNextMoves(board);
            // recursively call all valid moves
            for (Position[] move : nextMoves.values()) {
                // clone board
                Board boardClone = new Board(300);
                try {
                    boardClone = (Board) board.clone();
                } catch (CloneNotSupportedException e) {
                }
                // try this move
                try {
                    boardClone.move(move[0], move[1], 1);
                } catch (ImpossiblePositionException e) {
                    System.out.println(e);
                }
                int[] utility = { -1, -1, -1 };
                
                // recursive call if game is not over
                if (boardClone.gameOver() == false) {
                    utility = maxN(boardClone, moveCount);
                }
                
                // determine best utility for return
                if (utility[board.getTurn().ordinal()] > bestUtility[board.getTurn().ordinal()]) {
                    bestUtility = Arrays.copyOf(utility, 3);
                }
            }

        }
        // check if limit has been reached
        if (grudgeMode == true && (board.getTurn() == myColour))
            bestUtility = Arrays.copyOf(myGrudgeUtility(board), 3);
        else
            bestUtility = Arrays.copyOf(myPiecesUtility(board), 3);

        return bestUtility;
    }

    // SUPPLEMENTARY METHODS

    /**
     * returns a hashmap of next moves all potential legal unique next moves values
     * based on board
     * 
     * @param board the board to be assessed
     * @return a hashmap of moves
     */
    public static HashMap<Integer, Position[]> getNextMoves(Board board) {
        
        HashMap<Integer, Position[]> nextMovesHashmap = new HashMap<Integer, Position[]>();
        Position[] myPositions = board.getPositions(board.getTurn()).toArray(new Position[0]);
        Position start = myPositions[0];
        Position end = myPositions[0]; // dummy illegal move

        // cycle through my pieces
        for (int i = 0; i < myPositions.length; i++) {
            start = myPositions[i]; // start represents current piece location
            Piece mover = board.getPiece(start);
            Direction[][] dirCount = mover.getType().getSteps();
            int maxReps = board.getPiece(start).getType().getStepReps();
            // iterate through the allowed directions
            for (int dirIndex = 0; dirIndex < dirCount.length; dirIndex++) {
                Direction[] step = dirCount[dirIndex];
                // create cloneboard for move
                Board boardClone = new Board(300);
                try {
                    boardClone = (Board) board.clone();
                } catch (CloneNotSupportedException e) {
                }
                // iterate through allowed number of reps of move
                for (int actions = 1; actions <= maxReps; actions++) {
                    // try to make the number of steps
                    try {
                        end = boardClone.step(mover, step, start);
                    } catch (ImpossiblePositionException e) {
                    }
                    Integer thisCode = moveToInteger(new Position[] { start, end });
                    // if a unique move that takes a piece is found
                    if (board.isLegalMove(start, end) && !nextMovesHashmap.containsKey(thisCode)
                            && ((start.getRow() != end.getRow()) || (start.getColumn() != end.getColumn()))) {
                        // store move in hashmap
                        nextMovesHashmap.put(thisCode, new Position[] { start, end });
                    }
                }
            }
        }
        return nextMovesHashmap;
    }

    /**
     * Converts an move into an Integer for use as a key in a hashtable
     * 
     * @param move the move to be converted
     * @return an Integer representation of the move
     */
    public static Integer moveToInteger(Position[] move) {
        int value = 0;
        Position posOne = move[0];
        Position posTwo = move[1];
        switch (posOne.getColour()) {
            case BLUE:
                value += 100000;
                break;
            case GREEN:
                value += 200000;
                break;
            case RED:
                value += 300000;
                break;
        }
        value += (posOne.getRow() * 10000);
        value += (posOne.getColumn() * 1000);
        switch (posTwo.getColour()) {
            case BLUE:
                value += 100;
                break;
            case GREEN:
                value += 200;
                break;
            case RED:
                value += 300;
                break;
        }
        value += (posTwo.getRow() * 10);
        value += (posTwo.getColumn() * 1);
        return value;
    }

    // METHODS FOR SIMPLE ALTERNATE MOVE STRATEGIES

    /**
     * Checks all possible moves of the player and selects the one with kills the
     * highest ranking piece of the next player to take a turn or if none possible,
     * a random move
     * 
     * @param board board to be assessed
     * @return an array of positions representting a move
     */
    public Position[] playGrudgeMove(Board board) {
        Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
        // for victim
        Position bestStart = pieces[0];
        Position bestEnd = pieces[0];
        int bestUtility = 0;
        // for other
        Position bestOtherStart = pieces[0];
        Position bestOtherEnd = pieces[0];
        int bestOtherUtility = 0;
        // choose victim
        Colour myColour = board.getTurn();
        int myOrdinal = myColour.ordinal();
        Colour[] colours = Colour.values();
        Colour victimColour = colours[(myOrdinal + 1) % colours.length];

        HashMap<Integer, Position[]> nextMoves = getNextMoves(board);
        // search for highest utility move
        for (Position[] move : nextMoves.values()) {
            if (board.getPiece(move[1]) != null) {
                int pieceValue = board.getPiece(move[1]).getValue();
                if (board.getPiece(move[1]).getColour() == victimColour) {
                    if (pieceValue > bestUtility) {
                        bestUtility = pieceValue;
                        bestStart = move[0];
                        bestEnd = move[1];
                    }
                }
                // takable piece belongs to other player
                else if (pieceValue > bestOtherUtility) {
                    bestOtherUtility = pieceValue;
                    bestOtherStart = move[0];
                    bestOtherEnd = move[1];
                }
            }
        }
        // return move with the highest utility or if none random
        if (bestUtility > 0)
            return new Position[] { bestStart, bestEnd };
        else if (bestOtherUtility > 0)
            return new Position[] { bestOtherStart, bestOtherEnd };
        else
            return playRandomMove(board);
    }

    /**
     * Checks all possible moves of the player and selects the move which takes the
     * highest possible value piece from any opponent or a legal random move if no
     * opponent piece can be taken
     * 
     * @param board the board to be analysed
     * @return the positions of the move to be played
     */
    public Position[] playBestMove(Board board) {

        int bestUtility = 0;
        HashMap<Integer, Position[]> nextMoves = AgentUtilities.getNextMoves(board);
        Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
        Position start = pieces[0];
        Position end = pieces[0];
        for (Position[] move : nextMoves.values()) {
            if (board.getPiece(move[1]) != null) {
                if (board.getPiece(move[1]).getValue() > bestUtility) {
                    bestUtility = board.getPiece(move[1]).getValue();
                    start = move[0];
                    end = move[1];
                }
            }
        }
        if (bestUtility > 0)
            return new Position[] { start, end };
        else
            return playRandomMove(board);
    }

    /**
     * Play a random move in the game.
     * 
     * @param board The representation of the game state.
     * @return a move to be played
     **/
    public Position[] playRandomMove(Board board) {
        Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
        Position start = pieces[0];
        Position end = pieces[0]; // dummy illegal move
        while (!board.isLegalMove(start, end)) {
            start = pieces[random.nextInt(pieces.length)];
            Piece mover = board.getPiece(start);
            Direction[][] steps = mover.getType().getSteps();
            Direction[] step = steps[random.nextInt(steps.length)];
            int reps = 1 + random.nextInt(mover.getType().getStepReps());
            end = start;
            try {
                for (int i = 0; i < reps; i++)
                    end = board.step(mover, step, end, start.getColour() != end.getColour());
            } catch (ImpossiblePositionException e) {
            }
        }
        return new Position[] { start, end };
    }

    // METHODS FOR UTILITY CALCULATION

    /**
     * Calculates utility for board based on addition for pieces captured from
     * others and subtraction for pieces captured by others
     * 
     * @param board to be assesed
     * @return array of utility for each player
     */
    public static int[] myPiecesUtility(Board board) {
        int[] utility = { 0, 0, 0 };
        Colour[] colours = Colour.values();
        for (Colour player : colours) {
            for (Piece piece : board.getCaptured(player)) {
                utility[player.ordinal()] += piece.getValue();
                utility[piece.getColour().ordinal()] -= piece.getValue();
            }
        }
        return utility;
    }

    /**
     * Calculates utility of board based on addition for pieces captured from victim
     * subtraction for pieces captured by others
     * 
     * @param board to be assesed
     * @return array of utility for each player
     */
    public static int[] myGrudgeUtility(Board board) {
        int[] utility = { 0, 0, 0 };
        Colour[] colours = Colour.values();
        Colour myColour = board.getTurn();
        int myOrdinal = myColour.ordinal();
        Colour victimColour = colours[(myOrdinal + 1) % colours.length];
        for (Colour player : colours) {
            for (Piece piece : board.getCaptured(player)) {
                if (player == myColour) {
                    if (piece.getColour() == victimColour) {
                        utility[player.ordinal()] += piece.getValue();
                    }
                } else
                    utility[player.ordinal()] += piece.getValue();
                utility[piece.getColour().ordinal()] -= piece.getValue();
            }
        }
        return utility;
    }

    // METHODS REQUIRED FOR TOURNAMENT

    /**
     * @return the Agent's name, for annotating game description.
     **/
    public String toString() {
        return name;
    }

    /**
     * Displays the final board position to the agent, if required for learning
     * purposes. Other a default implementation may be given.
     * 
     * @param finalBoard the end position of the board
     **/
    public void finalBoard(Board finalBoard) {
    }

}
