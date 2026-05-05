import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class UnoGameGUI extends JFrame {
    private static final int INITIAL_CARDS = 7;

    private final GameState game;
    private final JLabel currentPlayerLabel = new JLabel();
    private final JLabel directionLabel = new JLabel();
    private final JTextArea logArea = new JTextArea(11, 30);
    private final JPanel handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JButton drawButton = new JButton("Pesca");
    private final TopCardPanel topCardPanel = new TopCardPanel();

    public UnoGameGUI(int totalPlayers) {
        super("UNO - Java Swing");
        this.game = new GameState(totalPlayers);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(new Color(22, 109, 52));
        tablePanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel statusBar = new JPanel(new GridLayout(2, 1));
        statusBar.setOpaque(false);
        currentPlayerLabel.setForeground(Color.WHITE);
        currentPlayerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        directionLabel.setForeground(Color.WHITE);
        directionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        statusBar.add(currentPlayerLabel);
        statusBar.add(directionLabel);
        tablePanel.add(statusBar, BorderLayout.NORTH);

        topCardPanel.setOpaque(false);
        tablePanel.add(topCardPanel, BorderLayout.CENTER);

        handPanel.setOpaque(false);
        JScrollPane handScroll = new JScrollPane(handPanel);
        handScroll.setBorder(BorderFactory.createTitledBorder("La tua mano"));
        handScroll.getViewport().setBackground(new Color(22, 109, 52));
        handScroll.setPreferredSize(new Dimension(860, 230));
        tablePanel.add(handScroll, BorderLayout.SOUTH);
        add(tablePanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBorder(new EmptyBorder(12, 0, 12, 12));
        logArea.setEditable(false);
        rightPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        drawButton.addActionListener(e -> onDrawClicked());
        rightPanel.add(drawButton, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        refreshUI();

        if (game.currentPlayer != 0) SwingUtilities.invokeLater(this::botLoop);
    }

    private void onDrawClicked() { /* unchanged logic */
        if (game.isGameOver() || game.currentPlayer != 0) return;
        Card drawn = game.drawCardForCurrentPlayer();
        log("Hai pescato: " + drawn);
        if (game.canPlay(drawn, game.topCard, game.currentColor)) {
            int choice = JOptionPane.showConfirmDialog(this, "Giocare subito la carta pescata?\n" + drawn,
                    "Carta pescata", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                playHumanCard(game.players.get(0).hand.size() - 1);
                return;
            }
        }
        game.advanceTurn(1);
        refreshUI();
        botLoop();
    }

    private void playHumanCard(int handIndex) {
        if (game.isGameOver() || game.currentPlayer != 0) return;
        Card card = game.players.get(0).hand.get(handIndex);
        if (!game.canPlay(card, game.topCard, game.currentColor)) {
            JOptionPane.showMessageDialog(this, "Carta non giocabile ora.");
            return;
        }
        ColorType chosen = card.color == ColorType.WILD ? askColor() : null;
        if (card.color == ColorType.WILD && chosen == null) return;

        game.playCard(0, handIndex, chosen);
        log("Hai giocato: " + card + (chosen != null ? " -> " + chosen : ""));
        if (game.checkWinAndHandle(0, this::log)) {
            refreshUI();
            return;
        }
        refreshUI();
        botLoop();
    }

    private ColorType askColor() {
        Object[] options = {"RED", "YELLOW", "GREEN", "BLUE"};
        Object choice = JOptionPane.showInputDialog(this, "Scegli un colore", "Carta Jolly", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        return choice == null ? null : ColorType.valueOf(choice.toString());
    }

    private void botLoop() {
        while (!game.isGameOver() && game.currentPlayer != 0) {
            Player bot = game.players.get(game.currentPlayer);
            List<Integer> playable = new ArrayList<>();
            for (int i = 0; i < bot.hand.size(); i++) if (game.canPlay(bot.hand.get(i), game.topCard, game.currentColor)) playable.add(i);

            if (playable.isEmpty()) {
                Card drawn = game.drawCardForCurrentPlayer();
                log(bot.name + " pesca.");
                if (game.canPlay(drawn, game.topCard, game.currentColor)) {
                    int idx = bot.hand.size() - 1;
                    ColorType chosen = drawn.color == ColorType.WILD ? bot.chooseColor() : null;
                    game.playCard(game.currentPlayer, idx, chosen);
                    log(bot.name + " gioca la pescata: " + drawn + (chosen != null ? " -> " + chosen : ""));
                    if (game.checkWinAndHandle(game.currentPlayer, this::log)) break;
                } else game.advanceTurn(1);
            } else {
                int idx = playable.get(new Random().nextInt(playable.size()));
                Card c = bot.hand.get(idx);
                ColorType chosen = c.color == ColorType.WILD ? bot.chooseColor() : null;
                game.playCard(game.currentPlayer, idx, chosen);
                log(bot.name + " gioca: " + c + (chosen != null ? " -> " + chosen : ""));
                if (game.checkWinAndHandle(game.currentPlayer, this::log)) break;
            }
            refreshUI();
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
        refreshUI();
    }

    private void refreshUI() {
        currentPlayerLabel.setText("Turno: " + game.players.get(game.currentPlayer).name + " | Colore attivo: " + game.currentColor);
        directionLabel.setText("Direzione: " + (game.direction == 1 ? "oraria" : "antioraria"));

        handPanel.removeAll();
        List<Card> hand = game.players.get(0).hand;
        for (int i = 0; i < hand.size(); i++) {
            int idx = i;
            CardButton btn = new CardButton(hand.get(i));
            btn.addActionListener(e -> playHumanCard(idx));
            handPanel.add(btn);
        }

        topCardPanel.setCard(game.topCard, game.currentColor);
        drawButton.setEnabled(!game.isGameOver() && game.currentPlayer == 0);
        handPanel.revalidate(); handPanel.repaint(); topCardPanel.repaint();

        if (game.isGameOver()) {
            String winner = game.players.stream().filter(p -> p.hand.isEmpty()).findFirst().map(p -> p.name).orElse("N/A");
            JOptionPane.showMessageDialog(this, "Partita finita! Vincitore: " + winner);
        }
    }

    private void log(String msg) { logArea.append(msg + "\n"); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Integer[] opts = {2, 3, 4, 5, 6};
            Integer players = (Integer) JOptionPane.showInputDialog(null, "Numero totale giocatori (2-6):", "Nuova partita UNO", JOptionPane.PLAIN_MESSAGE, null, opts, 4);
            if (players == null) System.exit(0);
            new UnoGameGUI(players).setVisible(true);
        });
    }

    static class CardButton extends JButton {
        private final Card card;
        CardButton(Card card) {
            this.card = card;
            setPreferredSize(new Dimension(96, 140));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
        }
        @Override protected void paintComponent(Graphics g) { drawUnoCard((Graphics2D) g, card, card.color == ColorType.WILD ? ColorType.RED : card.color, getWidth(), getHeight()); }
    }

    static class TopCardPanel extends JPanel {
        private Card card;
        private ColorType activeColor = ColorType.RED;
        void setCard(Card card, ColorType activeColor) { this.card = card; this.activeColor = activeColor; }
        @Override public Dimension getPreferredSize() { return new Dimension(360, 220); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (card == null) return;
            Graphics2D g2 = (Graphics2D) g;
            drawUnoCard(g2, card, activeColor, 130, 185);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g2.drawString("Carta attiva", 170, 80);
            g2.drawString("Colore: " + activeColor, 170, 110);
        }
    }

    static void drawUnoCard(Graphics2D g2, Card card, ColorType activeColor, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color base = switch (card.color == ColorType.WILD ? activeColor : card.color) {
            case RED -> new Color(206, 46, 46);
            case YELLOW -> new Color(224, 183, 40);
            case GREEN -> new Color(47, 153, 77);
            case BLUE -> new Color(49, 98, 194);
            default -> Color.BLACK;
        };

        g2.setColor(Color.WHITE); g2.fillRoundRect(2, 2, w - 4, h - 4, 26, 26);
        g2.setColor(base); g2.fillRoundRect(8, 8, w - 16, h - 16, 22, 22);
        g2.setColor(new Color(255, 255, 255, 70)); g2.fillOval(w / 7, h / 4, (int) (w * 0.7), (int) (h * 0.45));

        String label = shortLabel(card.value);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 42));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, (w - fm.stringWidth(label)) / 2, h / 2 + 15);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g2.drawString(label, 14, 24);
        int sw = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, w - sw - 14, h - 14);
    }

    static String shortLabel(ValueType v) {
        return switch (v) {
            case ZERO -> "0"; case ONE -> "1"; case TWO -> "2"; case THREE -> "3"; case FOUR -> "4";
            case FIVE -> "5"; case SIX -> "6"; case SEVEN -> "7"; case EIGHT -> "8"; case NINE -> "9";
            case SKIP -> "⊘"; case REVERSE -> "↺"; case DRAW_TWO -> "+2"; case WILD -> "W"; case WILD_DRAW_FOUR -> "+4";
        };
    }

    enum ColorType { RED, YELLOW, GREEN, BLUE, WILD }
    enum ValueType { ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR }
    static class Card { final ColorType color; final ValueType value; Card(ColorType c, ValueType v){color=c;value=v;} public String toString(){return color==ColorType.WILD?value.name():color+" "+value;} }
    static class Player {
        final String name; final List<Card> hand = new ArrayList<>(); Player(String n){name=n;}
        ColorType chooseColor(){Map<ColorType, Long> c=hand.stream().filter(x->x.color!=ColorType.WILD).collect(Collectors.groupingBy(x->x.color, Collectors.counting()));
            return Arrays.asList(ColorType.RED, ColorType.YELLOW, ColorType.GREEN, ColorType.BLUE).stream().max(Comparator.comparingLong(x->c.getOrDefault(x,0L))).orElse(ColorType.RED);} }
    static class GameState {
        final List<Player> players = new ArrayList<>(); final Deque<Card> deck = new ArrayDeque<>(); final Deque<Card> discard = new ArrayDeque<>();
        int currentPlayer=0, direction=1; Card topCard; ColorType currentColor;
        GameState(int totalPlayers){for(int i=0;i<totalPlayers;i++)players.add(new Player(i==0?"Tu":"Bot "+i)); buildDeck(); shuffleDeck(); dealInitialCards(); initDiscard();}
        void buildDeck(){for(ColorType color:Arrays.asList(ColorType.RED,ColorType.YELLOW,ColorType.GREEN,ColorType.BLUE)){deck.add(new Card(color,ValueType.ZERO)); ValueType[] vals={ValueType.ONE,ValueType.TWO,ValueType.THREE,ValueType.FOUR,ValueType.FIVE,ValueType.SIX,ValueType.SEVEN,ValueType.EIGHT,ValueType.NINE,ValueType.SKIP,ValueType.REVERSE,ValueType.DRAW_TWO}; for(ValueType v:vals){deck.add(new Card(color,v)); deck.add(new Card(color,v));}} for(int i=0;i<4;i++){deck.add(new Card(ColorType.WILD,ValueType.WILD)); deck.add(new Card(ColorType.WILD,ValueType.WILD_DRAW_FOUR));}}
        void shuffleDeck(){List<Card> cards=new ArrayList<>(deck); Collections.shuffle(cards); deck.clear(); cards.forEach(deck::add);} void dealInitialCards(){for(int i=0;i<INITIAL_CARDS;i++)for(Player p:players)p.hand.add(draw());}
        void initDiscard(){do{topCard=draw();}while(topCard.color==ColorType.WILD||topCard.value==ValueType.DRAW_TWO||topCard.value==ValueType.REVERSE||topCard.value==ValueType.SKIP); currentColor=topCard.color; discard.push(topCard);} Card draw(){if(deck.isEmpty())reshuffleFromDiscard(); return deck.removeFirst();}
        void reshuffleFromDiscard(){Card top=discard.pop(); List<Card> rest=new ArrayList<>(discard); Collections.shuffle(rest); deck.addAll(rest); discard.clear(); discard.push(top);} Card drawCardForCurrentPlayer(){Card c=draw(); players.get(currentPlayer).hand.add(c); return c;}
        boolean canPlay(Card card, Card top, ColorType activeColor){if(card.color==ColorType.WILD){if(card.value==ValueType.WILD_DRAW_FOUR){Player p=players.get(currentPlayer); boolean hasActive=p.hand.stream().anyMatch(c->c.color==activeColor); return !hasActive;} return true;} return card.color==activeColor||card.value==top.value;}
        void playCard(int pIdx,int hIdx,ColorType chosen){Card card=players.get(pIdx).hand.remove(hIdx); discard.push(card); topCard=card; currentColor=card.color==ColorType.WILD?chosen:card.color; applyEffect(card);} void applyEffect(Card card){switch(card.value){case SKIP->advanceTurn(2); case REVERSE->{if(players.size()==2)advanceTurn(2); else {direction*=-1; advanceTurn(1);}} case DRAW_TWO->{int n=nextPlayerIndex(1); players.get(n).hand.add(draw()); players.get(n).hand.add(draw()); advanceTurn(2);} case WILD_DRAW_FOUR->{int n=nextPlayerIndex(1); for(int i=0;i<4;i++)players.get(n).hand.add(draw()); advanceTurn(2);} default->advanceTurn(1);}}
        int nextPlayerIndex(int offset){int s=players.size(), idx=currentPlayer; for(int i=0;i<offset;i++)idx=(idx+direction+s)%s; return idx;} void advanceTurn(int steps){currentPlayer=nextPlayerIndex(steps);} boolean checkWinAndHandle(int idx, java.util.function.Consumer<String> logger){Player p=players.get(idx); if(p.hand.size()==1)logger.accept(p.name+" dice UNO!"); if(p.hand.isEmpty()){logger.accept(p.name+" ha vinto!"); return true;} return false;} boolean isGameOver(){return players.stream().anyMatch(p->p.hand.isEmpty());}
    }
}
