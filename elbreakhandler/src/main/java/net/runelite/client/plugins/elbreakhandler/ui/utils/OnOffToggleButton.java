package net.runelite.client.plugins.elbreakhandler.ui.utils;

import net.runelite.client.plugins.elbreakhandler.ElBreakHandlerPlugin;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

public class OnOffToggleButton extends JToggleButton
{
	private static final ImageIcon ON_SWITCHER;
	private static final ImageIcon OFF_SWITCHER;

	static
	{
		BufferedImage onSwitcher = ImageUtil.loadImageResource(ElBreakHandlerPlugin.class, "switcher_on.png");
		ON_SWITCHER = new ImageIcon(ImageUtil.recolorImage(onSwitcher, ColorScheme.BRAND_BLUE));
		OFF_SWITCHER = new ImageIcon(ImageUtil.flipImage(
			ImageUtil.luminanceScale(
				ImageUtil.grayscaleImage(onSwitcher),
				0.61f
			),
			true,
			false
		));
	}

	public OnOffToggleButton()
	{
		super(OFF_SWITCHER);
		setSelectedIcon(ON_SWITCHER);
		SwingUtil.removeButtonDecorations(this);
		setPreferredSize(new Dimension(25, 0));
		SwingUtil.addModalTooltip(this, "Disable", "Enable");
	}
}