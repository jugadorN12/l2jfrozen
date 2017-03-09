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
package com.l2jfrozen.gameserver.model.actor.status;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.l2jfrozen.CommonConfig;
import com.l2jfrozen.gameserver.ai.CtrlIntention;
import com.l2jfrozen.gameserver.managers.DuelManager;
import com.l2jfrozen.gameserver.model.L2Attackable;
import com.l2jfrozen.gameserver.model.L2Character;
import com.l2jfrozen.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jfrozen.gameserver.model.actor.instance.L2PcInstance;
import com.l2jfrozen.gameserver.model.actor.instance.L2SummonInstance;
import com.l2jfrozen.gameserver.model.actor.stat.CharStat;
import com.l2jfrozen.gameserver.model.entity.Duel;
import com.l2jfrozen.gameserver.network.serverpackets.ActionFailed;
import com.l2jfrozen.gameserver.skills.Formulas;
import com.l2jfrozen.thread.ThreadPoolManager;
import com.l2jfrozen.util.random.Rnd;

/**
 * The Class CharStatus.
 */
public class CharStatus
{
	
	/** The Constant LOGGER. */
	protected static final Logger LOGGER = Logger.getLogger(CharStatus.class);
	
	// =========================================================
	// Data Field
	/** The _active char. */
	private final L2Character _activeChar;
	
	/** The _current cp. */
	private double _currentCp = 0; // Current CP of the L2Character
	
	/** The _current hp. */
	private double _currentHp = 0; // Current HP of the L2Character
	
	/** The _current mp. */
	private double _currentMp = 0; // Current MP of the L2Character
	
	/** Array containing all clients that need to be notified about hp/mp updates of the L2Character. */
	private Set<L2Character> _StatusListener;
	
	/** The _reg task. */
	private Future<?> _regTask;
	
	/** The _flags regen active. */
	private byte _flagsRegenActive = 0;
	
	/** The Constant REGEN_FLAG_CP. */
	private static final byte REGEN_FLAG_CP = 4;
	
	/** The Constant REGEN_FLAG_HP. */
	private static final byte REGEN_FLAG_HP = 1;
	
	/** The Constant REGEN_FLAG_MP. */
	private static final byte REGEN_FLAG_MP = 2;
	
	// =========================================================
	// Constructor
	/**
	 * Instantiates a new char status.
	 * @param activeChar the active char
	 */
	public CharStatus(final L2Character activeChar)
	{
		_activeChar = activeChar;
	}
	
	/**
	 * Add the object to the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates. Players who must be informed are players that target this L2Character. When a RegenTask is in progress sever just need to go through this list to send Server->Client packet
	 * StatusUpdate.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Target a PC or NPC</li><BR>
	 * <BR>
	 * @param object L2Character to add to the listener
	 */
	public final void addStatusListener(final L2Character object)
	{
		if (object == getActiveChar())
			return;
		
		synchronized (getStatusListener())
		{
			getStatusListener().add(object);
		}
	}
	
	/**
	 * Reduce cp.
	 * @param value the value
	 */
	public final void reduceCp(final int value)
	{
		if (getCurrentCp() > value)
		{
			setCurrentCp(getCurrentCp() - value);
		}
		else
		{
			setCurrentCp(0);
		}
	}
	
	/**
	 * Reduce the current HP of the L2Character and launch the doDie Task if necessary.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2Attackable : Update the attacker AggroInfo of the L2Attackable _aggroList</li><BR>
	 * <BR>
	 * @param value the value
	 * @param attacker The L2Character who attacks
	 */
	public void reduceHp(final double value, final L2Character attacker)
	{
		reduceHp(value, attacker, true);
	}
	
	/**
	 * Reduce hp.
	 * @param value the value
	 * @param attacker the attacker
	 * @param awake the awake
	 */
	public void reduceHp(double value, final L2Character attacker, final boolean awake)
	{
		if (getActiveChar().isInvul())
			return;
		
		if (getActiveChar() instanceof L2PcInstance)
		{
			if (((L2PcInstance) getActiveChar()).isInDuel())
			{
				// the duel is finishing - players do not recive damage
				if (((L2PcInstance) getActiveChar()).getDuelState() == Duel.DUELSTATE_DEAD)
					return;
				else if (((L2PcInstance) getActiveChar()).getDuelState() == Duel.DUELSTATE_WINNER)
					return;
				
				// cancel duel if player got hit by another player, that is not part of the duel or a monster
				if (!(attacker instanceof L2SummonInstance) && !(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getDuelId() == ((L2PcInstance) getActiveChar()).getDuelId()))
				{
					((L2PcInstance) getActiveChar()).setDuelState(Duel.DUELSTATE_INTERRUPTED);
				}
			}
			if (getActiveChar().isDead() && !getActiveChar().isFakeDeath())
				return; // Disabled == null check so skills like Body to Mind work again untill another solution is found
		}
		else
		{
			if (getActiveChar().isDead())
				return; // Disabled == null check so skills like Body to Mind work again untill another solution is found
				
			if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isInDuel() && !(getActiveChar() instanceof L2SummonInstance && ((L2SummonInstance) getActiveChar()).getOwner().getDuelId() == ((L2PcInstance) attacker).getDuelId())) // Duelling player attacks mob
			{
				((L2PcInstance) attacker).setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}
		}
		
		if (awake && getActiveChar().isSleeping())
		{
			getActiveChar().stopSleeping(null);
		}
		
		if (awake && getActiveChar().isImmobileUntilAttacked())
		{
			getActiveChar().stopImmobileUntilAttacked(null);
		}
		
		if (getActiveChar().isStunned() && Rnd.get(10) == 0)
		{
			getActiveChar().stopStunning(null);
		}
		
		// Add attackers to npc's attacker list
		if (getActiveChar() instanceof L2NpcInstance)
		{
			getActiveChar().addAttackerToAttackByList(attacker);
		}
		
		if (value > 0) // Reduce Hp if any
		{
			// If we're dealing with an L2Attackable Instance and the attacker hit it with an over-hit enabled skill, set the over-hit values.
			// Anything else, clear the over-hit flag
			if (getActiveChar() instanceof L2Attackable)
			{
				if (((L2Attackable) getActiveChar()).isOverhit())
				{
					((L2Attackable) getActiveChar()).setOverhitValues(attacker, value);
				}
				else
				{
					((L2Attackable) getActiveChar()).overhitEnabled(false);
				}
			}
			
			value = getCurrentHp() - value; // Get diff of Hp vs value
			
			if (value <= 0)
			{
				// is the dieing one a duelist? if so change his duel state to dead
				if (getActiveChar() instanceof L2PcInstance && ((L2PcInstance) getActiveChar()).isInDuel())
				{
					getActiveChar().disableAllSkills();
					stopHpMpRegeneration();
					attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					attacker.sendPacket(ActionFailed.STATIC_PACKET);
					
					// let the DuelManager know of his defeat
					DuelManager.getInstance().onPlayerDefeat((L2PcInstance) getActiveChar());
					value = 1;
				}
				else
				{
					// Set value to 0 if Hp < 0
					value = 0;
				}
			}
			setCurrentHp(value); // Set Hp
		}
		else
		{
			// If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
			if (getActiveChar() instanceof L2Attackable)
			{
				((L2Attackable) getActiveChar()).overhitEnabled(false);
			}
		}
		
		if (getActiveChar().isDead())
		{
			getActiveChar().abortAttack();
			getActiveChar().abortCast();
			
			if (getActiveChar() instanceof L2PcInstance)
			{
				if (((L2PcInstance) getActiveChar()).isInOlympiadMode())
				{
					stopHpMpRegeneration();
					return;
				}
			}
			
			// first die (and calculate rewards), if currentHp < 0,
			// then overhit may be calculated
			if (CommonConfig.DEBUG)
			{
				LOGGER.debug("char is dead.");
			}
			
			// Start the doDie process
			getActiveChar().doDie(attacker);
			
			// now reset currentHp to zero
			setCurrentHp(0);
		}
		else
		{
			// If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
			if (getActiveChar() instanceof L2Attackable)
			{
				((L2Attackable) getActiveChar()).overhitEnabled(false);
			}
		}
	}
	
	/**
	 * Reduce mp.
	 * @param value the value
	 */
	public final void reduceMp(double value)
	{
		value = getCurrentMp() - value;
		
		if (value < 0)
		{
			value = 0;
		}
		
		setCurrentMp(value);
	}
	
	/**
	 * Remove the object from the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates. Players who must be informed are players that target this L2Character. When a RegenTask is in progress sever just need to go through this list to send Server->Client packet
	 * StatusUpdate.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Untarget a PC or NPC</li><BR>
	 * <BR>
	 * @param object L2Character to add to the listener
	 */
	public final void removeStatusListener(final L2Character object)
	{
		synchronized (getStatusListener())
		{
			getStatusListener().remove(object);
		}
	}
	
	/**
	 * Start the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate the regen task period</li>
	 * <li>Launch the HP/MP/CP Regeneration task with Medium priority</li><BR>
	 * <BR>
	 */
	public synchronized final void startHpMpRegeneration()
	{
		if (_regTask == null && !getActiveChar().isDead())
		{
			if (CommonConfig.DEBUG)
			{
				LOGGER.debug("HP/MP/CP regen started");
			}
			
			// Get the Regeneration periode
			final int period = Formulas.getRegeneratePeriod(getActiveChar());
			
			// Create the HP/MP/CP Regeneration task
			_regTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new RegenTask(), period, period);
		}
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li><BR>
	 * <BR>
	 */
	public synchronized final void stopHpMpRegeneration()
	{
		if (_regTask != null)
		{
			if (CommonConfig.DEBUG)
			{
				LOGGER.debug("HP/MP/CP regen stop");
			}
			
			// Stop the HP/MP/CP Regeneration task
			_regTask.cancel(false);
			_regTask = null;
			
			// Set the RegenActive flag to false
			_flagsRegenActive = 0;
		}
	}
	
	// =========================================================
	// Method - Private
	
	// =========================================================
	// Property - Public
	/**
	 * Gets the active char.
	 * @return the active char
	 */
	public L2Character getActiveChar()
	{
		return _activeChar;
	}
	
	/**
	 * Gets the current cp.
	 * @return the current cp
	 */
	public final double getCurrentCp()
	{
		return _currentCp;
	}
	
	/**
	 * Sets the current cp direct.
	 * @param newCp the new current cp direct
	 */
	public final void setCurrentCpDirect(final double newCp)
	{
		setCurrentCp(newCp, true, true);
	}
	
	/**
	 * Sets the current cp.
	 * @param newCp the new current cp
	 */
	public final void setCurrentCp(final double newCp)
	{
		setCurrentCp(newCp, true, false);
	}
	
	/**
	 * Sets the current cp.
	 * @param newCp the new cp
	 * @param broadcastPacket the broadcast packet
	 */
	public final void setCurrentCp(final double newCp, final boolean broadcastPacket)
	{
		setCurrentCp(newCp, broadcastPacket, false);
	}
	
	/**
	 * Sets the current cp.
	 * @param newCp the new cp
	 * @param broadcastPacket the broadcast packet
	 * @param direct the direct
	 */
	public final void setCurrentCp(double newCp, final boolean broadcastPacket, final boolean direct)
	{
		synchronized (this)
		{
			// Get the Max CP of the L2Character
			final int maxCp = getActiveChar().getStat().getMaxCp();
			
			if (newCp < 0)
			{
				newCp = 0;
			}
			
			if (newCp >= maxCp && !direct)
			{
				// Set the RegenActive flag to false
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	/**
	 * Gets the current hp.
	 * @return the current hp
	 */
	public final double getCurrentHp()
	{
		return _currentHp;
	}
	
	/**
	 * Sets the current hp.
	 * @param newHp the new current hp
	 */
	public final void setCurrentHp(final double newHp)
	{
		setCurrentHp(newHp, true);
	}
	
	/**
	 * Sets the current hp direct.
	 * @param newHp the new current hp direct
	 */
	public final void setCurrentHpDirect(final double newHp)
	{
		setCurrentHp(newHp, true, true);
	}
	
	/**
	 * Sets the current mp direct.
	 * @param newMp the new current mp direct
	 */
	public final void setCurrentMpDirect(final double newMp)
	{
		setCurrentMp(newMp, true, true);
	}
	
	/**
	 * Sets the current hp.
	 * @param newHp the new hp
	 * @param broadcastPacket the broadcast packet
	 */
	public final void setCurrentHp(final double newHp, final boolean broadcastPacket)
	{
		setCurrentHp(newHp, true, false);
	}
	
	/**
	 * Sets the current hp.
	 * @param newHp the new hp
	 * @param broadcastPacket the broadcast packet
	 * @param direct the direct
	 */
	public final void setCurrentHp(final double newHp, final boolean broadcastPacket, final boolean direct)
	{
		synchronized (this)
		{
			// Get the Max HP of the L2Character
			final double maxHp = getActiveChar().getStat().getMaxHp();
			
			if (newHp >= maxHp && !direct)
			{
				// Set the RegenActive flag to false
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				getActiveChar().setIsKilledAlready(false);
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				
				if (!getActiveChar().isDead())
				{
					getActiveChar().setIsKilledAlready(false);
				}
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
			
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	/**
	 * Sets the current hp mp.
	 * @param newHp the new hp
	 * @param newMp the new mp
	 */
	public final void setCurrentHpMp(final double newHp, final double newMp)
	{
		setCurrentHp(newHp, false, false);
		setCurrentMp(newMp, true, false); // send the StatusUpdate only once
	}
	
	/**
	 * Gets the current mp.
	 * @return the current mp
	 */
	public final double getCurrentMp()
	{
		return _currentMp;
	}
	
	/**
	 * Sets the current mp.
	 * @param newMp the new current mp
	 */
	public final void setCurrentMp(final double newMp)
	{
		setCurrentMp(newMp, true);
	}
	
	/**
	 * Sets the current mp.
	 * @param newMp the new mp
	 * @param broadcastPacket the broadcast packet
	 */
	public final void setCurrentMp(final double newMp, final boolean broadcastPacket)
	{
		setCurrentMp(newMp, broadcastPacket, false);
	}
	
	/**
	 * Sets the current mp.
	 * @param newMp the new mp
	 * @param broadcastPacket the broadcast packet
	 * @param direct the direct
	 */
	public final void setCurrentMp(final double newMp, final boolean broadcastPacket, final boolean direct)
	{
		synchronized (this)
		{
			// Get the Max MP of the L2Character
			final int maxMp = getActiveChar().getStat().getMaxMp();
			
			if (newMp >= maxMp && !direct)
			{
				// Set the RegenActive flag to false
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	/**
	 * Return the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates. Players who must be informed are players that target this L2Character. When a RegenTask is in progress sever just need to go through this list to send Server->Client packet
	 * StatusUpdate.<BR>
	 * <BR>
	 * @return The list of L2Character to inform or null if empty
	 */
	public final Set<L2Character> getStatusListener()
	{
		if (_StatusListener == null)
		{
			_StatusListener = new CopyOnWriteArraySet<>();
		}
		
		return _StatusListener;
	}
	
	// =========================================================
	// Runnable
	/**
	 * Task of HP/MP/CP regeneration.
	 */
	class RegenTask implements Runnable
	{
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				final CharStat charstat = getActiveChar().getStat();
				
				// Modify the current CP of the L2Character and broadcast Server->Client packet StatusUpdate
				if (getCurrentCp() < charstat.getMaxCp())
				{
					setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(getActiveChar()), false);
				}
				
				// Modify the current HP of the L2Character and broadcast Server->Client packet StatusUpdate
				if (getCurrentHp() < charstat.getMaxHp())
				{
					setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()), false);
				}
				
				// Modify the current MP of the L2Character and broadcast Server->Client packet StatusUpdate
				if (getCurrentMp() < charstat.getMaxMp())
				{
					setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()), false);
				}
				
				if (!getActiveChar().isInActiveRegion())
				{
					// no broadcast necessary for characters that are in inactive regions.
					// stop regeneration for characters who are filled up and in an inactive region.
					if (getCurrentCp() == charstat.getMaxCp() && getCurrentHp() == charstat.getMaxHp() && getCurrentMp() == charstat.getMaxMp())
					{
						stopHpMpRegeneration();
					}
				}
				else
				{
					getActiveChar().broadcastStatusUpdate(); // send the StatusUpdate packet
				}
				
				// charstat = null;
			}
			catch (final Throwable e)
			{
				if (CommonConfig.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				
				LOGGER.error("RegenTask failed for " + getActiveChar().getName(), e);
			}
		}
	}
}
