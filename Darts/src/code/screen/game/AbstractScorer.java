package code.screen.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import code.bean.PlayerAvatar;
import code.bean.ScrollTableDartsGame;
import code.db.PlayerEntity;
import code.utils.DartsColour;
import code.utils.DartsRegistry;
import util.TableUtil.DefaultModel;

public abstract class AbstractScorer extends JPanel
 									 implements DartsRegistry
{
	protected boolean human = false;
	protected int finishPos = -1;
	
	protected DefaultModel model = new DefaultModel();
	
	public AbstractScorer()
	{
		setLayout(new BorderLayout(0, 0));
		setPreferredSize(new Dimension(180, 600));
		
		add(tableScores, BorderLayout.CENTER);
		add(panelNorth, BorderLayout.NORTH);
		panelNorth.setLayout(new BorderLayout(0, 0));
		panelNorth.add(panel_1, BorderLayout.NORTH);
		panel_1.setLayout(new BorderLayout(0, 0));
		panel_1.add(lblName, BorderLayout.NORTH);
		lblName.setHorizontalAlignment(SwingConstants.CENTER);
		lblName.setFont(new Font("Trebuchet MS", Font.PLAIN, 16));
		panel_2.setBorder(new EmptyBorder(5, 15, 5, 15));
		panel_1.add(panel_2, BorderLayout.SOUTH);
		panel_2.setLayout(new BorderLayout(0, 0));
		panel_2.add(lblAvatar, BorderLayout.NORTH);
		add(panelSouth, BorderLayout.SOUTH);
		panelSouth.setLayout(new BorderLayout(0, 0));
		panelSouth.add(lblResult);
		lblResult.setOpaque(true);
		lblResult.setFont(new Font("Trebuchet MS", Font.PLAIN, 22));
		lblResult.setHorizontalAlignment(SwingConstants.CENTER);
		
		tableScores.setFillsViewportHeight(false);
		tableScores.setShowRowCount(false);
		tableScores.disableSorting();
		
		lblAvatar.setReadOnly(true);
	}
	
	protected final JLabel lblName = new JLabel();
	protected final ScrollTableDartsGame tableScores = new ScrollTableDartsGame();
	protected final JLabel lblResult = new JLabel("X Darts (D20)");
	protected final JPanel panelNorth = new JPanel();
	private final JPanel panel_1 = new JPanel();
	private final PlayerAvatar lblAvatar = new PlayerAvatar();
	private final JPanel panel_2 = new JPanel();
	protected final JPanel panelSouth = new JPanel();
	
	/**
	 * Abstract methods
	 */
	public abstract int getNumberOfColumns();
	public abstract void initImpl(String gameParams);

	/**
	 * Instance methods
	 */
	public void init(PlayerEntity player, String gameParams)
	{
		lblName.setVisible(player != null);
		lblAvatar.setVisible(player != null);
		panel_2.setVisible(player != null);
		
		//Sometimes we pass in a null player for the purpose of showing stats
		if (player != null)
		{
			String playerName = player.getName();
			lblName.setText(playerName);
			lblAvatar.init(player, false);
			
			human = player.getStrategy() == -1;
		}
		
		lblResult.setText("");
		
		//TableModel
		tableScores.setRowHeight(25);
		for (int i=0; i<getNumberOfColumns(); i++)
		{
			model.addColumn("");
		}
		tableScores.setModel(model);
		
		initImpl(gameParams);
	}
	
	public void addRow(Object[] row)
	{
		model.addRow(row);
		tableScores.scrollToBottom();
	}
	public Object[] getEmptyRow()
	{
		Object[] row = new Object[getNumberOfColumns()];
		for (int i=0; i<getNumberOfColumns(); i++)
		{
			row[i] = null;
		}
		
		return row;
	}
	public void reset()
	{
		human = false;
		lblName.setText("");
	}
	public boolean getHuman()
	{
		return human;
	}
	
	public String getPlayerName()
	{
		return lblName.getText();
	}
	
	public boolean canBeAssigned()
	{
		return isVisible()
		  && getPlayerName().isEmpty();
	}	
	
	public void setSelected(boolean selected)
	{
		Color color = selected?Color.RED:Color.BLACK;
		lblName.setForeground(color);
	}
	
	public void updateResultColourForPosition(int pos)
	{
		DartsColour.setFgAndBgColoursForPosition(lblResult, pos);
	}
	
	
	/**
	 * Default method, overridden by Round the Clock
	 */
	public int getNumberOfColumnsForAddingNewDart()
	{
		return getNumberOfColumns() - 1;
	}
	
	/*public void assignAsterisk()
	{
		String text = lblName.getText();
		text += "*";
		lblName.setText(text);
	}*/
	
	public DefaultModel getTableModel()
	{
		return model;
	}
}
