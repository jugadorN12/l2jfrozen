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

/**
 * format dddddd
 */
public class Earthquake extends L2GameServerPacket
{
	private static final String _S__C4_EARTHQUAKE = "[S] C4 Earthquake";
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _intensity;
	private final int _duration;
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param intensity
	 * @param duration
	 */
	public Earthquake(final int x, final int y, final int z, final int intensity, final int duration)
	{
		_x = x;
		_y = y;
		_z = z;
		_intensity = intensity;
		_duration = duration;
	}
	
	@Override
	protected final void writeImpl()
	{
		C(0xc4);
		D(_x);
		D(_y);
		D(_z);
		D(_intensity);
		D(_duration);
		D(0x00); // Unknown
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2jfrozen.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__C4_EARTHQUAKE;
	}
}
