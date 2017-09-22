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
package com.l2jfrozen.gameserver.network.serverpackets;

import javolution.util.FastList;

/**
 * Format : (h) d [dS] h sub id d: number of manors [ d: id S: manor name ]
 * @author l3x
 */
public class ExSendManorList extends L2GameServerPacket
{
	private static final String _S__FE_1B_EXSENDMANORLIST = "[S] FE:1B ExSendManorList";
	
	private final FastList<String> _manors;
	
	public ExSendManorList(final FastList<String> manors)
	{
		_manors = manors;
	}
	
	@Override
	protected void writeImpl()
	{
		C(0xFE);
		H(0x1B);
		D(_manors.size());
		for (int i = 0; i < _manors.size(); i++)
		{
			final int j = i + 1;
			D(j);
			S(_manors.get(i));
		}
		
	}
	
	@Override
	public String getType()
	{
		return _S__FE_1B_EXSENDMANORLIST;
	}
}
