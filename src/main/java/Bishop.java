
import javafx.scene.image.Image;

public class Bishop extends Piece {

    private final Image image;

    public Bishop(boolean isWhite) {
        super(isWhite);
        String imagePath = isWhite
                ? "/pieces_skin/skin01/white/L01bishop.png"
                : "/pieces_skin/skin01/black/D01bishop.png";
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

        // A bishop can only move diagonally (equal row and col difference) and cannot stay in place
        if (dRow != dCol || dRow == 0) {
            return false;
        }

        // Determine step direction (-1 or +1) along rows and columns
        int rowDirection = Integer.compare(targetRow, startRow);
        int colDirection = Integer.compare(targetCol, startCol);

        // Check if the diagonal path is clear (no pieces in between)
        int currentRow = startRow + rowDirection;
        int currentCol = startCol + colDirection;

        while (currentRow != targetRow && currentCol != targetCol) {
            if (board[currentRow][currentCol] != null) {
                return false; // Path is blocked by another piece
            }
            currentRow += rowDirection;
            currentCol += colDirection;
        }

        // Target square must be empty or contain an opponent's piece
        Piece targetPiece = board[targetRow][targetCol];
        return targetPiece == null || targetPiece.isWhite() != isWhite();
    }
}