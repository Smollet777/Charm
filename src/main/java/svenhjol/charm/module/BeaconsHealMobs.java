package svenhjol.charm.module;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import svenhjol.meson.Meson;
import svenhjol.meson.MesonModule;
import svenhjol.meson.iface.Module;

import java.util.List;
@Module(description = "Passive and friendly mobs will heal themselves within range of a beacon with the regeneration effect.")
public class BeaconsHealMobs extends MesonModule {
    public static void healInBeaconRange(World world, int levels, BlockPos pos, StatusEffect primaryEffect, StatusEffect secondaryEffect) {
        if (!Meson.enabled("charm:beacons_heal_mobs"))
            return;

        if (!world.isClient) {
            double d0 = levels * 10 + 10;
            Box bb = (new Box(pos)).expand(d0).expand(0.0D, world.getHeight(), 0.0D);

            if (primaryEffect == StatusEffects.REGENERATION || secondaryEffect == StatusEffects.REGENERATION) {
                List<PassiveEntity> list = world.getNonSpectatingEntities(PassiveEntity.class, bb);
                list.forEach(mob -> mob.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 4 * 20, 1)));
            }
        }
    }
}
