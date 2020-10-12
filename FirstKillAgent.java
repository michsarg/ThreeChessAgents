package threeChess.agents;
import threeChess.*;
import java.util.Random;

/**
 * An interface for AI bots to implement.
 * They are simply given a Board object indicating the positions of all pieces, 
 * the history of the game and whose turn it is, and they respond with a move, 
 * expressed as a pair of positions.
 * 
 * FirstKillAgent agent chooses move based on first detected move that results in loss
 * of a piece to another player (later implement patient killer)
 * 
 * **/ 
public class FirstKillAgent extends Agent{
  
  private static final String name = "FirstK";
  private static final Random random = new Random();


  /**
   * A no argument constructor, 
   * required for tournament management.
   * **/
  public FirstKillAgent(){
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


  // must check all legal moves for a kill piece by piece
  // now it just does a random direction and exits loop after 17 iterations
  // do like this: randomly select a piece, check all its moves for a legal one
  // if no legal moves result in kill, add piece to checked list



  public Position[] playMove(Board board){
    Position[] pieces = board.getPositions(board.getTurn()).toArray(new Position[0]);
    Position start = pieces[0];
    Position end = pieces[0]; //dummy illegal move
    boolean kill = false;
    int checkCount = 0;

    while (!board.isLegalMove(start, end) && kill==false){                         /// while start->end is not a legal move
      start = pieces[random.nextInt(pieces.length)];                /// selects random piece
      Piece mover = board.getPiece(start);                          /// mover = start piece
      Direction[][] steps = mover.getType().getSteps();             /// create 2d array of legal moves
      Direction[] step = steps[random.nextInt(steps.length)];       /// select one random move
      int reps = 1 + random.nextInt(mover.getType().getStepReps()); /// adds a random legal length of reps
      end = start;
      try{
        for(int i = 0; i<reps; i++)
          end = board.step(mover, step, end);                       /// execute the number of moves, updating end
      }catch(ImpossiblePositionException e){}                       
      
      checkCount++;
      if (board.getPiece(end) != null && board.isLegalMove(start, end)) kill = true;
      else if (checkCount > 1000) kill = true;                     /// let search revert to random if kill cant be found

    }

    
    
    return new Position[] {start,end};                              
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

}
