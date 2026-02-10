package com.marginallyclever.slavadukerani;

/// A single tile of the game board. Contains the tile's coordinates, type (empty, mine, or exit), sensor value, and
/// whether it's hidden or flagged.
public class GridTile {
    public int x, y;
    public int type = 0; // 0 = empty, 1 = mine, 2 = exit
    public int sensorValue = 0;
    public boolean hidden = true;
    public boolean flagged = false;

    public GridTile(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
