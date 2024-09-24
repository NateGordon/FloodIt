import java.util.*;
import tester.Tester;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// Represents a single square of the game area
class Cell {
  int x; // x coordinate
  int y; // y coordinate
  Color color; // color of the cell
  boolean flooded; // whether the cell is flooded
  // adjacent cells
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;

  // Constructor with random color
  Cell(int x, int y, ArrayList<Color> colors) {
    this.x = x;
    this.y = y;
    this.color = colors.get(new Random().nextInt(colors.size()));
    this.flooded = false;
  }

  // Constructor with specific color (for testing)
  Cell(int x, int y, Color color) {
    this.x = x;
    this.y = y;
    this.color = color;
    this.flooded = false;
  }

  // Draw the cell as a rectangle image
  WorldImage drawCell(int cellSize) {
    return new RectangleImage(cellSize, cellSize, OutlineMode.SOLID, this.color);
  }

  // EFFECT: floods adjacent cells of the right color if this cell is flooded
  void flooding(Color c) {
    if (this.flooded) {
      if (this.top != null) {
        this.top.floodingHelper(c);
      }
      if (this.left != null) {
        this.left.floodingHelper(c);
      }
      if (this.bottom != null) {
        this.bottom.floodingHelper(c);
      }
      if (this.right != null) {
        this.right.floodingHelper(c);
      }
    }
  }

  // EFFECT: floods the cell if the color matches and the cell is not flooded
  void floodingHelper(Color c) {
    if (this.color.equals(c) && !this.flooded) {
      this.flooded = true;
    }
  }

  // Returns the right and bottom neighbors of the cell if they have not already been selected
  // and if they exist
  ArrayList<Cell> waterNext(ArrayList<Cell> done) {
    ArrayList<Cell> temp = new ArrayList<Cell>();
    if (this.right != null && !done.contains(this.right)) {
      temp.add(this.right);
    }
    if (this.bottom != null && !done.contains(this.bottom)) {
      temp.add(this.bottom);
    }
    return temp;
  }
}

// Represents the game world
class FloodItWorld extends World {
  ArrayList<Cell> board; // list of cells in the game
  ArrayList<Cell> currCells; // current cells to be flooded
  ArrayList<Cell> doneCells; // cells already processed
  int boardSize; // size of the board
  int cellSize; // size of each cell
  ArrayList<Color> colors; // list of colors used in the game
  Color currColor; // current color selected
  int clicks; // number of clicks made by the player
  int moves; // maximum allowed moves
  boolean win; // whether the player has won

  // Constructor with specified size and number of colors
  //Constructor with specified size and number of colors
  FloodItWorld(int boardSize, int numColors, int cellSize) {
    this.boardSize = boardSize;
    this.cellSize = cellSize;
    this.colors = new ArrayList<Color>();
    ArrayList<Color> colorList = new ArrayList<Color>(Arrays.asList(Color.RED, Color.BLUE,
        Color.GREEN, Color.ORANGE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.BLACK,
        Color.GRAY, Color.PINK));
    for (int i = 0; i < numColors; i++) {
      int randNum = new Random().nextInt(colorList.size());
      this.colors.add(colorList.get(randNum));
      colorList.remove(randNum);
    }
    createBoard();
  }

  // Create the board with cells
  void createBoard() {
    this.board = new ArrayList<Cell>();
    for (int y = 0; y < boardSize; y++) {
      for (int x = 0; x < boardSize; x++) {
        this.board.add(new Cell(x, y, this.colors));
      }
    }
    for (Cell cell : board) {
      setNeighbors(cell);
    }
    Cell zero = this.board.get(0);
    zero.flooded = true;
    zero.flooding(zero.color);
    this.currColor = zero.color;
    this.clicks = 0;
    this.moves = (int)Math.round(1.75 * boardSize) + 3;
    this.currCells = new ArrayList<Cell>(Arrays.asList(zero));
    this.doneCells = new ArrayList<Cell>();
    this.win = false;
  }

  // Set the neighbors of the given cell
  void setNeighbors(Cell cell) {
    int x = cell.x;
    int y = cell.y;
    if (y > 0) {
      cell.top = board.get((y - 1) * boardSize + x);
    }
    if (y < boardSize - 1) {
      cell.bottom = board.get((y + 1) * boardSize + x);
    }
    if (x > 0) {
      cell.left = board.get(y * boardSize + (x - 1));
    }
    if (x < boardSize - 1) {
      cell.right = board.get(y * boardSize + (x + 1));
    }
  }

  // Make the scene for the game
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(boardSize * cellSize, boardSize * cellSize);
    for (Cell cell : board) {
      scene.placeImageXY(cell.drawCell(cellSize),
          cell.x * cellSize + cellSize / 2, cell.y * cellSize + cellSize / 2);
    }
    return scene;
  }

  // EFFECT: runs every tick, starts the waterfall effect and checks for win
  public void onTick() {
    if (this.currCells.size() > 0) {
      this.waterfall();
    }
    if (this.winCheck() && this.clicks <= this.moves) {
      this.win = true;
    }
  }

  // EFFECT: animates the waterfall effect of the game
  public void waterfall() {
    ArrayList<Cell> tempCells = new ArrayList<Cell>();
    for (Cell c : this.currCells) {
      if (c.flooded) {
        c.color = this.currColor;
      }
      tempCells.addAll(c.waterNext(this.doneCells));
      this.doneCells.addAll(c.waterNext(this.doneCells));
    }
    this.currCells = new ArrayList<Cell>();
    this.currCells.addAll(tempCells);
  }

  // EFFECT: handles mouse clicks, checks for validity of cell clicks
  public void onMouseClicked(Posn pos) {
    int xCo = pos.x / cellSize;
    int yCo = pos.y / cellSize;
    if (xCo >= 0 && xCo < boardSize && yCo >= 0 && yCo < boardSize) {
      Cell clickedCell = this.board.get(yCo * boardSize + xCo);
      if (!clickedCell.color.equals(this.currColor)) {
        this.clicks++;
        this.currColor = clickedCell.color;
        this.flooding();
      }
    }
  }

  //EFFECT: sets all cells that are flooded to be flooded = true
  public void flooding() {
    for (Cell c : this.board) {
      c.flooding(this.currColor);
    }
    this.currCells = new ArrayList<Cell>(Arrays.asList(this.board.get(0)));
    this.doneCells = new ArrayList<Cell>();
  }

  // EFFECT: handles the reset of the game
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      createBoard();
    }
  }

  // checks if win condition is fulfilled
  public boolean winCheck() {
    for (Cell c : this.board) {
      if (!c.flooded) {
        return false;
      }
    }
    return true;
  }
}

//Example class for testing
class ExamplesFloodItWorld {
  // Test creating a new game and rendering the scene
  void testGame(Tester t) {
    FloodItWorld world = new FloodItWorld(22, 6, 20);
    world.bigBang(440, 440, 0.1);
  }

  // Test creating a board and setting neighbors
  void testCreateBoard(Tester t) {
    FloodItWorld world = new FloodItWorld(3, 3, 20);
    Cell cell = world.board.get(4); // Center cell
    t.checkExpect(cell.top, world.board.get(1));
    t.checkExpect(cell.bottom, world.board.get(7));
    t.checkExpect(cell.left, world.board.get(3));
    t.checkExpect(cell.right, world.board.get(5));
  }

  // Test flood functionality
  void testFlooding(Tester t) {
    Cell cell1 = new Cell(0, 0, Color.RED);
    Cell cell2 = new Cell(0, 1, Color.RED);
    cell1.bottom = cell2;
    cell1.flooded = true;
    cell1.flooding(Color.RED);
    t.checkExpect(cell1.flooded, true);
    t.checkExpect(cell2.flooded, true);
  }

  // Test flood helper functionality
  void testFloodingHelper(Tester t) {
    Cell cell1 = new Cell(0, 0, Color.RED);
    Cell cell2 = new Cell(0, 1, Color.RED);
    cell1.bottom = cell2;
    cell1.flooded = true;
    cell1.floodingHelper(Color.RED);
    t.checkExpect(cell1.flooded, true);
    t.checkExpect(cell2.flooded, false); // cell2 should not be flooded until flood is called
  }

  // Test water neighbors functionality
  void testWaterNext(Tester t) {
    Cell cell1 = new Cell(0, 0, Color.RED);
    Cell cell2 = new Cell(0, 1, Color.RED);
    Cell cell3 = new Cell(1, 0, Color.RED);
    cell1.bottom = cell2;
    cell1.right = cell3;
    ArrayList<Cell> done = new ArrayList<Cell>();
    ArrayList<Cell> neighbors = cell1.waterNext(done);
    t.checkExpect(neighbors.size(), 2);
    t.checkExpect(neighbors.contains(cell2), true);
    t.checkExpect(neighbors.contains(cell3), true);
  }

  // Test onTick functionality
  void testOnTick(Tester t) {
    FloodItWorld world = new FloodItWorld(3, 3, 20);
    world.currCells = new ArrayList<Cell>(Arrays.asList(world.board.get(0)));
    world.doneCells = new ArrayList<Cell>();
    world.onTick();
    t.checkExpect(world.currCells.size(), 2); // should add right and bottom neighbors
  }

  // Test onMouseClicked functionality
  void testOnMouseClicked(Tester t) {
    FloodItWorld world = new FloodItWorld(3, 3, 20);
    world.currColor = Color.BLUE;
    world.board.get(0).color = Color.black;
    t.checkExpect(world.currColor, Color.BLUE);
    world.onMouseClicked(new Posn(6, 6));
    t.checkExpect(world.currColor, Color.black);
  }

  // Test flood functionality within FloodItWorld
  void testFloodWorld(Tester t) {
    FloodItWorld world = new FloodItWorld(3, 3, 20);
    world.board.get(0).color = Color.black;
    world.board.get(1).color = Color.black;
    world.board.get(2).color = Color.black;
    world.board.get(3).color = Color.black;
    world.currColor = Color.black;
    world.flooding();
    t.checkExpect(world.board.get(0).flooded, true);
    t.checkExpect(world.board.get(1).flooded, true);
    t.checkExpect(world.board.get(2).flooded, true);
    t.checkExpect(world.board.get(4).flooded, false);
  }


  // Test winCheck functionality
  void testWinCheck(Tester t) {
    FloodItWorld world = new FloodItWorld(3, 3, 20);
    for (Cell c : world.board) {
      c.flooded = true;
    }
    t.checkExpect(world.winCheck(), true);
  }

  // Test onKeyEvent functionality
  void testOnKeyEvent(Tester t) {
    FloodItWorld world = new FloodItWorld(3, 3, 20);
    world.onKeyEvent("r"); // Press 'r' key to reset
    t.checkExpect(world.clicks, 0); // Clicks should reset to 0
    t.checkExpect(world.currCells.size(), 1); // Should only contain the initial cell
  }
}
