package com.marginallyclever;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;

/// MetaGame runs multiple instances of [SlavaDukerani] and provides a toolbar to control them.
public class MetaGame extends JPanel implements FlagChangeListener, GameOverListener{
    private SlavaDukerani game = null;

    static final JFrame frame = new JFrame("Slava Dukerani");
    private final JToolBar toolBar = new JToolBar();
    private final JFormattedTextField numMinesLeft = addNumberFieldInt("Number of mines left", 0);
    private final JFormattedTextField numMines = addNumberFieldInt("Number of mines", 30);
    private final JFormattedTextField width = addNumberFieldInt("Width", 20);
    private final JFormattedTextField seed = addNumberFieldInt("Map Seed", (int)(Math.random()*1000000));
    private final JFormattedTextField height = addNumberFieldInt("Height", 10);
    private final JTextField timeDisplay = new JTextField("0:00");
    private final JButton newGame = new JButton("New Game");
    private final JButton resetGame = new JButton("⟳");
    private Timer timer;
    private long seconds;


    public static void main( String[] args ) {
        // open a centered window with the title "Slava Dukerani"

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new MetaGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public MetaGame() {
        setLayout(new BorderLayout());
        initToolBar();

        add(toolBar, BorderLayout.NORTH);

        newGame.addActionListener(e -> startNewGame());
        resetGame.addActionListener(e -> resetGame());
        startNewGame();
    }

    private void startNewGame() {
        seed.setText((int)(Math.random()*1000000)+"");
        resetGame();
    }

    private void resetGame() {
        int totalMines = Integer.parseInt(numMines.getText());
        numMinesLeft.setValue(totalMines);
        if(game!=null) {
            game.removeFlagChangeListener(this);
        }
        game = new SlavaDukerani(
                ((Number)width.getValue()).intValue(),
                ((Number)height.getValue()).intValue(),
                ((Number)seed.getValue()).intValue(),
                totalMines);
        removeAll();
        add(toolBar, BorderLayout.NORTH);
        add(game, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        revalidate();

        game.addFlagChangeListener(this);
        game.addGameOverListener(this);
        seconds = 0;
        timer = new Timer(1000, e -> {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            long s = seconds % 60;
            StringBuilder sb = new StringBuilder();
            if(h>0) sb.append(String.format("%02d",h)).append(":");
            if(h>0||m>0) sb.append(String.format("%02d",m)).append(":");
            sb.append(String.format("%02d",s));
            timeDisplay.setText(sb.toString());
        });
        timer.start();
    }

    private void initToolBar() {
        toolBar.setFloatable(false);
        toolBar.add(numMines);
        toolBar.add(width);
        toolBar.add(seed);
        toolBar.add(height);
        toolBar.add(newGame);
        toolBar.add(resetGame);
        toolBar.add(numMinesLeft);
        toolBar.add(timeDisplay);
        numMinesLeft.setEditable(false);
    }

    static public NumberFormatter getNumberFormatterInt() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        NumberFormatter formatter = new NumberFormatter(format);
        //formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(true);
        return formatter;
    }

    /**
     * <p>A convenience method to add a number field to a panel.</p>
     * @param toolTip the tooltip for the field
     * @param value the initial value
     * @return the {@link JFormattedTextField}
     */
    public static JFormattedTextField addNumberFieldInt(String toolTip, int value) {
        return addNumberField(toolTip,value,getNumberFormatterInt());
    }

    /**
     * <p>A convenience method to add a number field to a panel.</p>
     * @param toolTip the tooltip for the field
     * @param value the initial value
     * @param formatter the {@link NumberFormatter} to use
     * @return the {@link JFormattedTextField}
     */
    public static JFormattedTextField addNumberField(String toolTip, double value, NumberFormatter formatter) {
        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setValue(value);
        field.setToolTipText(toolTip);
        field.setColumns(3);
        field.setMinimumSize(new Dimension(0,20));
        return field;
    }

    @Override
    public void flagCountChanged(int flagCount) {
        numMinesLeft.setValue(Integer.parseInt(numMines.getText()) - flagCount);
    }

    @Override
    public void gameOver(boolean won) {
        timer.stop();
    }
}
