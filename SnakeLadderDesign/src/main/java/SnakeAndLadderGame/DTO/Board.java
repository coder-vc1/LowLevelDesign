package SnakeAndLadderGame.DTO;

public class Board {
    int boardSize;
    int boardStartingPoint;
    int boardFinalPoint
;

    public Board(int boardSize, int boardStartingPoint, int boardFinalPoint
) {
        this.boardSize = boardSize;
        this.boardStartingPoint = boardStartingPoint;
        this.boardFinalPoint
 = boardFinalPoint
;
    }

    public int getBoardSize() {
        return boardSize;
    }
    public int getBoardStartingPoint() {
        return boardStartingPoint;
    }
    public int getBoardFinalPoint
() {
        return boardFinalPoint
;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }
    public void setBoardStartingPoint(int boardStartingPoint) {
        this.boardStartingPoint = boardStartingPoint;
    }
    public void setBoardFinalPoint
(int boardFinalPoint
) {
        this.boardFinalPoint
 = boardFinalPoint
;
    }
}
