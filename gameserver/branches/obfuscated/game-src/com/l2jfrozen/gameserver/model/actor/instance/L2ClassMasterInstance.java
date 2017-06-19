/*
 * L2jFrozen Project - www.l2jfrozen.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.l2jfrozen.gameserver.model.actor.instance;

import com.l2jfrozen.CommonConfig;
import com.l2jfrozen.Config;
import com.l2jfrozen.gameserver.ai.CtrlIntention;
import com.l2jfrozen.gameserver.datatables.sql.CharTemplateTable;
import com.l2jfrozen.gameserver.datatables.sql.ItemTable;
import com.l2jfrozen.gameserver.model.base.ClassId;
import com.l2jfrozen.gameserver.model.base.ClassLevel;
import com.l2jfrozen.gameserver.model.base.PlayerClass;
import com.l2jfrozen.gameserver.model.quest.Quest;
import com.l2jfrozen.gameserver.network.SystemMessageId;
import com.l2jfrozen.gameserver.network.serverpackets.ActionFailed;
import com.l2jfrozen.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jfrozen.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jfrozen.gameserver.network.serverpackets.SystemMessage;
import com.l2jfrozen.gameserver.network.serverpackets.ValidateLocation;
import com.l2jfrozen.gameserver.templates.L2NpcTemplate;

import javolution.text.TextBuilder;

/**
 * This class ...
 * @version $Revision: 1.4.2.1.2.7 $ $Date: 2005/03/27 15:29:32 $
 */
public final class L2ClassMasterInstance extends L2FolkInstance
{
	
	/** The _instance. */
	private static L2ClassMasterInstance _instance;
	
	/**
	 * Instantiates a new l2 class master instance.
	 * @param objectId the object id
	 * @param template the template
	 */
	public L2ClassMasterInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		_instance = this;
	}
	
	/**
	 * Gets the single instance of L2ClassMasterInstance.
	 * @return single instance of L2ClassMasterInstance
	 */
	public static L2ClassMasterInstance getInstance()
	{
		
		return _instance;
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jfrozen.gameserver.model.actor.instance.L2FolkInstance#onAction(com.l2jfrozen.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (CommonConfig.DEBUG)
		{
			LOGGER.info("Class master activated!");
		}
		
		player.setLastFolkNPC(this);
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget() && !Config.ALLOW_REMOTE_CLASS_MASTERS)
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;
			
			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player) && !Config.ALLOW_REMOTE_CLASS_MASTERS)
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				ClassId classId = player.getClassId();
				int jobLevel = 0;
				final int level = player.getLevel();
				ClassLevel lvl = PlayerClass.values()[classId.getId()].getLevel();
				switch (lvl)
				{
					case First:
						jobLevel = 1;
						break;
					case Second:
						jobLevel = 2;
						break;
					case Third:
						jobLevel = 3;
						break;
					default:
						jobLevel = 4;
				}
				if (player.isAio() && !Config.ALLOW_AIO_USE_CM)
				{
					player.sendMessage("Aio Buffers Can't Speak To Class Masters.");
					return;
				}
				if (player.isGM())
				{
					showChatWindowChooseClass(player);
				}
				else if (level >= 20 && jobLevel == 1 && Config.ALLOW_CLASS_MASTERS_FIRST_CLASS || level >= 40 && jobLevel == 2 && Config.ALLOW_CLASS_MASTERS_SECOND_CLASS || level >= 76 && jobLevel == 3 && Config.ALLOW_CLASS_MASTERS_THIRD_CLASS || Config.CLASS_MASTER_STRIDER_UPDATE)
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					final TextBuilder sb = new TextBuilder();
					sb.append("<html><title>Class Manager</title><body><center><img src=L2Font-e.replay_logo-e width=258 height=60><br><br><br><img src=L2UI_CH3.herotower_deco width=256 height=32></center><br><br>");
					
					if (level >= 20 && jobLevel == 1 && Config.ALLOW_CLASS_MASTERS_FIRST_CLASS || level >= 40 && jobLevel == 2 && Config.ALLOW_CLASS_MASTERS_SECOND_CLASS || level >= 76 && jobLevel == 3 && Config.ALLOW_CLASS_MASTERS_THIRD_CLASS)
					{
						sb.append("<font color=AAAAAA>Please choose from the list of classes below...</font><br><br>");
						
						for (final ClassId child : ClassId.values())
							if (child.childOf(classId) && child.level() == jobLevel)
							{
								sb.append("<br><a action=\"bypass -h npc_" + getObjectId() + "_change_class " + child.getId() + "\"> " + CharTemplateTable.getClassNameById(child.getId()) + "</a>");
							}
						
						if (Config.CLASS_MASTER_SETTINGS.getRequireItems(jobLevel) != null && Config.CLASS_MASTER_SETTINGS.getRequireItems(jobLevel).size() > 0)
						{
							sb.append("<br><br>Item(s) required for class change:");
							sb.append("<table width=220>");
							for (final Integer _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(jobLevel).keySet())
							{
								final int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(jobLevel).get(_itemId);
								sb.append("<tr><td><font color=\"LEVEL\">" + _count + "</font></td><td>" + ItemTable.getInstance().getTemplate(_itemId).getName() + "</td></tr>");
							}
							sb.append("</table>");
						}
					}
					if (Config.CLASS_MASTER_STRIDER_UPDATE)
					{
						sb.append("<br><br><a action=\"bypass -h npc_" + getObjectId() + "_upgrade_hatchling\">Upgrade Hatchling to Strider</a><br>");
					}
					sb.append("<br><center><img src=L2UI_CH3.herotower_deco width=256 height=32></center></body></html>");
					html.setHtml(sb.toString());
					player.sendPacket(html);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					final TextBuilder sb = new TextBuilder();
					sb.append("<html><title>Class Manager</title><body><center><img src=L2Font-e.replay_logo-e width=258 height=60><br><br><br><img src=L2UI_CH3.herotower_deco width=256 height=32></center><br><br>");
					switch (jobLevel)
					{
						case 1:
							sb.append("Come back here when you reach level 20 to change your class.<br>");
							break;
						case 2:
							sb.append("Come back here when you reach level 40 to change your class.<br>");
							break;
						case 3:
							sb.append("Come back here when you reach level 76 to change your class.<br>");
							break;
						case 4:
							sb.append("There are no more class changes for you.<br>");
							break;
					}
					if (Config.CLASS_MASTER_STRIDER_UPDATE)
					{
						sb.append("<br><br><a action=\"bypass -h npc_" + getObjectId() + "_upgrade_hatchling\">Upgrade Hatchling to Strider</a><br>");
					}
					for (final Quest q : Quest.findAllEvents())
					{
						sb.append("Event: <a action=\"bypass -h Quest " + q.getName() + "\">" + q.getDescr() + "</a><br>");
					}
					sb.append("<br><center><img src=L2UI_CH3.herotower_deco width=256 height=32></center></body></html>");
					html.setHtml(sb.toString());
					player.sendPacket(html);
				}
				lvl = null;
				classId = null;
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jfrozen.gameserver.model.actor.instance.L2FolkInstance#onBypassFeedback(com.l2jfrozen.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (command.startsWith("1stClass"))
		{
			if (player.isGM())
			{
				showChatWindow1st(player);
			}
		}
		else if (command.startsWith("2ndClass"))
		{
			if (player.isGM())
			{
				showChatWindow2nd(player);
			}
		}
		else if (command.startsWith("3rdClass"))
		{
			if (player.isGM())
			{
				showChatWindow3rd(player);
			}
		}
		else if (command.startsWith("baseClass"))
		{
			if (player.isGM())
			{
				showChatWindowBase(player);
			}
		}
		else if (command.startsWith("change_class"))
		{
			final int val = Integer.parseInt(command.substring(13));
			
			// Exploit prevention
			ClassId classId = player.getClassId();
			final int level = player.getLevel();
			int jobLevel = 0;
			int newJobLevel = 0;
			
			player.setTarget(player);
			
			ClassLevel lvlnow = PlayerClass.values()[classId.getId()].getLevel();
			
			if (player.isGM())
			{
				changeClass(player, val);
				player.rewardSkills();
				
				if (val >= 88)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER)); // system sound 3rd occupation
				}
				else
				{
					player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER)); // system sound for 1st and 2nd occupation
				}
				
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				TextBuilder sb = new TextBuilder();
				sb.append("<html><title>Class Manager</title><body><center><img src=L2Font-e.replay_logo-e width=258 height=60><br><br><br><img src=L2UI_CH3.herotower_deco width=256 height=32></center><br><br>");
				sb.append("You have now become a <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getClassId().getId()) + "</font>.");
				sb.append("<br><center><img src=L2UI_CH3.herotower_deco width=256 height=32></center></body></html>");
				
				html.setHtml(sb.toString());
				player.sendPacket(html);
				html = null;
				sb = null;
				return;
			}
			switch (lvlnow)
			{
				case First:
					jobLevel = 1;
					break;
				case Second:
					jobLevel = 2;
					break;
				case Third:
					jobLevel = 3;
					break;
				default:
					jobLevel = 4;
			}
			
			if (jobLevel == 4)
				return; // no more job changes
				
			ClassLevel lvlnext = PlayerClass.values()[val].getLevel();
			switch (lvlnext)
			{
				case First:
					newJobLevel = 1;
					break;
				case Second:
					newJobLevel = 2;
					break;
				case Third:
					newJobLevel = 3;
					break;
				default:
					newJobLevel = 4;
			}
			
			lvlnext = null;
			// prevents changing between same level jobs
			if (newJobLevel != jobLevel + 1)
				return;
			
			if (level < 20 && newJobLevel > 1)
				return;
			if (level < 40 && newJobLevel > 2)
				return;
			if (level < 76 && newJobLevel > 3)
				return;
			// -- prevention ends
			
			if (newJobLevel == 2 && !Config.ALLOW_CLASS_MASTERS_FIRST_CLASS)
				return;
			
			if (newJobLevel == 3 && !Config.ALLOW_CLASS_MASTERS_SECOND_CLASS)
				return;
			
			if (newJobLevel == 4 && !Config.ALLOW_CLASS_MASTERS_THIRD_CLASS)
				return;
			
			// check if player have all required items for class transfer
			for (final Integer _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(jobLevel).keySet())
			{
				final int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(jobLevel).get(_itemId);
				if (player.getInventory().getInventoryItemCount(_itemId, -1) < _count)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
			}
			
			// get all required items for class transfer
			for (final Integer _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(jobLevel).keySet())
			{
				final int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(jobLevel).get(_itemId);
				player.destroyItemByItemId("ClassMaster", _itemId, _count, player, true);
			}
			
			// reward player with items
			for (final Integer _itemId : Config.CLASS_MASTER_SETTINGS.getRewardItems(jobLevel).keySet())
			{
				final int _count = Config.CLASS_MASTER_SETTINGS.getRewardItems(jobLevel).get(_itemId);
				player.addItem("ClassMaster", _itemId, _count, player, true);
			}
			
			changeClass(player, val);
			
			player.rewardSkills();
			
			// Check player skills
			if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
				player.checkAllowedSkills();
			
			if (val >= 88)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER)); // system sound 3rd occupation
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER)); // system sound for 1st and 2nd occupation
			}
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><title>Class Manager</title><body><center><img src=L2Font-e.replay_logo-e width=258 height=60><br><br><br><img src=L2UI_CH3.herotower_deco width=256 height=32></center><br><br>");
			sb.append("You have now become a <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getClassId().getId()) + "</font>.");
			sb.append("<br><center><img src=L2UI_CH3.herotower_deco width=256 height=32></center></body></html>");
			
			html.setHtml(sb.toString());
			player.sendPacket(html);
			
			sb = null;
			html = null;
			lvlnow = null;
			classId = null;
		}
		else if (command.startsWith("upgrade_hatchling") && Config.CLASS_MASTER_STRIDER_UPDATE)
		{
			boolean canUpgrade = false;
			if (player.getPet() != null)
			{
				if (player.getPet().getNpcId() == 12311 || player.getPet().getNpcId() == 12312 || player.getPet().getNpcId() == 12313)
				{
					if (player.getPet().getLevel() >= 55)
					{
						canUpgrade = true;
					}
					else
					{
						player.sendMessage("The level of your hatchling is too low to be upgraded.");
					}
				}
				else
				{
					player.sendMessage("You have to summon your hatchling.");
				}
			}
			else
			{
				player.sendMessage("You have to summon your hatchling if you want to upgrade him.");
			}
			
			if (!canUpgrade)
				return;
			
			final int[] hatchCollar =
			{
				3500,
				3501,
				3502
			};
			final int[] striderCollar =
			{
				4422,
				4423,
				4424
			};
			
			for (int i = 0; i < 3; i++)
			{
				final L2ItemInstance collar = player.getInventory().getItemByItemId(hatchCollar[i]);
				
				if (collar != null)
				{
					// Unsummon the hatchling
					player.getPet().unSummon(player);
					player.destroyItem("ClassMaster", collar, player, true);
					player.addItem("ClassMaster", striderCollar[i], 1, player, true);
					
					return;
				}
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	/**
	 * Show chat window choose class.
	 * @param player the player
	 */
	private void showChatWindowChooseClass(final L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		TextBuilder sb = new TextBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<table width=200>");
		sb.append("<tr><td><center>GM Class Master:</center></td></tr>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_baseClass\">Base Classes.</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_1stClass\">1st Classes.</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_2ndClass\">2nd Classes.</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_3rdClass\">3rd Classes.</a></td></tr>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("</table>");
		sb.append("</body>");
		sb.append("</html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
		html = null;
		sb = null;
		return;
	}
	
	/**
	 * Show chat window1st.
	 * @param player the player
	 */
	private void showChatWindow1st(final L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		TextBuilder sb = new TextBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<table width=200>");
		sb.append("<tr><td><center>GM Class Master:</center></td></tr>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 1\">Advance to " + CharTemplateTable.getClassNameById(1) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 4\">Advance to " + CharTemplateTable.getClassNameById(4) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 7\">Advance to " + CharTemplateTable.getClassNameById(7) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 11\">Advance to " + CharTemplateTable.getClassNameById(11) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 15\">Advance to " + CharTemplateTable.getClassNameById(15) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 19\">Advance to " + CharTemplateTable.getClassNameById(19) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 22\">Advance to " + CharTemplateTable.getClassNameById(22) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 26\">Advance to " + CharTemplateTable.getClassNameById(26) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 29\">Advance to " + CharTemplateTable.getClassNameById(29) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 32\">Advance to " + CharTemplateTable.getClassNameById(32) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 35\">Advance to " + CharTemplateTable.getClassNameById(35) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 39\">Advance to " + CharTemplateTable.getClassNameById(39) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 42\">Advance to " + CharTemplateTable.getClassNameById(42) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 45\">Advance to " + CharTemplateTable.getClassNameById(45) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 47\">Advance to " + CharTemplateTable.getClassNameById(47) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 50\">Advance to " + CharTemplateTable.getClassNameById(50) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 54\">Advance to " + CharTemplateTable.getClassNameById(54) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 56\">Advance to " + CharTemplateTable.getClassNameById(56) + "</a></td></tr>");
		sb.append("</table>");
		sb.append("</body>");
		sb.append("</html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
		html = null;
		sb = null;
		return;
	}
	
	/**
	 * Show chat window2nd.
	 * @param player the player
	 */
	private void showChatWindow2nd(final L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		TextBuilder sb = new TextBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<table width=200>");
		sb.append("<tr><td><center>GM Class Master:</center></td></tr>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 2\">Advance to " + CharTemplateTable.getClassNameById(2) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 3\">Advance to " + CharTemplateTable.getClassNameById(3) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 5\">Advance to " + CharTemplateTable.getClassNameById(5) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 6\">Advance to " + CharTemplateTable.getClassNameById(6) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 8\">Advance to " + CharTemplateTable.getClassNameById(8) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 9\">Advance to " + CharTemplateTable.getClassNameById(9) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 12\">Advance to " + CharTemplateTable.getClassNameById(12) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 13\">Advance to " + CharTemplateTable.getClassNameById(13) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 14\">Advance to " + CharTemplateTable.getClassNameById(14) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 16\">Advance to " + CharTemplateTable.getClassNameById(16) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 17\">Advance to " + CharTemplateTable.getClassNameById(17) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 20\">Advance to " + CharTemplateTable.getClassNameById(20) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 21\">Advance to " + CharTemplateTable.getClassNameById(21) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 23\">Advance to " + CharTemplateTable.getClassNameById(23) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 24\">Advance to " + CharTemplateTable.getClassNameById(24) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 27\">Advance to " + CharTemplateTable.getClassNameById(27) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 28\">Advance to " + CharTemplateTable.getClassNameById(28) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 30\">Advance to " + CharTemplateTable.getClassNameById(30) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 33\">Advance to " + CharTemplateTable.getClassNameById(33) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 34\">Advance to " + CharTemplateTable.getClassNameById(34) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 36\">Advance to " + CharTemplateTable.getClassNameById(36) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 37\">Advance to " + CharTemplateTable.getClassNameById(37) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 40\">Advance to " + CharTemplateTable.getClassNameById(40) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 41\">Advance to " + CharTemplateTable.getClassNameById(41) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 43\">Advance to " + CharTemplateTable.getClassNameById(43) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 46\">Advance to " + CharTemplateTable.getClassNameById(46) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 48\">Advance to " + CharTemplateTable.getClassNameById(48) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 51\">Advance to " + CharTemplateTable.getClassNameById(51) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 52\">Advance to " + CharTemplateTable.getClassNameById(52) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 55\">Advance to " + CharTemplateTable.getClassNameById(55) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 57\">Advance to " + CharTemplateTable.getClassNameById(57) + "</a></td></tr>");
		sb.append("</table>");
		sb.append("</body>");
		sb.append("</html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
		html = null;
		sb = null;
		return;
	}
	
	/**
	 * Show chat window3rd.
	 * @param player the player
	 */
	private void showChatWindow3rd(final L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		TextBuilder sb = new TextBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<table width=200>");
		sb.append("<tr><td><center>GM Class Master:</center></td></tr>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 88\">Advance to " + CharTemplateTable.getClassNameById(88) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 89\">Advance to " + CharTemplateTable.getClassNameById(89) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 90\">Advance to " + CharTemplateTable.getClassNameById(90) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 91\">Advance to " + CharTemplateTable.getClassNameById(91) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 92\">Advance to " + CharTemplateTable.getClassNameById(92) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 93\">Advance to " + CharTemplateTable.getClassNameById(93) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 94\">Advance to " + CharTemplateTable.getClassNameById(94) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 95\">Advance to " + CharTemplateTable.getClassNameById(95) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 96\">Advance to " + CharTemplateTable.getClassNameById(96) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 97\">Advance to " + CharTemplateTable.getClassNameById(97) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 98\">Advance to " + CharTemplateTable.getClassNameById(98) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 99\">Advance to " + CharTemplateTable.getClassNameById(99) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 100\">Advance to " + CharTemplateTable.getClassNameById(100) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 101\">Advance to " + CharTemplateTable.getClassNameById(101) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 102\">Advance to " + CharTemplateTable.getClassNameById(102) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 103\">Advance to " + CharTemplateTable.getClassNameById(103) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 104\">Advance to " + CharTemplateTable.getClassNameById(104) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 105\">Advance to " + CharTemplateTable.getClassNameById(105) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 106\">Advance to " + CharTemplateTable.getClassNameById(106) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 107\">Advance to " + CharTemplateTable.getClassNameById(107) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 108\">Advance to " + CharTemplateTable.getClassNameById(108) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 109\">Advance to " + CharTemplateTable.getClassNameById(109) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 110\">Advance to " + CharTemplateTable.getClassNameById(110) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 111\">Advance to " + CharTemplateTable.getClassNameById(111) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 112\">Advance to " + CharTemplateTable.getClassNameById(112) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 113\">Advance to " + CharTemplateTable.getClassNameById(113) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 114\">Advance to " + CharTemplateTable.getClassNameById(114) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 115\">Advance to " + CharTemplateTable.getClassNameById(115) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 116\">Advance to " + CharTemplateTable.getClassNameById(116) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 117\">Advance to " + CharTemplateTable.getClassNameById(117) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 118\">Advance to " + CharTemplateTable.getClassNameById(118) + "</a></td></tr>");
		sb.append("</table>");
		sb.append("</body>");
		sb.append("</html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
		html = null;
		sb = null;
		return;
	}
	
	/**
	 * Show chat window base.
	 * @param player the player
	 */
	private void showChatWindowBase(final L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		TextBuilder sb = new TextBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<table width=200>");
		sb.append("<tr><td><center>GM Class Master:</center></td></tr>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 0\">Advance to " + CharTemplateTable.getClassNameById(0) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 10\">Advance to " + CharTemplateTable.getClassNameById(10) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 18\">Advance to " + CharTemplateTable.getClassNameById(18) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 25\">Advance to " + CharTemplateTable.getClassNameById(25) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 31\">Advance to " + CharTemplateTable.getClassNameById(31) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 38\">Advance to " + CharTemplateTable.getClassNameById(38) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 44\">Advance to " + CharTemplateTable.getClassNameById(44) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 49\">Advance to " + CharTemplateTable.getClassNameById(49) + "</a></td></tr>");
		sb.append("<tr><td><a action=\"bypass -h npc_" + getObjectId() + "_change_class 53\">Advance to " + CharTemplateTable.getClassNameById(53) + "</a></td></tr>");
		sb.append("</table>");
		sb.append("</body>");
		sb.append("</html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
		html = null;
		sb = null;
		return;
	}
	
	/**
	 * Change class.
	 * @param player the player
	 * @param val the val
	 */
	private void changeClass(final L2PcInstance player, final int val)
	{
		if (CommonConfig.DEBUG)
		{
			LOGGER.info("Changing class to ClassId:" + val);
		}
		player.setClassId(val);
		
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		}
		else
		{
			ClassId classId = ClassId.getClassIdByOrdinal(player.getActiveClass());
			
			if (classId.getParent() != null)
			{
				while (classId.level() == 0)
				{ // go to root
					classId = classId.getParent();
				}
			}
			
			player.setBaseClass(classId);
			
			// player.setBaseClass(player.getActiveClass());
		}
		
		player.broadcastUserInfo();
		player.broadcastClassIcon();
	}
}