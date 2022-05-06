package xyz.kubasz.personalspace;

import net.minecraft.world.WorldProvider;

public class PersonalWorldProvider extends WorldProvider {
    @Override
    public String getDimensionName() {
        return "Personal World";
    }
}
