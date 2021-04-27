package net.runelite.client.plugins.elbreakhandler.ui;

import net.runelite.client.plugins.elbreakhandler.ElBreakHandler;
import net.runelite.client.plugins.elbreakhandler.ElBreakHandlerPlugin;
import static net.runelite.client.plugins.elbreakhandler.ElBreakHandlerPlugin.CONFIG_GROUP;
import static net.runelite.client.plugins.elbreakhandler.ElBreakHandlerPlugin.sanitizedName;
import static net.runelite.client.plugins.elbreakhandler.ui.ElBreakHandlerPanel.BACKGROUND_COLOR;
import static net.runelite.client.plugins.elbreakhandler.ui.ElBreakHandlerPanel.NORMAL_FONT;
import static net.runelite.client.plugins.elbreakhandler.ui.ElBreakHandlerPanel.PANEL_BACKGROUND_COLOR;
import static net.runelite.client.plugins.elbreakhandler.ui.ElBreakHandlerPanel.SMALL_FONT;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class ElBreakHandlerStatusPanel extends JPanel
{
	private final ConfigManager configManager;
	private final ElBreakHandlerPlugin elBreakHandlerPlugin;
	private final ElBreakHandler elBreakHandler;
	private final Plugin plugin;

	private final JPanel contentPanel = new JPanel(new GridBagLayout());
	private final JPanel infoWrapper = new JPanel(new BorderLayout());
	private final JPanel infoPanel = new JPanel(new GridBagLayout());
	private final JPanel extraDataPanel = new JPanel(new GridBagLayout());
	private final JLabel timeLabel = new JLabel();
	private final JLabel runtimeLabel = new JLabel();
	private final JLabel breaksLabel = new JLabel();

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(PluginPanel.PANEL_WIDTH, super.getPreferredSize().height);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	ElBreakHandlerStatusPanel(ElBreakHandlerPlugin elBreakHandlerPlugin, ElBreakHandler elBreakHandler, Plugin plugin)
	{
		this.configManager = elBreakHandlerPlugin.getConfigManager();
		this.elBreakHandlerPlugin = elBreakHandlerPlugin;
		this.elBreakHandler = elBreakHandler;
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(BACKGROUND_COLOR);

		elBreakHandler
			.configChanged
			.subscribe(this::onConfigChanged);

		if (elBreakHandlerPlugin.disposables.containsKey(plugin))
		{
			Disposable seconds = elBreakHandlerPlugin.disposables.get(plugin);

			if (!seconds.isDisposed())
			{
				seconds.dispose();
			}
		}

		Disposable secondsDisposable = Observable
			.interval(500, TimeUnit.MILLISECONDS)
			.subscribe(this::milliseconds);

		elBreakHandlerPlugin.disposables.put(plugin, secondsDisposable);

		Disposable extraDataDisposable = elBreakHandler
			.getExtraDataObservable()
			.subscribe((data) -> SwingUtil.syncExec(() -> this.extraData(data)));

		elBreakHandlerPlugin.disposables.put(plugin, extraDataDisposable);

		init();
	}

	public static String formatDuration(Duration duration)
	{
		long seconds = duration.getSeconds();
		long absSeconds = Math.abs(seconds);

		return String.format(
			"%02d:%02d:%02d",
			absSeconds / 3600,
			(absSeconds % 3600) / 60,
			absSeconds % 60);
	}

	private void milliseconds(long ignored)
	{
		Instant now = Instant.now();

		Map<Plugin, Instant> startTimes = elBreakHandler.getStartTimes();

		if (startTimes.containsKey(plugin))
		{
			Duration duration = Duration.between(elBreakHandler.getStartTimes().get(plugin), now);
			runtimeLabel.setText(formatDuration(duration));
		}

		Map<Plugin, Integer> breaks = elBreakHandler.getAmountOfBreaks();

		if (breaks.containsKey(plugin))
		{
			breaksLabel.setText(String.valueOf(elBreakHandler.getAmountOfBreaks().get(plugin)));
		}

		if (!elBreakHandler.isBreakPlanned(plugin) && !elBreakHandler.isBreakActive(plugin))
		{
			timeLabel.setText("00:00:00");
			return;
		}

		if (elBreakHandler.isBreakPlanned(plugin))
		{
			Instant breaktime = elBreakHandler.getPlannedBreak(plugin);

			if (now.isAfter(breaktime))
			{
				timeLabel.setText("Waiting for plugin");
			}
			else
			{
				Duration duration = Duration.between(now, breaktime);
				timeLabel.setText(formatDuration(duration));
			}
		}
		else if (elBreakHandler.isBreakActive(plugin))
		{
			Instant breaktime = elBreakHandler.getActiveBreak(plugin);

			if (now.isAfter(breaktime))
			{
				timeLabel.setText("00:00:00");
			}
			else
			{
				Duration duration = Duration.between(now, breaktime);
				timeLabel.setText(formatDuration(duration));
			}
		}
		else
		{
			timeLabel.setText("-");
		}

		boolean enabled = Boolean.parseBoolean(configManager.getConfiguration(ElBreakHandlerPlugin.CONFIG_GROUP, sanitizedName(plugin) + "-enabled"));

		if (enabled && elBreakHandler.getPlugins().get(plugin) && elBreakHandlerPlugin.isValidBreak(plugin) && !elBreakHandler.isBreakPlanned(plugin) && !elBreakHandler.isBreakActive(plugin))
		{
			elBreakHandlerPlugin.scheduleBreak(plugin);
		}
	}

	private void extraData(Map<Plugin, Map<String, String>> extraData)
	{
		Map<String, String> pluginData = extraData.get(plugin);

		if (pluginData.isEmpty())
		{
			return;
		}

		extraDataPanel.setBackground(BACKGROUND_COLOR);
		extraDataPanel.setBorder(new CompoundBorder(
				new CompoundBorder(
						BorderFactory.createMatteBorder(0, 0, 1, 0, PANEL_BACKGROUND_COLOR),
						BorderFactory.createLineBorder(BACKGROUND_COLOR)
				), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		extraDataPanel.removeAll();

		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(2, 0, 2, 0);
		c.gridx = 0;
		c.gridy = 0;

		pluginData.computeIfPresent("State", (k, v) -> {
			JLabel keyLabel = new JLabel(k);
			c.gridwidth = 2;
			c.weightx = 2;
			c.gridx = 0;
			c.gridy = 0;
			extraDataPanel.add(keyLabel, c);

			JLabel valueLabel = new JLabel(v);
			c.weightx = 1;
			c.gridx = 0;
			c.gridy = 1;
			extraDataPanel.add(valueLabel, c);

			c.gridx += 1;

			return null;
		});

		int index = 0;
		for (Map.Entry<String, String> data : pluginData.entrySet())
		{
			index++;

			JLabel keyLabel = new JLabel(data.getKey());
			JLabel valueLabel = new JLabel(data.getValue());

			boolean bump = index % 2 == 0;

			if (!bump && index == pluginData.size())
			{
				c.gridwidth = 2;
			}
			else
			{
				c.gridwidth = 1;
			}

			c.gridx = bump ? 1 : 0;

			c.weightx = 2;
			if (bump)
			{
				c.gridy -= 1;
			}
			else
			{
				c.gridy += 1;
			}
			extraDataPanel.add(keyLabel, c);

			c.weightx = 1;
			c.gridy += 1;
			extraDataPanel.add(valueLabel, c);
		}

		extraDataPanel.revalidate();
		extraDataPanel.repaint();
	}

	private void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged == null || !configChanged.getGroup().equals(CONFIG_GROUP) || !configChanged.getKey().contains(sanitizedName(plugin)))
		{
			return;
		}

		if (configChanged.getKey().contains("enabled") && configChanged.getNewValue().equals("false"))
		{
			elBreakHandler.removePlannedBreak(plugin);
		}

		statusPanel();

		contentPanel.revalidate();
		contentPanel.repaint();
	}

	private void init()
	{
		timeLabel.setFont(new Font("", Font.PLAIN, 20));

		JPanel titleWrapper = new JPanel(new BorderLayout());
		titleWrapper.setBackground(BACKGROUND_COLOR);
		titleWrapper.setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, PANEL_BACKGROUND_COLOR),
			BorderFactory.createLineBorder(BACKGROUND_COLOR)
		));

		JLabel title = new JLabel();
		title.setText(plugin.getName());
		title.setFont(NORMAL_FONT);
		title.setPreferredSize(new Dimension(0, 24));
		title.setForeground(Color.WHITE);
		title.setBorder(new EmptyBorder(0, 8, 0, 0));

		JPanel titleActions = new JPanel(new BorderLayout(3, 0));
		titleActions.setBackground(BACKGROUND_COLOR);
		titleActions.setBorder(new EmptyBorder(0, 0, 0, 8));

		JLabel status = new JLabel();
		status.setText("running");
		status.setFont(SMALL_FONT.deriveFont(16f));
		status.setForeground(Color.GREEN);

		titleActions.add(status, BorderLayout.EAST);

		titleWrapper.add(title, BorderLayout.CENTER);
		titleWrapper.add(titleActions, BorderLayout.EAST);

		statusPanel();
		infoPanel();

		infoWrapper.add(infoPanel, BorderLayout.NORTH);
		infoWrapper.add(extraDataPanel, BorderLayout.SOUTH);

		add(titleWrapper, BorderLayout.NORTH);
		add(contentPanel, BorderLayout.CENTER);
		add(infoWrapper, BorderLayout.SOUTH);
	}

	private void statusPanel()
	{
		contentPanel.removeAll();

		contentPanel.setBackground(BACKGROUND_COLOR);
		contentPanel.setBorder(new CompoundBorder(
			new CompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, PANEL_BACKGROUND_COLOR),
				BorderFactory.createLineBorder(BACKGROUND_COLOR)
			), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		GridBagConstraints c = new GridBagConstraints();

		if (!elBreakHandler.getPlugins().get(plugin) && !elBreakHandler.isBreakActive(plugin))
		{
			c.insets = new Insets(2, 0, 2, 0);
			c.weightx = 0;
			c.gridx = 0;
			c.gridy = 0;
			contentPanel.add(new JLabel("The plugin will handle break timings"), c);

			return;
		}

		if (elBreakHandler.getPlugins().get(plugin))
		{
			boolean enabled = Boolean.parseBoolean(configManager.getConfiguration(ElBreakHandlerPlugin.CONFIG_GROUP, sanitizedName(plugin) + "-enabled"));

			if (!enabled)
			{
				c.insets = new Insets(2, 0, 2, 0);
				c.weightx = 0;
				c.gridx = 0;
				c.gridy = 0;
				contentPanel.add(new JLabel("Breaks are disabled for this plugin"), c);

				return;
			}

			if (elBreakHandler.getPlugins().get(plugin) && elBreakHandlerPlugin.isValidBreak(plugin) && !elBreakHandler.isBreakPlanned(plugin) && !elBreakHandler.isBreakActive(plugin))
			{
				elBreakHandlerPlugin.scheduleBreak(plugin);
			}
		}

		JLabel breaking = new JLabel();

		if (elBreakHandler.isBreakPlanned(plugin))
		{
			breaking.setText("Scheduled break in:");
		}
		else if (elBreakHandler.isBreakActive(plugin))
		{
			breaking.setText("Taking a break for:");
		}

		c.insets = new Insets(2, 0, 2, 0);
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 0;
		contentPanel.add(breaking, c);

		c.fill = GridBagConstraints.CENTER;
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 1;
		contentPanel.add(timeLabel, c);
	}

	private void infoPanel()
	{
		infoPanel.removeAll();

		infoPanel.setBackground(BACKGROUND_COLOR);
		infoPanel.setBorder(new CompoundBorder(
			new CompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, PANEL_BACKGROUND_COLOR),
				BorderFactory.createLineBorder(BACKGROUND_COLOR)
			), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(2, 0, 2, 0);
		c.weightx = 2;
		c.gridx = 0;
		c.gridy = 0;

		infoPanel.add(new JLabel("Total runtime"), c);

		c.insets = new Insets(2, 0, 2, 0);
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 1;

		infoPanel.add(runtimeLabel, c);

		c.insets = new Insets(2, 0, 2, 0);
		c.weightx = 2;
		c.gridx = 1;
		c.gridy = 0;

		infoPanel.add(new JLabel("Amount of breaks"), c);

		c.insets = new Insets(2, 0, 2, 0);
		c.weightx = 1;
		c.gridx = 1;
		c.gridy = 1;

		infoPanel.add(breaksLabel, c);
	}
}
