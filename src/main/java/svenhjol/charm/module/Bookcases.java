package svenhjol.charm.module;

import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import svenhjol.charm.Charm;
import svenhjol.charm.block.BookcaseBlock;
import svenhjol.charm.blockentity.BookcaseBlockEntity;
import svenhjol.charm.gui.BookcaseScreen;
import svenhjol.charm.screenhandler.BookcaseScreenHandler;
import svenhjol.meson.MesonModule;
import svenhjol.meson.enums.IVariantMaterial;
import svenhjol.meson.enums.VanillaVariantMaterial;
import svenhjol.meson.helper.EnchantmentsHelper;
import svenhjol.meson.iface.Config;
import svenhjol.meson.iface.Module;

import java.util.*;

@Module(description = "Bookshelves that can hold up to 9 stacks of books and maps.")
public class Bookcases extends MesonModule {
    public static final Identifier ID = new Identifier(Charm.MOD_ID, "bookcase");
    public static final Map<IVariantMaterial, BookcaseBlock> BOOKCASE_BLOCKS = new HashMap<>();

    public static ScreenHandlerType<BookcaseScreenHandler> SCREEN_HANDLER;
    public static BlockEntityType<BookcaseBlockEntity> BLOCK_ENTITY;

    public static List<Class<? extends Item>> validItems = new ArrayList<>();

    @Config(name = "Valid books", description = "Additional items that may be placed in bookcases.")
    public static List<String> configValidItems = Arrays.asList(
        "strange:scroll"
    );

    @Override
    public void register() {
        validItems.addAll(Arrays.asList(
            Items.BOOK.getClass(),
            Items.ENCHANTED_BOOK.getClass(),
            Items.WRITTEN_BOOK.getClass(),
            Items.WRITABLE_BOOK.getClass(),
            Items.KNOWLEDGE_BOOK.getClass(),
            Items.PAPER.getClass(),
            Items.MAP.getClass(),
            Items.FILLED_MAP.getClass()
        ));

        VanillaVariantMaterial.getTypes().forEach(type -> {
            BOOKCASE_BLOCKS.put(type, new BookcaseBlock(this, type));
        });

        configValidItems.forEach(string -> {
            Item item = Registry.ITEM.get(new Identifier(string));
            validItems.add(item.getClass());
        });

        SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(ID, BookcaseScreenHandler::new);
        BLOCK_ENTITY = BlockEntityType.Builder.create(BookcaseBlockEntity::new).build(null);
        Registry.register(Registry.BLOCK_ENTITY_TYPE, ID, BLOCK_ENTITY);
    }

    @Override
    public void init() {
        EnchantmentsHelper.ENCHANTING_BLOCKS.addAll(BOOKCASE_BLOCKS.values());
    }

    @Override
    public void clientInit() {
        ScreenRegistry.register(SCREEN_HANDLER, BookcaseScreen::new);
    }

    public static boolean canContainItem(ItemStack stack) {
        return validItems.contains(stack.getItem().getClass());
    }
}
