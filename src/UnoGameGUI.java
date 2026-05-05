import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class UnoGameGUI extends JFrame {
    private static final int INITIAL_CARDS = 7;

    private final GameState game;
    private final JLabel topCardLabel = new JLabel();
    private final JLabel currentPlayerLabel = new JLabel();
    private final JLabel directionLabel = new JLabel();
    private final JTextArea logArea = new JTextArea(10, 40);
    private final JPanel handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JButton drawButton = new JButton("Pesca Carta");

    public UnoGameGUI(int totalPlayers) {
        super("UNO - Java Swing");
        this.game = new GameState(totalPlayers);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new GridLayout(3, 1));
        header.setBorder(new EmptyBorder(10, 10, 0, 10));
        header.add(topCardLabel);
        header.add(currentPlayerLabel);
        header.add(directionLabel);
        add(header, BorderLayout.NORTH);

        handPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane handScroll = new JScrollPane(handPanel);
        handScroll.setPreferredSize(new Dimension(900, 180));
        add(handScroll, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        logArea.setEditable(false);
        rightPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        drawButton.addActionListener(e -> onDrawClicked());
        rightPanel.add(drawButton, BorderLayout.SOUTH);
        rightPanel.setBorder(new EmptyBorder(10, 0, 10, 10));
        add(rightPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        refreshUI();

        if (game.currentPlayer != 0) {
            SwingUtilities.invokeLater(this::botLoop);
        }
    }

    private void onDrawClicked() {
        if (game.isGameOver()) return;
        if (game.currentPlayer != 0) return;

        Card drawn = game.drawCardForCurrentPlayer();
        log("Hai pescato: " + drawn);

        if (game.canPlay(drawn, game.topCard, game.currentColor)) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Vuoi giocare subito la carta pescata?\n" + drawn,
                    "Giocare carta pescata",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                int idx = game.players.get(0).hand.size() - 1;
                playHumanCard(idx);
                return;
            }
        }

        game.advanceTurn(1);
        refreshUI();
        botLoop();
    }

    private void playHumanCard(int handIndex) {
        if (game.isGameOver()) return;
        if (game.currentPlayer != 0) return;

        Card card = game.players.get(0).hand.get(handIndex);
        if (!game.canPlay(card, game.topCard, game.currentColor)) {
            JOptionPane.showMessageDialog(this, "Carta non giocabile ora.");
            return;
        }

        ColorType chosen = null;
        if (card.color == ColorType.WILD) {
            chosen = askColor();
            if (chosen == null) return;
        }

        game.playCard(0, handIndex, chosen);
        log("Hai giocato: " + card + (chosen != null ? " (colore: " + chosen + ")" : ""));

        if (game.checkWinAndHandle(0, this::log)) {
            refreshUI();
            return;
        }

        refreshUI();
        botLoop();
    }

    private ColorType askColor() {
        Object[] options = {"RED", "YELLOW", "GREEN", "BLUE"};
        Object choice = JOptionPane.showInputDialog(this, "Scegli un colore", "Carta Jolly",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == null) return null;
        return ColorType.valueOf(choice.toString());
    }

    private void botLoop() {
        while (!game.isGameOver() && game.currentPlayer != 0) {
            Player bot = game.players.get(game.currentPlayer);
            List<Integer> playable = new ArrayList<>();
            for (int i = 0; i < bot.hand.size(); i++) {
                if (game.canPlay(bot.hand.get(i), game.topCard, game.currentColor)) playable.add(i);
            }

            if (playable.isEmpty()) {
                Card drawn = game.drawCardForCurrentPlayer();
                log(bot.name + " pesca una carta.");
                if (game.canPlay(drawn, game.topCard, game.currentColor)) {
                    int idx = bot.hand.size() - 1;
                    Card c = bot.hand.get(idx);
                    ColorType chosen = c.color == ColorType.WILD ? bot.chooseColor() : null;
                    game.playCard(game.currentPlayer, idx, chosen);
                    log(bot.name + " gioca la carta pescata: " + c + (chosen != null ? " -> " + chosen : ""));
                    if (game.checkWinAndHandle(game.currentPlayer, this::log)) break;
                } else {
                    game.advanceTurn(1);
                }
            } else {
                int idx = playable.get(new Random().nextInt(playable.size()));
                Card c = bot.hand.get(idx);
                ColorType chosen = c.color == ColorType.WILD ? bot.chooseColor() : null;
                game.playCard(game.currentPlayer, idx, chosen);
                log(bot.name + " gioca: " + c + (chosen != null ? " -> " + chosen : ""));
                if (game.checkWinAndHandle(game.currentPlayer, this::log)) break;
            }

            refreshUI();
            try { Thread.sleep(350); } catch (InterruptedException ignored) {}
        }

        refreshUI();
    }

    private void refreshUI() {
        topCardLabel.setText("Carta in cima: " + game.topCard + " | Colore attivo: " + game.currentColor);
        currentPlayerLabel.setText("Turno: " + game.players.get(game.currentPlayer).name);
        directionLabel.setText("Direzione: " + (game.direction == 1 ? "oraria" : "antioraria"));

        handPanel.removeAll();
        List<Card> hand = game.players.get(0).hand;
        for (int i = 0; i < hand.size(); i++) {
            int idx = i;
            JButton btn = new JButton(hand.get(i).toString());
            btn.addActionListener(e -> playHumanCard(idx));
            handPanel.add(btn);
        }

        drawButton.setEnabled(!game.isGameOver() && game.currentPlayer == 0);
        handPanel.revalidate();
        handPanel.repaint();

        if (game.isGameOver()) {
            String winner = game.players.stream().filter(p -> p.hand.isEmpty()).findFirst().map(p -> p.name).orElse("N/A");
            JOptionPane.showMessageDialog(this, "Partita finita! Vincitore: " + winner);
        }
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Integer[] opts = {2, 3, 4, 5, 6};
            Integer players = (Integer) JOptionPane.showInputDialog(null,
                    "Numero totale giocatori (2-6):",
                    "Nuova partita UNO",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    opts,
                    4);
            if (players == null) System.exit(0);
            new UnoGameGUI(players).setVisible(true);
        });
    }

    enum ColorType { RED, YELLOW, GREEN, BLUE, WILD }

    enum ValueType {
        ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE,
        SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR
    }

    static class Card {
        final ColorType color;
        final ValueType value;

        Card(ColorType color, ValueType value) {
            this.color = color;
            this.value = value;
        }

        public String toString() {
            return color == ColorType.WILD ? value.name() : color.name() + " " + value.name();
        }
    }

    static class Player {
        final String name;
        final List<Card> hand = new ArrayList<>();

        Player(String name) { this.name = name; }

        ColorType chooseColor() {
            Map<ColorType, Long> counts = hand.stream()
                    .filter(c -> c.color != ColorType.WILD)
                    .collect(Collectors.groupingBy(c -> c.color, Collectors.counting()));
            return Arrays.asList(ColorType.RED, ColorType.YELLOW, ColorType.GREEN, ColorType.BLUE)
                    .stream().max(Comparator.comparingLong(c -> counts.getOrDefault(c, 0L))).orElse(ColorType.RED);
        }
    }

    static class GameState {
        final List<Player> players = new ArrayList<>();
        final Deque<Card> deck = new ArrayDeque<>();
        final Deque<Card> discard = new ArrayDeque<>();
        int currentPlayer = 0;
        int direction = 1;
        Card topCard;
        ColorType currentColor;

        GameState(int totalPlayers) {
            for (int i = 0; i < totalPlayers; i++) {
                players.add(new Player(i == 0 ? "Tu" : "Bot " + i));
            }
            buildDeck();
            shuffleDeck();
            dealInitialCards();
            initDiscard();
        }

        void buildDeck() {
            for (ColorType color : Arrays.asList(ColorType.RED, ColorType.YELLOW, ColorType.GREEN, ColorType.BLUE)) {
                deck.add(new Card(color, ValueType.ZERO));
                ValueType[] vals = {ValueType.ONE, ValueType.TWO, ValueType.THREE, ValueType.FOUR, ValueType.FIVE,
                        ValueType.SIX, ValueType.SEVEN, ValueType.EIGHT, ValueType.NINE,
                        ValueType.SKIP, ValueType.REVERSE, ValueType.DRAW_TWO};
                for (ValueType v : vals) {
                    deck.add(new Card(color, v));
                    deck.add(new Card(color, v));
                }
            }
            for (int i = 0; i < 4; i++) {
                deck.add(new Card(ColorType.WILD, ValueType.WILD));
                deck.add(new Card(ColorType.WILD, ValueType.WILD_DRAW_FOUR));
            }
        }

        void shuffleDeck() {
            List<Card> cards = new ArrayList<>(deck);
            Collections.shuffle(cards);
            deck.clear();
            cards.forEach(deck::add);
        }

        void dealInitialCards() {
            for (int i = 0; i < INITIAL_CARDS; i++) {
                for (Player p : players) p.hand.add(draw());
            }
        }

        void initDiscard() {
            do {
                topCard = draw();
            } while (topCard.color == ColorType.WILD || topCard.value == ValueType.DRAW_TWO || topCard.value == ValueType.REVERSE || topCard.value == ValueType.SKIP);
            currentColor = topCard.color;
            discard.push(topCard);
        }

        Card draw() {
            if (deck.isEmpty()) reshuffleFromDiscard();
            return deck.removeFirst();
        }

        void reshuffleFromDiscard() {
            Card top = discard.pop();
            List<Card> rest = new ArrayList<>(discard);
            Collections.shuffle(rest);
            deck.addAll(rest);
            discard.clear();
            discard.push(top);
        }

        Card drawCardForCurrentPlayer() {
            Card c = draw();
            players.get(currentPlayer).hand.add(c);
            return c;
        }

        boolean canPlay(Card card, Card top, ColorType activeColor) {
            if (card.color == ColorType.WILD) {
                if (card.value == ValueType.WILD_DRAW_FOUR) {
                    // Regola ufficiale: consentita solo se non hai carte del colore attivo.
                    Player p = players.get(currentPlayer);
                    boolean hasActiveColor = p.hand.stream().anyMatch(c -> c.color == activeColor);
                    return !hasActiveColor;
                }
                return true;
            }
            return card.color == activeColor || card.value == top.value;
        }

        void playCard(int playerIdx, int handIndex, ColorType chosenColor) {
            Card card = players.get(playerIdx).hand.remove(handIndex);
            discard.push(card);
            topCard = card;
            currentColor = (card.color == ColorType.WILD) ? chosenColor : card.color;

            applyEffect(card);
        }

        void applyEffect(Card card) {
            switch (card.value) {
                case SKIP -> advanceTurn(2);
                case REVERSE -> {
                    if (players.size() == 2) {
                        advanceTurn(2);
                    } else {
                        direction *= -1;
                        advanceTurn(1);
                    }
                }
                case DRAW_TWO -> {
                    int next = nextPlayerIndex(1);
                    players.get(next).hand.add(draw());
                    players.get(next).hand.add(draw());
                    advanceTurn(2);
                }
                case WILD_DRAW_FOUR -> {
                    int next = nextPlayerIndex(1);
                    for (int i = 0; i < 4; i++) players.get(next).hand.add(draw());
                    advanceTurn(2);
                }
                default -> advanceTurn(1);
            }
        }

        int nextPlayerIndex(int offset) {
            int size = players.size();
            int idx = currentPlayer;
            for (int i = 0; i < offset; i++) idx = (idx + direction + size) % size;
            return idx;
        }

        void advanceTurn(int steps) {
            currentPlayer = nextPlayerIndex(steps);
        }

        boolean checkWinAndHandle(int playerIdx, java.util.function.Consumer<String> logger) {
            Player p = players.get(playerIdx);
            if (p.hand.size() == 1) logger.accept(p.name + " dice UNO!");
            if (p.hand.isEmpty()) {
                logger.accept(p.name + " ha vinto la partita!");
                return true;
            }
            return false;
        }

        boolean isGameOver() {
            return players.stream().anyMatch(p -> p.hand.isEmpty());
        }
    }
}
