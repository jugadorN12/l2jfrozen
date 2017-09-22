/*
 * L2jFrozen Project - www.l2jfrozen.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jfrozen.gameserver.network.serverpackets;

import com.l2jfrozen.gameserver.model.PartyMatchRoom;
import com.l2jfrozen.gameserver.model.PartyMatchRoomList;
import com.l2jfrozen.gameserver.model.PartyMatchWaitingList;
import com.l2jfrozen.gameserver.model.actor.instance.L2PcInstance;

import javolution.util.FastList;

/**
 * @author Gnacik
 */
public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;
	private final int _minlvl;
	private final int _maxlvl;
	private final int _mode;
	private final FastList<L2PcInstance> _members;
	
	public ExListPartyMatchingWaitingRoom(final L2PcInstance player, final int page, final int minlvl, final int maxlvl, final int mode)
	{
		_activeChar = player;
		_minlvl = minlvl;
		_maxlvl = maxlvl;
		_mode = mode;
		_members = new FastList<>();
	}
	
	@Override
	protected void writeImpl()
	{
		C(0xfe);
		H(0x35);
		
		// If the mode is 0 and the activeChar isn't the PartyRoom leader, return an empty list.
		if (_mode == 0)
		{
			// Retrieve the activeChar PartyMatchRoom
			final PartyMatchRoom _room = PartyMatchRoomList.getInstance().getRoom(_activeChar.getPartyRoom());
			if (_room != null && _room.getOwner() != null && !_room.getOwner().equals(_activeChar))
			{
				D(0);
				D(0);
				return;
			}
		}
		
		for (final L2PcInstance cha : PartyMatchWaitingList.getInstance().getPlayers())
		{
			// Don't add yourself in the list
			if (cha == null || cha == _activeChar)
				continue;
			
			if (!cha.isPartyWaiting())
			{
				PartyMatchWaitingList.getInstance().removePlayer(cha);
				continue;
			}
			
			if ((cha.getLevel() < _minlvl) || (cha.getLevel() > _maxlvl))
				continue;
			
			_members.add(cha);
		}
		
		int _count = 0;
		final int _size = _members.size();
		
		D(1);
		D(_size);
		while (_size > _count)
		{
			S(_members.get(_count).getName());
			D(_members.get(_count).getActiveClass());
			D(_members.get(_count).getLevel());
			_count++;
		}
	}
	
	@Override
	public String getType()
	{
		return "[S] FE:35 ExListPartyMatchingWaitingRoom";
	}
}