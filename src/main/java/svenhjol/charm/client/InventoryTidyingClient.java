package svenhjol.charm.client;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import svenhjol.charm.base.CharmResources;
import svenhjol.charm.event.RenderGuiCallback;
import svenhjol.charm.event.SetupGuiCallback;
import svenhjol.charm.gui.BookcaseScreen;
import svenhjol.charm.gui.CrateScreen;
import svenhjol.charm.mixin.accessor.SlotAccessor;
import svenhjol.charm.module.InventoryTidying;
import svenhjol.meson.MesonModule;
import svenhjol.meson.helper.ScreenHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static svenhjol.charm.handler.InventoryTidyingHandler.PLAYER;
import static svenhjol.charm.handler.InventoryTidyingHandler.BE;

public class InventoryTidyingClient {
    private static final int LEFT = 159;
    private static final int TOP = 12;
    private final MesonModule module;
    private final List<TexturedButtonWidget> sortingButtons = new ArrayList<>();

    public final List<Class<? extends Screen>> blockEntityScreens = new ArrayList<>();
    public final List<Class<? extends Screen>> blacklistScreens = new ArrayList<>();

    public InventoryTidyingClient(MesonModule module) {
        this.module = module;

        if (!module.enabled)
            return;

        blockEntityScreens.addAll(Arrays.asList(
            GenericContainerScreen.class,
            HopperScreen.class,
            ShulkerBoxScreen.class,
            CrateScreen.class,
            BookcaseScreen.class,
            Generic3x3ContainerScreen.class
        ));

        blacklistScreens.addAll(Arrays.asList(
            CreativeInventoryScreen.class
        ));

        // set up client listeners
        SetupGuiCallback.EVENT.register(((client, width, height, buttons, addButton) -> {
            if (client.player == null)
                return;

            if (!(client.currentScreen instanceof HandledScreen))
                return;

            if (blacklistScreens.contains(client.currentScreen.getClass()))
                return;

            this.sortingButtons.clear();

            HandledScreen<?> screen = (HandledScreen<?>)client.currentScreen;
            ScreenHandler screenHandler = screen.getScreenHandler();

            int x = ScreenHelper.getX(screen) + LEFT;
            int y = ScreenHelper.getY(screen) - TOP;

            List<Slot> slots = screenHandler.slots;
            for (Slot slot : slots) {
                if (blockEntityScreens.contains(screen.getClass()) && ((SlotAccessor)slot).getIndex() == 0) {
                    this.addSortingButton(screen, x, y + slot.y, click -> sendSortMessage(BE));
                }

                if (slot.inventory == client.player.inventory) {
                    if (screen instanceof InventoryScreen)
                        y += 76;

                    this.addSortingButton(screen, x, y + slot.y, click -> sendSortMessage(PLAYER));
                    break;
                }
            }

            this.sortingButtons.forEach(addButton);
        }));

        RenderGuiCallback.EVENT.register(((client, matrices, mouseX, mouseY, delta) -> {
            if (client.currentScreen instanceof InventoryScreen
                && !blacklistScreens.contains(client.currentScreen.getClass())
            ) {
                // handles the recipe being open/closed
                InventoryScreen screen = (InventoryScreen)client.currentScreen;
                int x = ScreenHelper.getX(screen);
                this.sortingButtons.forEach(button -> button.setPos(x + LEFT, button.y));
            }
        }));
    }

    private void addSortingButton(Screen screen, int x, int y, ButtonWidget.PressAction onPress) {
        this.sortingButtons.add(new TexturedButtonWidget(x, y, 10, 10, 40, 0, 10, CharmResources.INVENTORY_BUTTONS, onPress));
    }

    private void sendSortMessage(int type) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(type);
        ClientSidePacketRegistry.INSTANCE.sendToServer(InventoryTidying.MSG_SERVER_TIDY_INVENTORY, data);
    }
}