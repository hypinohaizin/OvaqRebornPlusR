package net.shoreline.client.mixin.accessor;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author h_ypi
 * @since 1.0
 */
@Mixin(TextFieldWidget.class)
public interface AccessorTextFieldWidget
{
    /**
     * @return
     * @see TextFieldWidget#drawsBackground
     */
    @Accessor("drawsBackground")
    boolean isDrawsBackground();
}
