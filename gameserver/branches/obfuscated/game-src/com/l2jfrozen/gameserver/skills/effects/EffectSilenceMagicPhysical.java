/* L2jFrozen Project - www.l2jfrozen.com 
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
package com.l2jfrozen.gameserver.skills.effects;

import com.l2jfrozen.gameserver.model.EffectType;
import com.l2jfrozen.gameserver.model.L2Effect;
import com.l2jfrozen.gameserver.skills.Env;

public class EffectSilenceMagicPhysical extends L2Effect
{
	
	public EffectSilenceMagicPhysical(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SILENCE_MAGIC_PHYSICAL;
	}
	
	@Override
	public void onStart()
	{
		getEffected().startMuted();
		getEffected().startPsychicalMuted();
	}
	
	@Override
	public boolean onActionTime()
	{
		getEffected().stopMuted(this);
		getEffected().stopPsychicalMuted(this);
		return false;
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopMuted(this);
		getEffected().stopPsychicalMuted(this);
	}
}
