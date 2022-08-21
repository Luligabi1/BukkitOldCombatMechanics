/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kernitus.plugin.OldCombatMechanics.utilities.damage;

import kernitus.plugin.OldCombatMechanics.utilities.potions.PotionEffects;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static kernitus.plugin.OldCombatMechanics.utilities.Messenger.debug;

public class OCMEntityDamageByEntityEvent extends Event implements Cancellable {

    private boolean cancelled;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final Entity damager, damagee;
    private final DamageCause cause;
    private final double rawDamage;

    private ItemStack weapon;
    private int sharpnessLevel;
    private int strengthLevel;

    private double baseDamage = 0, mobEnchantmentsDamage = 0, sharpnessDamage = 0, criticalMultiplier = 1;
    private double strengthModifier = 0, weaknessModifier = 0;

    // In 1.9 strength modifier is an addend, in 1.8 it is a multiplier and addend (+130%)
    private boolean isStrengthModifierMultiplier = false;
    private boolean isStrengthModifierAddend = true;
    private boolean isWeaknessModifierMultiplier = false;

    private boolean was1_8Crit = false;
    private boolean wasSprinting = false;

    // Here we reverse-engineer all the various damages caused by removing them one at a time, backwards from what NMS code does.
    // This is so the modules can listen to this event and make their modifications, then EntityDamageByEntityListener sets the new values back.
    public OCMEntityDamageByEntityEvent(Entity damager, Entity damagee, DamageCause cause, double rawDamage) {
        this.damager = damager;
        this.damagee = damagee;
        this.cause = cause;
        this.rawDamage = rawDamage;

        if (!(damager instanceof LivingEntity)) {
            setCancelled(true);
            return;
        }

        final LivingEntity le = (LivingEntity) damager;

        final EntityEquipment equipment = le.getEquipment();
        weapon = equipment.getItemInMainHand();
        // Yay paper. Why do you need to return null here?
        if (weapon == null) weapon = new ItemStack(Material.AIR);
        // Technically the weapon could be in the offhand, i.e. a bow.
        // However, we are only concerned with melee weapons here, which will always be in the main hand.

        final EntityType damageeType = damagee.getType();

        debug(le, "Raw damage: " + rawDamage);

        /*
        Normally invulnerability overdamage would have to be taken into account here, but instead in the EDBEListener
        we artificially set the last damage to 0 between events so that all hits will register.
        Once all modules have acted upon the damage, we then work out if it will be an overdamage or not,
        and whether we need to cancel the event accordingly.
         */

        mobEnchantmentsDamage = MobDamage.applyEntityEnchantmentDamage(damageeType, weapon, rawDamage) - rawDamage;

        // todo scale by attack strength
        sharpnessLevel = weapon.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
        sharpnessDamage = DamageUtils.getNewSharpnessDamage(sharpnessLevel);

        debug(le, "Mob: " + mobEnchantmentsDamage + " Sharpness: " + sharpnessDamage);

        //Amount of damage including potion effects and critical hits
        double tempDamage = rawDamage - mobEnchantmentsDamage - sharpnessDamage;

        debug(le, "No ench damage: " + tempDamage);

        //Check if it's a critical hit
        if (DamageUtils.isCriticalHit1_8(le)) {
            was1_8Crit = true;
            debug(le, "1.8 Critical hit detected");
            // In 1.9 a crit also requires the player not to be sprinting
            if (le instanceof Player) {
                wasSprinting = ((Player) le).isSprinting();
                if (!wasSprinting) {
                    debug(le, "1.9 Critical hit detected");
                    criticalMultiplier = 1.5;
                    tempDamage /= 1.5;
                }
            }
        }

        //amplifier 0 = Strength I    amplifier 1 = Strength II
        int amplifier = PotionEffects.get(le, PotionEffectType.INCREASE_DAMAGE)
                .map(PotionEffect::getAmplifier)
                .orElse(-1);

        strengthLevel = ++amplifier;

        strengthModifier = strengthLevel * 3;

        debug(le, "Strength Modifier: " + strengthModifier);

        if (le.hasPotionEffect(PotionEffectType.WEAKNESS)) weaknessModifier = -4;

        debug(le, "Weakness Modifier: " + weaknessModifier);

        baseDamage = tempDamage + weaknessModifier - strengthModifier;
        debug(le, "Base tool damage: " + baseDamage);

        // todo scale by attack strength
    }

    public Entity getDamager() {
        return damager;
    }

    public Entity getDamagee() {
        return damagee;
    }

    public DamageCause getCause() {
        return cause;
    }

    public double getRawDamage() {
        return rawDamage;
    }

    public ItemStack getWeapon() {
        return weapon;
    }

    public int getSharpnessLevel() {
        return sharpnessLevel;
    }

    public double getStrengthModifier() {
        return strengthModifier;
    }

    public void setStrengthModifier(double strengthModifier) {
        this.strengthModifier = strengthModifier;
    }

    public int getStrengthLevel() {
        return strengthLevel;
    }

    public double getWeaknessModifier() {
        return weaknessModifier;
    }

    public void setWeaknessModifier(double weaknessModifier) {
        this.weaknessModifier = weaknessModifier;
    }

    public boolean isStrengthModifierMultiplier() {
        return isStrengthModifierMultiplier;
    }

    public void setIsStrengthModifierMultiplier(boolean isStrengthModifierMultiplier) {
        this.isStrengthModifierMultiplier = isStrengthModifierMultiplier;
    }

    public void setIsStrengthModifierAddend(boolean isStrengthModifierAddend) {
        this.isStrengthModifierAddend = isStrengthModifierAddend;
    }

    public boolean isWeaknessModifierMultiplier() {
        return isWeaknessModifierMultiplier;
    }

    public void setIsWeaknessModifierMultiplier(boolean weaknessModifierMultiplier) {
        isWeaknessModifierMultiplier = weaknessModifierMultiplier;
    }

    public boolean isStrengthModifierAddend() {
        return isStrengthModifierAddend;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public void setBaseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    public double getMobEnchantmentsDamage() {
        return mobEnchantmentsDamage;
    }

    public void setMobEnchantmentsDamage(double mobEnchantmentsDamage) {
        this.mobEnchantmentsDamage = mobEnchantmentsDamage;
    }

    public double getSharpnessDamage() {
        return sharpnessDamage;
    }

    public void setSharpnessDamage(double sharpnessDamage) {
        this.sharpnessDamage = sharpnessDamage;
    }

    public double getCriticalMultiplier() {
        return criticalMultiplier;
    }

    public void setCriticalMultiplier(double criticalMultiplier) {
        this.criticalMultiplier = criticalMultiplier;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean wasSprinting() {
        return wasSprinting;
    }

    public void setWasSprinting(boolean wasSprinting) {
        this.wasSprinting = wasSprinting;
    }

    public boolean was1_8Crit() {
        return was1_8Crit;
    }

    public void setWas1_8Crit(boolean was1_8Crit) {
        this.was1_8Crit = was1_8Crit;
    }
}
