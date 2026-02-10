package com.marginallyclever.slavadukerani;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/// Controls one "round" of the game, including the game state, rendering, and user input.
///
/// Can be embedded in a larger UX or used standalone.
public class SlavaDukerani extends JPanel {
    public static void main( String[] args ) {
        // open a centered 800x600 window with the title "Slava Dukerani"
        var app = new SlavaDukerani(20,10, 30,(int)(Math.random()*1000000));

        JFrame frame = new JFrame("SlavaDukerani");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(app);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static final int TILE_SIZE_X = 32;
    static final int TILE_SIZE_Y = 32;

    private GridTile [][] grid;
    private GridTile hoverOver;  // the cursor is over this tile.
    private final EventListenerList listenerList = new EventListenerList();
    private final Random rand;

    private int gridWidth = 20;
    private int gridHeight = 10;  // grid width and height

    // add field
    private BufferedImage playerImage,
            sensorImage,
            flagImage,
            mineImage,
            exitImage;


    private int px=0,py=0;  // player position
    private int sx =1, sy =1;  // sensor position
    private int numMines = 30; // number of mines to place on the grid
    private boolean gameOver = false;
    private boolean youWon = false;
    private boolean initialized=false;
    private final int sensorRange = 2; // range of the sensor


    /// Construct a new SlavaDukerani game with the specified grid size, seed, and number of mines.
    /// @param gridWidth  Width of the grid in tiles.
    /// @param gridHeight Height of the grid in tiles.
    /// @param seed       Seed for the random number generator.
    /// @param numMines   Number of mines to place on the grid.
    public SlavaDukerani(int gridWidth, int gridHeight,int seed,int numMines) {
        super(new BorderLayout(5, 5));

        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.numMines = numMines;

        rand = new Random(seed);

        setSize(this.gridWidth *TILE_SIZE_X, this.gridHeight * TILE_SIZE_Y);
        setMinimumSize  (new Dimension(this.gridWidth * TILE_SIZE_X, this.gridHeight * TILE_SIZE_Y));
        setPreferredSize(new Dimension(this.gridWidth * TILE_SIZE_X, this.gridHeight * TILE_SIZE_Y));
        setMaximumSize  (new Dimension(this.gridWidth * TILE_SIZE_X, this.gridHeight * TILE_SIZE_Y));

        attachMouseListeners();
        attachKeyboardListeners();
    }

    void onFirstFrame() throws Exception {
        System.out.println("Initializing game...");
        //initToolBar();
        initGame();
        initArt();
        System.out.println("Ready...");
    }

    private void attachKeyboardListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(gameOver || !initialized) return;
                int dx=0,dy=0;
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_W, KeyEvent.VK_UP   :  dy=-1;  break;
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN :  dy= 1;  break;
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT :  dx=-1;  break;
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT:  dx= 1;  break;
                }
                movePlayer(dx, dy);
            }
        });
    }

    private void movePlayer(int dx, int dy) {
        int x = px+dx;
        int y = py+dy;
        // check bounds
        if (x<0 || x>= gridWidth || y<0 || y>= gridHeight) return;
        // check for mine
        var tile = grid[x][y];
        if(tile.type == 1) {
            // you died, game over
            System.out.println("You died.  Game over!");
            fireGameOver(false);
        } else if(tile.type==2) {
            // you win!
            System.out.println("You win!");
            fireGameOver(true);
        }
        // if pushing into the sensor, try to push the sensor
        if(sx==x && sy==y) {
            int bx2 = sx +dx;
            int by2 = sy +dy;
            if (bx2<0 || bx2>= gridWidth || by2<0 || by2>= gridHeight) return; // box out of bounds
            var tile2 = grid[bx2][by2];
            if(tile2.type==1) {
                // equipment destroyed, game over.
                System.out.println("Equipment destroyed.  Game over!");
                fireGameOver(false);
            }
            if(tile2.type==2) {
                // box pushed onto exit, you win!
                System.out.println("You win!");
                fireGameOver(true);
            }
            // move sensor
            sx = bx2;
            sy = by2;
            calculateSensorValues();
        }
        px = x;
        py = y;
        repaint();
    }

    private void attachMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(gameOver || !initialized) return;

                // get tile at cursor position.
                int x = e.getX()/(getWidth()/ gridWidth);
                int y = e.getY()/(getHeight()/ gridHeight);
                if (x<0 || x>= gridWidth ||
                    y<0 || y>= gridHeight) return;

                GridTile tile = grid[x][y];

                // right click
                if (SwingUtilities.isRightMouseButton(e)) {
                    // on hidden tile to flag/unflag it.
                    if (tile.hidden) {
                        tile.flagged = !tile.flagged;
                        fireFlagChanged();
                        repaint();
                    }
                }

                // left click
                if(SwingUtilities.isLeftMouseButton(e)){
                    // on a hidden tile with no flag to reveal it.
                    if(tile.hidden) {
                        if(!tile.flagged) {
                            clearTile(x, y);
                        }
                    } else {
                        // on a revealed tile to move player there if adjacent.
                        if((tile.x==px && Math.abs(tile.y-py)==1) ||
                           (tile.y==py && Math.abs(tile.x-px)==1)) {
                            movePlayer(tile.x-px, tile.y-py);
                        }
                    }
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if(gameOver || !initialized) return;
                // get tile at cursor position.
                int x = e.getX()/(getWidth()/ gridWidth);
                int y = e.getY()/(getHeight()/ gridHeight);
                if (x<0 || x>= gridWidth ||
                    y<0 || y>= gridHeight) return;
                // show sensor value in title bar
                hoverOver = grid[x][y];
                repaint();
            }
        });
    }

    public void addFlagChangeListener(FlagChangeListener listener) {
        listenerList.add(FlagChangeListener.class, listener);
    }
    public void removeFlagChangeListener(FlagChangeListener listener) {
        listenerList.remove(FlagChangeListener.class, listener);
    }

    private void fireFlagChanged() {
        int numFlags = 0;
        for (int x = 0; x< gridWidth; x++) {
            for (int y = 0; y< gridHeight; y++) {
                if(grid[x][y].flagged) numFlags++;
            }
        }

        for (FlagChangeListener listener : listenerList.getListeners(FlagChangeListener.class)) {

            listener.flagCountChanged(numFlags);
        }
    }

    public void addGameOverListener(GameOverListener listener) {
        listenerList.add(GameOverListener.class, listener);
    }
    public void removeGameOverListener(GameOverListener listener) {
        listenerList.remove(GameOverListener.class, listener);
    }

    private void fireGameOver(boolean won) {
        gameOver = true;
        youWon = won;
        for (GameOverListener listener : listenerList.getListeners(GameOverListener.class)) {
            listener.gameOver(won);
        }
    }

    private void initGame() {
        // allocate empty grid
        grid = new GridTile[gridWidth][gridHeight];
        for (int x = 0; x< gridWidth; x++) {
            for (int y = 0; y< gridHeight; y++) {
                grid[x][y] = new GridTile(x,y);
            }
        }
        grid[px][py].type = 0; // player start
        grid[sx][sy].type = 3; // sensor start
        grid[gridWidth -1][gridHeight -1].type = 2; // exit

        grid[0][0].type = 3; // temp fill so first click isn't a mine, will be cleared later
        grid[0][1].type = 3; // temp fill so first click isn't a mine, will be cleared later
        grid[1][0].type = 3; // temp fill so first click isn't a mine, will be cleared later

        // add some mines
        placeMines();

        grid[0][0].type = 0; // clear
        grid[0][1].type = 0; // clear
        grid[1][0].type = 0; // clear

        calculateSensorValues();
        clearTile(px,py);
        clearTile(sx, sy);
    }

    // update all sensor values.
    private void calculateSensorValues() {
        for (int x = 0; x< gridWidth; x++) {
            for (int y = 0; y< gridHeight; y++) {
                grid[x][y].sensorValue = calculateSensorValue(x,y);
            }
        }
    }

    private int calculateSensorValue(int x, int y) {
        int count = 0;
        for (int dx=-1;dx<=1;dx++) {
            for (int dy=-1;dy<=1;dy++) {
                if(dx==0 && dy==0) continue;
                int nx = x+dx;
                int ny = y+dy;
                if (nx>=0 && nx< gridWidth && ny>=0 && ny< gridHeight) {
                    if(grid[nx][ny].type == 1) count++;
                }
            }
        }
        return count;
    }

    private void placeMines() {
        int placed=0;
        while(placed<numMines) {
            int x = (int)(rand.nextDouble() * gridWidth);
            int y = (int)(rand.nextDouble() * gridHeight);
            if(grid[x][y].type==0) {
                grid[x][y].type = 1;
                placed++;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!initialized) {
            try {
                onFirstFrame();
            } catch(Exception e) {
                e.printStackTrace();
                java.lang.System.exit(1);
            }
            initialized=true;
        }

        // draw all tiles
        for(int x = 0; x< gridWidth; ++x) {
            for(int y = 0; y< gridHeight; ++y) {
                drawOneTile(g,x,y);
            }
        }

        drawImage(g,playerImage,px,py,Color.BLUE);
        drawImage(g,sensorImage, sx, sy,Color.ORANGE);
        highlightHoverOver(g);
        drawSensorRange(g);

        if(gameOver) {
            // draw game over text
            String text = youWon ? "You Win!" : "Game Over";
            Font oldFont = g.getFont();
            Font bold32 = oldFont.deriveFont(Font.BOLD, 64f);
            g.setFont(bold32);
            FontMetrics fm = g.getFontMetrics(bold32);
            int sx = (getWidth() - fm.stringWidth(text)) / 2;
            int sy = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.setColor(youWon ? Color.BLUE : Color.RED);
            g.drawString(text, sx, sy);
            g.setFont(oldFont);
        }
    }

    // illustrate sensor range
    private void drawSensorRange(Graphics g) {
        g.setColor(new Color(255, 165, 0, 32)); // semi-transparent orange
        int sensorDrawX = (sx -sensorRange)* TILE_SIZE_X;
        int sensorDrawY = (sy -sensorRange)* TILE_SIZE_Y;
        int sensorDrawSizeX = (sensorRange*2+1)* TILE_SIZE_X;
        int sensorDrawSizeY = (sensorRange*2+1)* TILE_SIZE_Y;
        g.fillRect(sensorDrawX, sensorDrawY, sensorDrawSizeX, sensorDrawSizeY);
    }

    // highlight if hover over
    private void highlightHoverOver(Graphics g) {
        if(hoverOver==null) return;

        int drawX = hoverOver.x* TILE_SIZE_X;
        int drawY = hoverOver.y* TILE_SIZE_Y;
        // if hoverOver is cardinal with and adjacent to the player, highlight in green, otherwise yellow.
        if((hoverOver.x==px && Math.abs(hoverOver.y-py)==1) ||
           (hoverOver.y==py && Math.abs(hoverOver.x-px)==1)) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.YELLOW);
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(drawX, drawY, TILE_SIZE_X, TILE_SIZE_Y);
    }

    private void drawImage(Graphics g, BufferedImage img, int x, int y,Color fallbackColor) {
        int dx = x * TILE_SIZE_X;
        int dy = y * TILE_SIZE_Y;
        if (img != null) {
            g.drawImage(img, dx, dy, TILE_SIZE_X, TILE_SIZE_Y, null);
        } else {
            g.setColor(fallbackColor);
            g.fillRect(dx, dy, TILE_SIZE_X, TILE_SIZE_Y);
        }
    }

    private void drawOneTile(Graphics g, int x, int y) {
        GridTile tile = grid[x][y];
        int drawX = x* TILE_SIZE_X;
        int drawY = y* TILE_SIZE_Y;

        // draw hidden tile
        if(tile.hidden) {
            g.setColor(Color.GRAY);
            g.fillRect(drawX, drawY, TILE_SIZE_X, TILE_SIZE_Y);
            if(gameOver) {
                // if game over, show mines
                if(tile.type==1) {
                    drawImage(g, mineImage,x,y,Color.BLACK);
                }
            } else if(tile.flagged) {
                g.setColor(Color.WHITE);
                g.drawImage(flagImage, drawX, drawY, TILE_SIZE_X, TILE_SIZE_Y, null);
            }
        } else {
            // draw revealed tile
            switch (tile.type) {
                case 0: // empty
                    g.setColor(Color.WHITE);
                    g.fillRect(drawX, drawY, TILE_SIZE_X, TILE_SIZE_Y);
                    if (tile.sensorValue > 0) {
                        // if bx/by is within sensorRange of this tile, show sensor value
                        if(Math.abs(tile.x- sx) <= sensorRange && Math.abs(tile.y- sy) <= sensorRange) {
                            drawSensorValue(g,drawX,drawY,tile.sensorValue);
                        }
                    }
                    break;
                case 1: // mine
                    drawImage(g, mineImage,x,y,Color.BLACK);
                    break;
                case 2: // exit
                    drawImage(g, exitImage,x,y,Color.GREEN);
                    break;
            }
        }
        // draw tile border
        g.setColor(Color.DARK_GRAY);
        g.drawRect(drawX, drawY, TILE_SIZE_X, TILE_SIZE_Y);
    }

    private void drawSensorValue(Graphics g, int drawX, int drawY, int sensorValue) {
        // set bold 16px font and center the text using FontMetrics
        Font oldFont = g.getFont();
        Font bold16 = oldFont.deriveFont(Font.BOLD, 16f);
        g.setFont(bold16);
        FontMetrics fm = g.getFontMetrics(bold16);
        String text = Integer.toString(sensorValue);
        int sx = drawX + (TILE_SIZE_X - fm.stringWidth(text)) / 2;
        int sy = drawY + (TILE_SIZE_Y - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(Color.BLACK);
        g.drawString(text, sx, sy);
        g.setFont(oldFont);
    }

    // clear this tile.
    private void clearTile(int x, int y) {
        GridTile tile = grid[x][y];
        tile.hidden = false;
        if(tile.type==1) {
            System.out.println("Poked a mine.  Game over!");
            fireGameOver(false);
            return;
        }

        if(tile.sensorValue>0) return;
        // repeat in adjacent hidden unflagged tiles.
        for(int dx=-1;dx<=1;dx++) {
            for(int dy=-1;dy<=1;dy++) {
                int nx = x+dx;
                int ny = y+dy;
                if (nx>=0 && nx< gridWidth && ny>=0 && ny< gridHeight) {
                    GridTile adjacentTile = grid[nx][ny];
                    if(adjacentTile.hidden && !adjacentTile.flagged) {
                        clearTile(nx, ny);
                    }
                }
            }
        }
    }

    // replace initArt() with:
    private void initArt() throws IOException {
        try {
            String packagePath = getClass().getPackage().getName().replace('.', '/');
            java.util.List<String> pngs = listPngResources(packagePath+"/dukes"); // folder inside src/main/resources
            if (pngs.isEmpty()) {
                System.err.println("No PNGs found.");
                return;
            }
            String chosen = pngs.get(new Random().nextInt(pngs.size()));
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(chosen)) {
                if (is != null) {
                    playerImage = ImageIO.read(is);
                    System.out.println("Loaded art: " + chosen);
                } else {
                    System.err.println("Failed to open resource: " + chosen);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        sensorImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("sensor.png")));
        flagImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("flag-32.png")));
        mineImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("mine.png")));
        exitImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("exit-32.png")));
    }

    // helper to list pngs from a resource folder (handles both file and jar)
    private java.util.List<String> listPngResources(String resourceFolder) throws IOException, URISyntaxException {
        List<String> result = new ArrayList<>();
        ClassLoader cl = getClass().getClassLoader();
        URL dirURL = cl.getResource(resourceFolder);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            File folder = new File(dirURL.toURI());
            File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File f : files) result.add(resourceFolder + "/" + f.getName());
            }
        } else if (dirURL != null && dirURL.getProtocol().equals("jar")) {
            String dirPath = dirURL.getPath();
            String jarPath = dirPath.substring(5, dirPath.indexOf("!")); // strip "file:" and everything after "!"
            try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(resourceFolder + "/") && name.toLowerCase().endsWith(".png")) {
                        result.add(name);
                    }
                }
            }
        } else {
            // fallback: attempt scanning classpath URLs (may not list inside JARs)
            Enumeration<URL> resources = cl.getResources(resourceFolder);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if ("file".equals(url.getProtocol())) {
                    File folder = new File(url.toURI());
                    File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
                    if (files != null) for (File f : files) result.add(resourceFolder + "/" + f.getName());
                }
            }
        }
        return result;
    }
}
