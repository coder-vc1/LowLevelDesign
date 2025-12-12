package SnakeAndLadderGame.DTO;

public class Player {
    String playerName;
    int currentPosition;

    public Player(String playerName, int currentPosition) {
        this.playerName = playerName;
        this.currentPosition = currentPosition;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }


}
