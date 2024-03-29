/*
 * Copyright (c) 2017, Tyler <http://github.com/tylerthardy>
 * Copyright (c) 2022, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package bbp.equipmentscreenshot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Point;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varbit;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayUtil;

class EquipmentRunepouchOverlay
{
	private static final int NUM_SLOTS = 4;
	private static final int[] AMOUNT_VARBITS = {
			Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3, Varbits.RUNE_POUCH_AMOUNT4
	};
	private static final int[] RUNE_VARBITS = {
			Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3, Varbits.RUNE_POUCH_RUNE4
	};
	private static final Dimension IMAGE_SIZE = new Dimension(11, 11);
	private BufferedImage[] runeIcons = new BufferedImage[0];

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private EquipmentScreenshotConfig config;

	@Inject
	private EquipmentScreenshotUtil util;

	void renderRunepouchOverlay(Graphics2D graphics, Point location)
	{
		boolean showIcons = true;
		if (configManager.getConfiguration("runepouch", "runeicons", Boolean.class) != null)
			showIcons = configManager.getConfiguration("runepouch", "runeicons", Boolean.class);

		final int[] runeIds = new int[NUM_SLOTS];
		final int[] amounts = new int[NUM_SLOTS];
		final EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		int num = 0;

		for (int i = 0; i < NUM_SLOTS; i++)
		{
			@Varbit int amountVarbit = AMOUNT_VARBITS[i];
			int amount = client.getVarbitValue(amountVarbit);
			amounts[i] = amount;

			@Varbit int runeVarbit = RUNE_VARBITS[i];
			int runeId = client.getVarbitValue(runeVarbit);
			runeIds[i] = runeId;

			if (runeId != 0 && amount > 0)
			{
				++num;
			}
		}

		if (num == 0)
		{
			return;
		}

		if (num < 4)
		{
			renderList(graphics, location, runepouchEnum, runeIds, amounts);
		}
		else
		{
			renderGrid(graphics, location, runepouchEnum, runeIds, amounts);
		}
	}

	private void renderList(Graphics2D graphics, Point location, EnumComposition runepouchEnum, int[] runeIds, int[] amounts)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());

		int runeNum = -1;
		for (int i = 0; i < NUM_SLOTS; ++i)
		{
			final int runeId = runeIds[i];
			final int amount = amounts[i];

			if (runeId == 0 || amount <= 0)
			{
				continue;
			}

			++runeNum;

			final String text = formatNumber(amount);
			final int textX = location.getX() + 11;
			final int textY = location.getY() + 12 + (graphics.getFontMetrics().getHeight() - 1) * runeNum;

			graphics.setColor(Color.BLACK);
			graphics.drawString(text, textX + 1, textY + 1);

			graphics.setColor(config.fontColor());
			graphics.drawString(text, textX, textY);

			BufferedImage image = getRuneImage(runepouchEnum, runeId);
			if (image != null)
			{
				OverlayUtil.renderImageLocation(graphics,
						new Point(
								location.getX() - 1,
								location.getY() + graphics.getFontMetrics().getHeight() * runeNum - 1
						),
						image);
			}
		}
	}

	private void renderGrid(Graphics2D graphics, Point location, EnumComposition runepouchEnum, int[] runeIds, int[] amounts)
	{
		for (int i = 0; i < NUM_SLOTS; ++i)
		{
			final int runeId = runeIds[i];
			final int amount = amounts[i];

			if (runeId == -1 || amount <= 0)
			{
				continue;
			}

			final int iconX = location.getX() + 2 + (i == 1 || i == 3 ? IMAGE_SIZE.width + 2 /* pad */ + 2 /* bar offset */ : 0);
			final int iconY = location.getY() + 5 + (i >= 2 ? IMAGE_SIZE.height + 2 /* pad */ : 0);

			BufferedImage image = getRuneImage(runepouchEnum, runeId);
			if (image != null)
			{
				OverlayUtil.renderImageLocation(graphics,
						new Point(iconX, iconY),
						image);
			}

			final int height;
			final Color color;

			if (amount < 1000)
			{
				// with <1k runes, the bar is shown in red with 100 increments
				height = amount / 100;
				color = Color.RED;
			}
			else
			{
				// with >=1k runes, the bar is shown in 1000 increments capped at 10k
				height = Math.min(10, amount / 1000);
				color = Color.GREEN;
			}

			graphics.setColor(color);
			graphics.fillRect(iconX + IMAGE_SIZE.width, iconY + 1 + (10 - height), 2, height);
		}
	}

	private BufferedImage getRuneImage(EnumComposition runepouchEnum, int runeId)
	{
		if (runeId < runeIcons.length && runeIcons[runeId] != null)
		{
			return runeIcons[runeId];
		}

		BufferedImage runeImg = itemManager.getImage(runepouchEnum.getIntValue(runeId));
		if (runeImg == null)
		{
			return null;
		}

		BufferedImage resizedImg = new BufferedImage(IMAGE_SIZE.width, IMAGE_SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = resizedImg.createGraphics();
		g.drawImage(runeImg, 0, 0, IMAGE_SIZE.width, IMAGE_SIZE.height, null);
		g.dispose();

		if (runeId >= runeIcons.length)
		{
			runeIcons = Arrays.copyOf(runeIcons, runeId + 1);
		}
		runeIcons[runeId] = resizedImg;
		return resizedImg;
	}

	private static String formatNumber(int amount)
	{
		return amount < 1000 ? String.valueOf(amount) : amount / 1000 + "K";
	}
}

