package svenhjol.charm.brewing.module;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import svenhjol.charm.Charm;
import svenhjol.charm.base.CharmCategories;
import svenhjol.charm.brewing.block.FlavoredCakeBlock;
import svenhjol.charm.base.message.MessageCakeImbue;
import svenhjol.meson.MesonModule;
import svenhjol.meson.handler.PacketHandler;
import svenhjol.meson.helper.ComposterHelper;
import svenhjol.meson.helper.PlayerHelper;
import svenhjol.meson.iface.Config;
import svenhjol.meson.iface.Module;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Module(mod = Charm.MOD_ID, category = CharmCategories.BREWING, hasSubscriptions = true,
    description = "Right-click a Long Potion on a cake to make a Flavored Cake that gives the potion effect after eating each slice." +
        "You can also make a Flavored Cake using a dispenser.")
public class FlavoredCake extends MesonModule
{
    @Config(
        name = "Effect duration multiplier",
        description = "Effect duration of a single slice of cake as a multiplier of the original potion duration.")
    public static double multiplier = 0.1D;

    public static List<String> validPotions = Arrays.asList(
        "swiftness",
        "strength",
        "leaping",
        "regeneration",
        "fire_resistance",
        "water_breathing",
        "invisibility",
        "night_vision"
    );
    public static Map<Potion, FlavoredCakeBlock> types = new HashMap<>();
    public static Map<String, FlavoredCakeBlock> cakes = new HashMap<>();

    @Override
    public void init()
    {
        validPotions.forEach(potion -> new FlavoredCakeBlock(this, potion));
    }

    @Override
    public void setup(FMLCommonSetupEvent event)
    {
        // add dispenser behavior for potions so that they can be used to imbue cakes
        DispenserBlock.registerDispenseBehavior(Items.POTION, new DispenseImbueBehavior());

        // add cakes to composter
        cakes.forEach((s, flavoredCakeBlock) -> ComposterHelper.addCompostableItem(new ItemStack(flavoredCakeBlock), 1.0f));
    }

    @SubscribeEvent
    public void onPotionUse(RightClickBlock event)
    {
        // check held potion, imbue and return
        ItemStack held = event.getEntityPlayer().getHeldItem(event.getHand());
        boolean result = FlavoredCake.imbue(event.getWorld(), event.getPos(), held);
        if (result) {
            if (!event.getWorld().isRemote) {
                PlayerHelper.addOrDropStack(event.getEntityPlayer(), new ItemStack(Items.GLASS_BOTTLE));
            }
            event.setCanceled(true);
        }
    }

    public static boolean imbue(World world, BlockPos pos, ItemStack stack)
    {
        Block block = world.getBlockState(pos).getBlock();
        if (block == Blocks.CAKE || cakes.values().contains(block)) {
            Potion potion = PotionUtils.getPotionFromItem(stack);

            if (types.containsKey(potion)) {
                BlockState current = world.getBlockState(pos);
                BlockState imbued = types.get(potion).getDefaultState().with(FlavoredCakeBlock.BITES, current.get(FlavoredCakeBlock.BITES));
                world.setBlockState(pos, imbued, 2);
                stack.shrink(1);

                // let clients know the cake has been imbued
                if (!world.isRemote) {
                    PacketHandler.sendToAll(new MessageCakeImbue(pos));
                }
                return true;
            }
        }

        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public static void effectImbued(BlockPos pos)
    {
        World world = Minecraft.getInstance().world;
        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 0.5F, 1.1F);
        for (int i = 0; i < 8; ++i) {
            double d0 = world.rand.nextGaussian() * 0.02D;
            double d1 = world.rand.nextGaussian() * 0.02D;
            double d2 = world.rand.nextGaussian() * 0.02D;
            double dx = (float)pos.getX() + MathHelper.clamp(world.rand.nextFloat(), 0.25f, 0.75f);
            double dy = (float)pos.getY() + 0.65f;
            double dz = (float)pos.getZ() + MathHelper.clamp(world.rand.nextFloat(), 0.25f, 0.75f);
            world.addParticle(ParticleTypes.WITCH, dx, dy, dz, d0, d1, d2);
        }
    }

    public static class DispenseImbueBehavior extends DefaultDispenseItemBehavior
    {
        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack)
        {
            World world = source.getWorld();
            BlockPos pos = source.getBlockPos().offset(source.getBlockState().get(DispenserBlock.FACING));
            boolean result = imbue(world, pos, stack);

            if (!result) return super.dispenseStack(source, stack);
            return stack;
        }
    }
}