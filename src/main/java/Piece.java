

import javafx.scene.image.Image;

public abstract class Piece {
    private final boolean isWhite;

    public Piece(boolean isWhite) {
        this.isWhite = isWhite;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public abstract Image getImage();
    public abstract boolean isValidMove(int startRow, int startCol, int targetRow, int targetCol, Piece[][] board);
}