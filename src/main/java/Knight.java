
import javafx.scene.image.Image;

public class Knight extends Piece {

    private final Image image;

    public Knight(boolean isWhite) {
        super(isWhite);
        String imagePath = isWhite
                ? "/pieces_skin/skin01/white/L01knight.png"
                : "/pieces_skin/skin01/black/D01knight.png";
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

        // Jezdec skáče do "L": (2 pole jedním směrem a 1 pole druhým)
        boolean isLMove = (dRow == 2 && dCol == 1) || (dRow == 1 && dCol == 2);

        if (!isLMove) {
            return false;
        }

        // Cílové políčko musí být buď prázdné, nebo na něm musí stát soupeřova figurka
        Piece targetPiece = board[targetRow][targetCol];
        return targetPiece == null || targetPiece.isWhite() != isWhite();
    }
}