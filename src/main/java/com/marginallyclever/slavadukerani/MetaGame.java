package com.marginallyclever.slavadukerani;

import javax.swing.*;
import java.awt.*;

/// MetaGame runs multiple instances of [SlavaDukerani] and provides a UX to control them.
public class MetaGame extends JPanel implements FlagChangeListener, GameOverListener{
    private SlavaDukerani game = null;
    private final MetaGameSettingsPanel settingsPanel = new MetaGameSettingsPanel(20,10,30,(int)(Math.random()*1000000));
    static final JFrame frame = new JFrame("Slava Dukerani");
    private final JMenuBar menuBar = new JMenuBar();
    private final JFormattedTextField numMinesLeft = PanelHelper.addNumberFieldInt("Number of mines left", 0);
    private final JTextField timeDisplay = new JTextField("0:00");
    private final JMenuItem settingsButton = new JMenuItem("Settings");
    private final JMenuItem newGame = new JMenuItem("New Game");
    private final JMenuItem resetGame = new JMenuItem("Restart");
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
        initMenuBar();

        newGame.addActionListener(e -> startNewGame());
        resetGame.addActionListener(e -> resetGame());
        settingsButton.addActionListener(e->showSettingsDialog());
        startNewGame();
    }

    private void showSettingsDialog() {
        int result = JOptionPane.showConfirmDialog(frame, settingsPanel, "Game Settings", JOptionPane.OK_CANCEL_OPTION);
        if(result==JOptionPane.OK_OPTION) {
            startNewGame();
        }
    }

    private void startNewGame() {
        settingsPanel.newSeed();
        resetGame();
    }

    private void resetGame() {
        int totalMines = settingsPanel.getMines();
        numMinesLeft.setValue(totalMines);
        if(game!=null) {
            game.removeFlagChangeListener(this);
            game.setRequestFocusEnabled(false);
        }
        game = new SlavaDukerani(
                settingsPanel.getBoardWidth(),
                settingsPanel.getBoardHeight(),
                settingsPanel.getSeed(),
                settingsPanel.getMines());
        removeAll();
        add(game, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        revalidate();

        game.addFlagChangeListener(this);
        game.addGameOverListener(this);
        startNewTimer();

        game.setFocusable(true);
        // set focus on the game.
        game.setRequestFocusEnabled(true);
        // Request focus on the EDT to ensure keyboard events go to the game component.
        SwingUtilities.invokeLater(() -> {
            // prefer requestFocusInWindow; if that fails, fall back to requestFocus
            if (!game.requestFocusInWindow()) {
                game.requestFocus();
            }
        });

        timer.start();
    }

    private void startNewTimer() {
        // stop any existing timer to avoid multiple timers running concurrently
        if (timer != null) {
            timer.stop();
        }
        seconds = 0;
        timer = new Timer(1000, e -> {
            //System.out.println(seconds%2==0?"tick":"tock");
            seconds++;
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            long s = seconds % 60;
            StringBuilder sb = new StringBuilder();
            if(h>0) sb.append(String.format("%02d",h)).append(":");
            if(h>0||m>0) sb.append(String.format("%02d",m)).append(":");
            sb.append(String.format("%02d",s));
            timeDisplay.setText(sb.toString());
        });
    }

    private void initMenuBar() {
        frame.setJMenuBar(menuBar);
        JMenu menu = new JMenu("Game");
        menuBar.add(menu);
        menu.add(settingsButton);
        menu.add(newGame);
        menu.add(resetGame);

        menuBar.add(numMinesLeft);
        menuBar.add(timeDisplay);
        numMinesLeft.setEditable(false);
        numMinesLeft.setFocusable(false);
        timeDisplay.setEditable(false);
        timeDisplay.setFocusable(false);
        // timeDisplay right justified
        timeDisplay.setHorizontalAlignment(JTextField.RIGHT);
    }

    @Override
    public void flagCountChanged(int flagCount) {
        numMinesLeft.setValue(settingsPanel.getMines()- flagCount);
    }

    @Override
    public void gameOver(boolean won) {
        timer.stop();
    }
}
