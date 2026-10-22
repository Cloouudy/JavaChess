import javafx.scene.image.Image;

public class Pawn extends Piece {

    private final Image image;

    public Pawn(boolean isWhite) {
        super(isWhite);
        String imagePath = isWhite
                ? "/pieces_skin/skin01/white/L01pawn.png"
                : "/pieces_skin/skin01/black/D01pawn.png";
        this.image = new Image(getClass().getResourceAsStream(imagePath));
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int targetRow, int targetCol, Piece[][] board) {
        // Bílá jde nahoru (-1 v matici), Černá jde dolů (+1 v matici)
        int direction = isWhite() ? -1 : 1;
        int startRank = isWhite() ? 6 : 1; // Výchozí řádek pro pěšce (řádek 6 pro bílé, řádek 1 pro černé)

        int dRow = targetRow - startRow;
        int dCol = targetCol - startCol;

        // 1. Pohyb o 1 pole dopředu na prázdné políčko
        if (dCol == 0 && dRow == direction && board[targetRow][targetCol] == null) {
            return true;
        }

        // 2. První pohyb o 2 pole dopředu (musí být na startovním řádku a obě políčka volná)
        if (dCol == 0 && startRow == startRank && dRow == 2 * direction) {
            int middleRow = startRow + direction;
            if (board[middleRow][targetCol] == null && board[targetRow][targetCol] == null) {
                return true;
            }
        }

        // 3. Braní figurky úhlopříčně o 1 pole
        if (Math.abs(dCol) == 1 && dRow == direction && board[targetRow][targetCol] != null) {
            // Musí to být soupeřova figurka
            if (board[targetRow][targetCol].isWhite() != isWhite()) {
                return true;
            }
        }

        return false; // Jakýkoliv jiný pohyb je neplatný
    }
}