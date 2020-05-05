package dartzee.screen

import dartzee.db.PlayerEntity
import dartzee.game.GameType
import dartzee.screen.stats.player.PlayerStatisticsScreen
import dartzee.stats.PlayerSummaryStats
import net.miginfocom.swing.MigLayout
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.TitledBorder

class PlayerSummaryPanel(private val player: PlayerEntity, private val gameType: GameType, stats: PlayerSummaryStats) : JPanel(), ActionListener
{
    private val nfGamesPlayed = JTextField("${stats.gamesPlayed}")
    private val nfGamesWon = JTextField("${stats.gamesWon}")
    private val nfBestGame = JTextField("${stats.bestScore}")
    private val btnViewStats = JButton("View Stats")
    private val lblP = JLabel("Played")
    private val lblW = JLabel("Won")
    private val lblHighScore = JLabel("High score")

    init
    {
        border = TitledBorder(null, gameType.getDescription(), TitledBorder.LEADING, TitledBorder.TOP, Font("Tahoma", Font.PLAIN, 20))
        layout = MigLayout("", "[][][][][][][][][grow][]", "[][][]")

        add(lblP, "cell 0 0")

        add(lblW, "cell 2 0,alignx leading")

        add(lblHighScore, "cell 4 0")
        nfGamesPlayed.isEditable = false
        add(nfGamesPlayed, "cell 0 1,growx")
        nfGamesPlayed.columns = 10
        val horizontalStrut = Box.createHorizontalStrut(20)
        add(horizontalStrut, "cell 1 1")
        nfGamesWon.isEditable = false
        add(nfGamesWon, "cell 2 1,growx")
        nfGamesWon.columns = 10

        val strutOne = Box.createHorizontalStrut(20)
        add(strutOne, "cell 3 1")
        nfBestGame.isEditable = false
        add(nfBestGame, "cell 4 1,growx")
        nfBestGame.columns = 10

        val strut2 = Box.createHorizontalStrut(20)
        add(strut2, "flowx,cell 8 1")
        btnViewStats.font = Font("Tahoma", Font.PLAIN, 16)
        add(btnViewStats, "cell 8 1,alignx center")

        btnViewStats.addActionListener(this)
        btnViewStats.isEnabled = stats.gamesPlayed > 0
    }

    override fun actionPerformed(arg0: ActionEvent)
    {
        if (arg0.source === btnViewStats)
        {
            val statsScrn = ScreenCache.get<PlayerStatisticsScreen>()
            statsScrn.setVariables(gameType, player)

            ScreenCache.switch(statsScrn)
        }
    }
}
