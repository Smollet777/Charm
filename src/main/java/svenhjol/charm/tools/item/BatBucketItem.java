package svenhjol.charm.tools.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import svenhjol.charm.base.message.MessageGlowing;
import svenhjol.charm.tools.module.BatInABucket;
import svenhjol.meson.MesonItem;
import svenhjol.meson.MesonModule;
import svenhjol.meson.handler.PacketHandler;
import svenhjol.meson.helper.ItemNBTHelper;
import svenhjol.meson.helper.SoundHelper;

public class BatBucketItem extends MesonItem
{
    public static final String BAT_SIGNAL = "stored_bat";

    public BatBucketItem(MesonModule module)
    {
        super(module, "bat_bucket", new Item.Properties()
            .group(ItemGroup.MISC)
            .maxStackSize(1));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        BlockPos pos = player.getPosition();
        Direction facing = player.getHorizontalFacing();
        ItemStack held = player.getHeldItem(hand);

        double x = pos.getX() + 0.5 + facing.getXOffset();
        double y = pos.getY() + 0.5 + facing.getYOffset();
        double z = pos.getZ() + 0.5 + facing.getZOffset();

        if (world.isRemote) {
            SoundHelper.playSoundAtPos(player, SoundEvents.ENTITY_BAT_TAKEOFF, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        } else {
            if (!player.isCreative()) {
                // spawn the bat
                BatEntity bat = new BatEntity(EntityType.BAT, world);
                CompoundNBT data = ItemNBTHelper.getCompound(held, BAT_SIGNAL);
                if (!data.isEmpty()) {
                    bat.read(data);
                }

                bat.setPosition(x, y, z);
                world.addEntity(bat);
            }
            player.swingArm(hand);

            // send client message to start glowing
            PacketHandler.sendTo(new MessageGlowing(BatInABucket.range, BatInABucket.time * 20), (ServerPlayerEntity)player);
        }

        if (!player.isCreative()) {
            player.setHeldItem(hand, new ItemStack(Items.BUCKET));
        }

        /* @todo Use stats */

        return new ActionResult<>(ActionResultType.SUCCESS, held);
    }
}
