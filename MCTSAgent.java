package threeChess.agents;

import java.util.*;
import threeChess.*;

/**
 * An interface for AI bots to implement. They are simply given a Board object
 * indicating the positions of all pieces, the history of the game and whose
 * turn it is, and they respond with a move, expressed as a pair of positions.
 * 
 **/
public class MCTSAgent extends Agent {

    // FIELDS
    private String name = "MCTS";
    private double rate = 0.1;
    // public static long startTime = 0;
    // public static long endTime = 0;
    private boolean grudgeMode = false;
    private static final Random random = new Random();

    /**
     * A no argument constructor, required for tournament management.
     **/
    public MCTSAgent() {
    }

    /**
     * constructor for altering variables used in testing
     * 
     * @param rate
     * @param
     */
    public MCTSAgent(Double rate, boolean grudgeMode) {

        if (grudgeMode == true) {
            this.grudgeMode = true;
            name += "G";
        }
        if (rate != null) {
            this.rate = rate;
            name = name + (Double.toString(rate)).replace(".", "");
        }

    }

    /**
     * Play a move in the game using a Monte Carlo Tree Search.
     *
     * @param board The representation of the game state.
     * @return tow Position array representing a move
     * 
     **/
    public Position[] playMove(Board board) {

        // timing variables
        long timeLeft = board.getTimeLeft(board.getTurn());
        long timeA = System.currentTimeMillis();
        long timeB = System.currentTimeMillis();

        HashMap<Integer, moveNode> moveNodesHashMap = new HashMap<Integer, moveNode>();
        Position[] firstMove = new Position[2];
        Position[] move = new Position[2];

        int bestMove = -1000;
        double bestAverage = -1000;
        boolean keepSearching = true;

        while (keepSearching) {

            // late game policy
            if (board.getTimeLeft(board.getTurn()) < 1000)
                rate = 0.1;

            // search time policy
            if ((timeLeft * rate) < (timeB - timeA))
                keepSearching = false;

            // clone board and store agents colour
            Board boardClone = new Board(300);
            try {
                boardClone = (Board) board.clone();
            } catch (CloneNotSupportedException e) {
                System.out.println(e);
            }
            Colour myColour = boardClone.getTurn();
            int myOrdinal = myColour.ordinal();
            Colour[] colours = Colour.values();
            Colour victimColour = colours[(myOrdinal + 1) % colours.length];

            // select and store first move
            firstMove = playRandomMove(boardClone);
            try {
                boardClone.move(firstMove[0], firstMove[1], 1);
            } catch (ImpossiblePositionException e) {
                System.out.println(e);
            }

            // play out the rest of the game with random moves
            while (!boardClone.gameOver()) {
                move = playRandomMove(boardClone);
                try {
                    boardClone.move(move[0], move[1], 1);
                } catch (ImpossiblePositionException e) {
                    System.out.println(e);
                }
            }

            // store outcome of this game
            int[] outcome = { 0, 0, 0 };
            if (grudgeMode == true) {
                if (boardClone.getLoser() == victimColour && 
                    boardClone.getWinner() == myColour) {
                        outcome[boardClone.getWinner().ordinal()] = 1;
                }
                outcome[boardClone.getLoser().ordinal()] = -1;
            } else {
                outcome[boardClone.getWinner().ordinal()] = 1;
                outcome[boardClone.getLoser().ordinal()] = -1;
            }

            // convert move to Integer for use as HashMap key
            Integer thisMoveAsInt = moveToInteger(firstMove);

            // Add outcome to Hashmap
            // if this move has been played before
            if (moveNodesHashMap.containsKey(thisMoveAsInt)) {
                moveNode tempNode = new moveNode();
                // tempNode.gamesPlayed = moveNodes.get(thisMoveAsInt).gamesPlayed;
                tempNode.copy(moveNodesHashMap.get(thisMoveAsInt));
                tempNode.gamesWon += outcome[myColour.ordinal()];
                tempNode.gamesPlayed += 1;
                moveNodesHashMap.put(thisMoveAsInt, tempNode);
            }
            // if this move has NOT been played before
            else if (!moveNodesHashMap.containsKey(thisMoveAsInt)) {
                moveNode tempNode = new moveNode();
                tempNode.start = firstMove[0];
                tempNode.end = firstMove[1];
                tempNode.gamesWon += outcome[myColour.ordinal()];
                tempNode.gamesPlayed += 1;
                moveNodesHashMap.put(thisMoveAsInt, tempNode);
            }
            // neither! shouldn't reach this
            else {
                System.out.print("ERROR");
            }

            // update if best move
            int played = moveNodesHashMap.get(thisMoveAsInt).gamesPlayed;
            int won = moveNodesHashMap.get(thisMoveAsInt).gamesWon;
            if (bestAverage <= (won / played)) {
                bestAverage = (won / played);
                bestMove = thisMoveAsInt;
            }
            timeB = System.currentTimeMillis();
        }
        return new Position[] { moveNodesHashMap.get(bestMove).start, moveNodesHashMap.get(bestMove).end };
    }

    // SUPPLEMENTARY STRUCTURE

    /**
     * class to store data for hashmap values
     */
    public class moveNode {

        // FIELDS
        Position start;
        Position end;
        int gamesPlayed;
        int gamesWon;

        // CONSTRUCTOR
        public moveNode() {
            Board board = new Board(300);
            Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
            this.start = pieces[0];
            this.end = pieces[0];
            this.gamesPlayed = 0;
            this.gamesWon = 0;
        }

        // METHODS
        public void copy(moveNode moveNode) {
            this.gamesPlayed = moveNode.gamesPlayed;
            this.gamesWon = moveNode.gamesWon;
            this.start = moveNode.start;
            this.end = moveNode.end;
        }
    }

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
