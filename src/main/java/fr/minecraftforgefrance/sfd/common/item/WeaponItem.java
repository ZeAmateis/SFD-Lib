package fr.minecraftforgefrance.sfd.common.item;

import com.google.common.base.Predicate;
import fr.minecraftforgefrance.sfd.common.SFDProjectiles;
import fr.minecraftforgefrance.sfd.common.entity.SFDProjectileEntity;
import fr.minecraftforgefrance.sfd.common.weapons.WeaponDefinition;
import fr.minecraftforgefrance.sfd.maths.MathUtils;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class WeaponItem extends Item {

    private final WeaponDefinition definition;

    public WeaponItem(WeaponDefinition definition) {
        this.definition = definition;
        setMaxStackSize(1);
        setMaxDamage(definition.getCooldown());
        setCreativeTab(CreativeTabs.COMBAT);
        setUnlocalizedName(definition.getId());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, EnumHand hand) {
        if(itemStackIn.getItemDamage() == 0) {
            if(getMaxItemUseDuration(itemStackIn) > 0) {
                playerIn.setActiveHand(hand);
            } else {
                fire(worldIn, playerIn, itemStackIn);
            }
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemStackIn);
        }
        return new ActionResult<ItemStack>(EnumActionResult.FAIL, itemStackIn);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft) {

    }

    private void fire(World world, final EntityLivingBase shooter, ItemStack stack) {
        if(!world.isRemote) {
            stack.setItemDamage(getMaxDamage());
            float raycastDistance = 2000f; // 2000 blocks across
            switch (definition.getWeaponType()) {
                case PROJECTILE:
                    SFDProjectileEntity entity = SFDProjectiles.create(world, definition.getProjectileType(), shooter);
                    world.spawnEntityInWorld(entity);
                    break;

                case MELEE:
                    raycastDistance = 3f; // reduced to 3 blocks to simulate melee weapons
                case HITSCAN: // fall through intentional, same code used twice otherwise
                    RayTraceResult raycast = MathUtils.raycast(world, shooter.getPositionEyes(1f).addVector(shooter.width/2f, 0, shooter.width/2f), shooter.getLookVec(), raycastDistance, new Predicate<Entity>() {
                        @Override
                        public boolean apply(@Nullable Entity input) {
                            return input != shooter;
                        }
                    });
                    if(raycast != null) {
                        if(raycast.typeOfHit == RayTraceResult.Type.ENTITY && raycast.entityHit instanceof EntityLivingBase) {
                            raycast.entityHit.attackEntityFrom(createMeleeDamage(definition.getId(), shooter), definition.getBaseDamage());
                         // TODO   world.playSound(shooter.posX, shooter.posY, shooter.posZ, MCDoom.instance.chainsawHit, SoundCategory.PLAYERS, 1, 1, false);
                        }
                    }
                    break;
            }
        }
    }

    private static DamageSource createMeleeDamage(String weaponID, EntityLivingBase user) {
        return new EntityDamageSource("melee."+weaponID, user).setProjectile();
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        tooltip.add("Requires "+definition.getAmmoType());
    }

    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        fire(worldIn, entityLiving, stack);
        return stack;
    }

    public int getMaxItemUseDuration(ItemStack stack)
    {
        return definition.getPreFiringPause();
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    public WeaponDefinition getDefinition() {
        return definition;
    }
}
