/*
 * Decompiled with CFR 0.150.
 */
package a;

@Module.registerModule(name ="KTLeave", alias="KT Leave", description="Телепортирует вас в КТ в другое место", category= ModuleCategory.PLAYER)
public class KTLeave extends Module {


    // Без сомнения, базовый модуль для целестиала

    @Override
    public void onEnable(Module module) {
        if (module == this) {
            int xCoord = 8000 + interpolateRandom(-6500, 6500);
            int zCoord = 8000 + interpolateRandom(-6500, 6500);
            if (CelestialManager.isReallyWorld()) {
                for (int i = 0; i < 12; ++i) {
                    mc.player.connection.sendPacket(new CPacketPlayer.Position(xCoord, 180.0, zCoord, true));
                    mc.player.connection.sendPacket(new CPacketPlayer.Position(xCoord, 180.0, zCoord, true));
                }
                for (int i = 0; i < 12; ++i) {
                    mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, 180.0, mc.player.posZ, true));
                    mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, 180.0, mc.player.posZ, true));
                }
            } else {
                mc.player.setPosition(xCoord, 3.0, zCoord);
            }
        }
        super.onEnable(module);
    }

    public static int interpolateRandom(int a, int b) {
        return (int)(Math.random() * (double)(b - a) + (double)a);
    }
}

