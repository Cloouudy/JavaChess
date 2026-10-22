import javafx.scene.image.Image;

public class Rook extends Piece {

    private final Image image;

    public Rook(boolean isWhite) {
        super(isWhite);
        String imagePath = isWhite
                ? "/pieces_skin/skin01/white/L01rook.png"
                : "/pieces_skin/skin01/black/D01rook.png";
        this.image = new Image(getClass().getResourceAsStream(imagePath));
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int targetRow, int targetCol, Piece[][] board) {
        // vez bud ve stejnem radku nebo ve stejnem sloupci
        if (startRow != targetRow && startCol != targetCol) {
            return false;
        }

        // Určení směru pohybu (-1, 0, nebo 1)
        int rowStep = Integer.compare(targetRow, startRow);
        int colStep = Integer.compare(targetCol, startCol);

        // Kontrola, zda na cestě mezi startem a cílem nestojí jiná figurka
        int currentRow = startRow + rowStep;
        int currentCol = startCol + colStep;

        while (currentRow != targetRow || currentCol != targetCol) {
            if (board[currentRow][currentCol] != null) {
                return false; // Cesta je zablokovaná
            }
            currentRow += rowStep;
            currentCol += colStep;
        }

        // Cílové políčko musí být prázdné nebo na něm stojí soupeřova figurka
        Piece targetPiece = board[targetRow][targetCol];
        return targetPiece == null || targetPiece.isWhite() != isWhite();
    }
}