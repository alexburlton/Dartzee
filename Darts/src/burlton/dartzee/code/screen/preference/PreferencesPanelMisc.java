package burlton.dartzee.code.screen.preference;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import burlton.desktopcore.code.bean.NumberField;
import burlton.dartzee.code.bean.SliderAiSpeed;
import burlton.dartzee.code.utils.PreferenceUtil;
import net.miginfocom.swing.MigLayout;

public class PreferencesPanelMisc extends AbstractPreferencesPanel
{
	public PreferencesPanelMisc()
	{
		nfLeaderboardSize.setColumns(10);
		setLayout(new MigLayout("", "[][grow][]", "[][][][][][]"));
		
		add(lblDefaultAiSpeed, "cell 0 0");
		add(slider, "cell 1 0");
		
		add(lblRowsToShow, "cell 0 1,alignx leading");
		
		add(nfLeaderboardSize, "cell 1 1,alignx leading");
		add(chckbxAiAutomaticallyFinish, "flowx,cell 0 2");
		
		add(chckbxCheckForUpdates, "flowx,cell 0 3");
		
		add(chckbxShowAnimations, "cell 0 4");
		
		add(chckbxPreloadResources, "cell 0 5");
	}
	
	private final JLabel lblDefaultAiSpeed = new JLabel("Default AI speed");
	private final SliderAiSpeed slider = new SliderAiSpeed(false);
	private final JCheckBox chckbxAiAutomaticallyFinish = new JCheckBox("AI automatically finish");
	private final JCheckBox chckbxCheckForUpdates = new JCheckBox("Automatically check for updates");
	private final JLabel lblRowsToShow = new JLabel("Rows to show on Leaderboards");
	private final NumberField nfLeaderboardSize = new NumberField(10, 200);
	private final JCheckBox chckbxShowAnimations = new JCheckBox("Show animations");
	private final JCheckBox chckbxPreloadResources = new JCheckBox("Pre-load resources (recommended)");
	
	@Override
	public void refresh(boolean useDefaults)
	{
		int aiSpd = PreferenceUtil.getIntValue(PREFERENCES_INT_AI_SPEED, useDefaults);
		slider.setValue(aiSpd);
		
		int leaderboardSize = PreferenceUtil.getIntValue(PREFERENCES_INT_LEADERBOARD_SIZE, useDefaults);
		nfLeaderboardSize.setValue(leaderboardSize);
		
		boolean aiAuto = PreferenceUtil.getBooleanValue(PREFERENCES_BOOLEAN_AI_AUTO_CONTINUE, useDefaults);
		chckbxAiAutomaticallyFinish.setSelected(aiAuto);
		
		boolean checkForUpdates = PreferenceUtil.getBooleanValue(PREFERENCES_BOOLEAN_CHECK_FOR_UPDATES, useDefaults);
		chckbxCheckForUpdates.setSelected(checkForUpdates);
		
		boolean showAnimations = PreferenceUtil.getBooleanValue(PREFERENCES_BOOLEAN_SHOW_ANIMATIONS, useDefaults);
		chckbxShowAnimations.setSelected(showAnimations);
		
		boolean preLoad = PreferenceUtil.getBooleanValue(PREFERENCES_BOOLEAN_PRE_LOAD_RESOURCES, useDefaults);
		chckbxPreloadResources.setSelected(preLoad);
		
	}

	@Override
	public boolean valid()
	{
		return true;
	}

	@Override
	public void save()
	{
		int aiSpd = slider.getValue();
		PreferenceUtil.saveInt(PREFERENCES_INT_AI_SPEED, aiSpd);
		
		int leaderboardSize = nfLeaderboardSize.getNumber();
		PreferenceUtil.saveInt(PREFERENCES_INT_LEADERBOARD_SIZE, leaderboardSize);
		
		boolean aiAuto = chckbxAiAutomaticallyFinish.isSelected();
		PreferenceUtil.saveBoolean(PREFERENCES_BOOLEAN_AI_AUTO_CONTINUE, aiAuto);
		
		boolean checkForUpdates = chckbxCheckForUpdates.isSelected();
		PreferenceUtil.saveBoolean(PREFERENCES_BOOLEAN_CHECK_FOR_UPDATES, checkForUpdates);
		
		boolean showAnimations = chckbxShowAnimations.isSelected();
		PreferenceUtil.saveBoolean(PREFERENCES_BOOLEAN_SHOW_ANIMATIONS, showAnimations);
		
		boolean preLoad = chckbxPreloadResources.isSelected();
		PreferenceUtil.saveBoolean(PREFERENCES_BOOLEAN_PRE_LOAD_RESOURCES, preLoad);
	}

}