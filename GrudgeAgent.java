package threeChess.agents;

import threeChess.*;
import java.util.Random;

/**
 * An interface for AI bots to implement. They are simply given a Board object
 * indicating the positions of all pieces, the history of the game and whose
 * turn it is, and they respond with a move, expressed as a pair of positions.
 * 
 * BestKill agent chooses move based by scanning all possible moves and
 * selecting the one which takes the highest value opponent piece or if none
 * detected, a random move.
 * 
 **/
public class GrudgeAgent extends Agent {

    private static final String name = "Grudge";
    private static final Random random = new Random();

    /**
     * A no argument constructor, required for tournament management.
     **/
    public GrudgeAgent() {
    }

    /*
     * Checks all possible moves of the player and selects the one with kills the
     * highest ranking piece of the next player to take a turn or if none possible,
     * a random move
     * 
     * @return an array of positions i.e. start and finish
     */
    public Position[] playMove(Board board) {
        Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
        Position start = pieces[0];
        Position end = pieces[0]; // dummy illegal move
        Position bestStart = pieces[0];
        Position bestEnd = pieces[0];
        int bestUtility = 0;
        Position bestBackUpStart = pieces[0];
        Position bestBackUpEnd = pieces[0];
        int bestBackUpUtility = 0;

        // choose victim
        Colour myColour = board.getTurn();
        int myOrdinal = myColour.ordinal();
        Colour[] colours = Colour.values();
        Colour victimColour = colours[(myOrdinal + 1) % colours.length];

        boolean kill = false;
        int checkCount = 0;
        int utility = 0;

        // NEXT: update utility calculation so it factors in
        // cost of moving into a position that isn't safe
        // i.e. if in danger; utility = win - loss

        // cycle through my pieces
        for (int i = 0; i < pieces.length; i++) {

            start = pieces[i]; // start represents current piece location
            Piece mover = board.getPiece(start);
            Direction[][] steps = mover.getType().getSteps();
            int maxReps = board.getPiece(start).getType().getStepReps();

            // for each piece play out each of their moves
            for (int j = 0; j < steps.length; j++) {
                utility = 0;
                mover = board.getPiece(start);
                Direction[] step = steps[j];
                try {
                    // iterate through allowed number of reps of move
                    for (int m = 1; m <= maxReps; m++) {
                        end = board.step(mover, step, end);
                        // if a move that takes a piece is found
                        if (board.getPiece(end) != null && board.isLegalMove(start, end)) {
                            // check utility of piece taken
                            if (board.getPiece(end).getValue() > bestUtility) {
                                // check colour of piece taken for victimhood
                                if (victimColour.equals(board.getPiece(end).getColour())) {
                                    // store this best utility found
                                    bestUtility = board.getPiece(end).getValue();
                                    bestStart = start;
                                    bestEnd = end;
                                }
                                // store backup kill if no victim piece can be taken
                                else {
                                    bestBackUpUtility = board.getPiece(end).getValue();
                                    bestBackUpStart = start;
                                    bestBackUpEnd = end;
                                }
                            }
                            // break after the playable move is encountered
                            break;
                        }
                    }
                } catch (ImpossiblePositionException e) {
                }
            }
        }

        // return move with the highest utility
        // or returns random move if no best found
        if (bestUtility > 0)
            return new Position[] { bestStart, bestEnd };
        else if (bestBackUpUtility > 0)
            return new Position[] { bestBackUpStart, bestBackUpEnd };
        else
            return randomPlayMove(board);
    }

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

    /*
     * backup random move
     */
    public Position[] randomPlayMove(Board board) {
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
                    end = board.step(mover, step, end);
            } catch (ImpossiblePositionException e) {
            }
        }
        return new Position[] { start, end };
    }

}

class GrudgeAgent {

}
