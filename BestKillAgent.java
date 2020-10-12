package threeChess.agents;
import threeChess.*;
import java.util.Random;

/**
 * An interface for AI bots to implement.
 * They are simply given a Board object indicating the positions of all pieces, 
 * the history of the game and whose turn it is, and they respond with a move, 
 * expressed as a pair of positions.
 * 
 * BestKill agent chooses move based by scanning all possible moves
 * and selecting the one which takes the highest value opponent piece
 * or if none detected, a random move.
 * 
 * **/ 
public class BestKillAgent extends Agent{
  
  private static final String name = "BestK";
  private static final Random random = new Random();


  /**
   * A no argument constructor, 
   * required for tournament management.
   * **/
  public BestKillAgent(){
  }

  /**
   * Play a move in the game. 
   * The agent is given a Board Object representing the position of all pieces, 
   * the history of the game and whose turn it is. 
   * They respond with a move represented by a pair (two element array) of positions: 
   * the start and the end position of the move.
   * 
   * @param board The representation of the game state.
   * @return a two element array of Position objects, where the first element is the 
   * current position of the piece to be moved, and the second element is the 
   * position to move that piece to.
   * **/



/*
 * Checks all possible moves of the player and selects the one with
 * kills the highest ranking opponent
 * @return an array of positions i.e. start and finish
 */
  public Position[] playMove(Board board){
    Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
    Position start = pieces[0];
    Position end = pieces[0]; //dummy illegal move
    Position bestStart = pieces[0];
    Position bestEnd = pieces[0];
    int bestUtility = 0;

    boolean kill = false;
    int checkCount = 0;
    int utility = 0;
    
    // NEXT: update utility calculation so it factors in
    // cost of moving into a position that isn't safe
    // i.e. if in danger; utility = win - loss

    // cycle through my pieces
    for (int i=0; i<pieces.length; i++) {

        start = pieces[i]; // start represents current piece location
        Piece mover = board.getPiece(start);
        Direction[][] steps = mover.getType().getSteps();
        int maxReps = board.getPiece(start).getType().getStepReps();
        
        // for each piece play out each of their moves
        for (int j = 0; j<steps.length; j++) {
            utility = 0;
            mover = board.getPiece(start);
            Direction[] step = steps[j];         
            try {
                  // iterate through allowed number of reps of move
                  for (int m=1; m <= maxReps; m++){
                      end = board.step(mover, step, end);
                      // if a move that takes a piece is found
                      if (board.getPiece(end) != null && board.isLegalMove(start, end)) {
                        //check utility of piece taken
                        if (board.getPiece(end).getValue() > bestUtility) {
                            //store this best utility found
                            bestUtility = board.getPiece(end).getValue();
                            bestStart = start;
                            bestEnd = end;
                          }
                      // break after the playable move is encountered
                      break;
                      }
                    }
                  }
            catch(ImpossiblePositionException e){}
          }
      }
    
    // return move with the highest utility
    // or returns random move if no best found 
    if (bestUtility > 0 ) 
        return new Position[]{bestStart, bestEnd};
    else 
        return randomPlayMove(board);                           
  
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


  /*
   * backup random move
   */
  public Position[] randomPlayMove(Board board){
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
          end = board.step(mover, step, end);
      }catch(ImpossiblePositionException e){}
    }
    return new Position[] {start,end};
  }


}
