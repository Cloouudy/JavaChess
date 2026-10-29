

import javafx.scene.image.Image;

public class King extends Piece {

    private final Image image;

    public King(boolean isWhite) {
        super(isWhite);
        String imagePath = isWhite
                ? "/pieces_skin/skin01/white/L01king.png"
                : "/pieces_skin/skin01/black/D01king.png";
        this.image = new Image(getClass().getResourceAsStream(imagePath));
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int targetRow, int targetCol, Piece[][] board) {
        int dRow = Math.abs(targetRow - startRow);
        int dCol = Math.abs(targetCol - startCol);

        // Král nemůže zůstat na místě
        if (dRow == 0 && dCol == 0) {
            return false;
        }

        // Král se může pohnout maximálně o 1 políčko v jakémkoliv směru (horizontálně, vertikálně i diagonálně)
        boolean isOneSquareMove = dRow <= 1 && dCol <= 1;

        if (!isOneSquareMove) {
            return false;
        }

        // Cílové políčko musí být buď prázdné, nebo na něm stojí soupeřova figurka
        Piece targetPiece = board[targetRow][targetCol];
        return targetPiece == null || targetPiece.isWhite() != isWhite();
    }
}