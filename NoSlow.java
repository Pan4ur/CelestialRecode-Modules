/*
 * Decompiled with CFR 0.150.
 */
package a;

@Module.registerModule(name ="NoSlow", alias ="No Slow", description="Убирает замедление при использовании предмета", category = ModuleCategory.MOVEMENT)
public class NoSlow extends Module {
    
    public final ModeSetting mode = new ModeSetting("Режим", "Matrix", "Matrix", "Really World", "Старый Sunrise", "Vanilla");
    public final NumberSetting speed = new NumberSetting("Скорость", 100.0f, 10.0f, 100.0f, 10.0f).VisibleIf(() -> mode.selectedMode.equals("Vanilla"));

    public NoSlow() {
        register(mode, speed);
    }

    public final boolean canNoSlow() {
        if (mc.player.isPotionActive(MobEffects.SLOWNESS)) {
            return false;
        }
        ModuleManager mm = Celestial.INSTANCE.moduleManager;
        if (mm.waterSpeed.isActive()) {
            return false;
        }
        if (mm.strafe.isStrafing()) {
            return false;
        }
        return mc.gameSettings.keyBindJump.isPressed() || mc.player.onGround || mc.player.movementInput.jump || !(mc.player.fallDistance < 1.0f);
    }


        /*
            это в EntityPlayerSP  onLivingUpdate()


            if (this.isHandActive() && !this.isRiding()) {
            NoSlowEvent e = new NoSlowEvent();
            EventBus.post(e);
            if (!e.isCanceled()) {
                bl2 = ((Module)Celestial.INSTANCE.moduleManager.noSlow).isEnabled() && Celestial.INSTANCE.moduleManager.noSlow.canNoSlow();
                float f2 = Celestial.INSTANCE.moduleManager.noSlow.speed.getNumberValue() / 100.0f;
                this.movementInput.moveStrafe *= bl2 ? f2 : 0.2f;
                this.movementInput.moveForward *= bl2 ? f2 : 0.2f;
                this.FX = 0;
            }
        }
     */

    @Override
    public void onEvent(Event event2) {
        if (event2 instanceof NoSlowEvent && !mode.selectedMode.equals("Vanilla") && canNoSlow()) { // -> fB:675 (EntityPlayerSP)
            event2.cancel();
        }
        if (event2 instanceof EntitySyncEvent) {
            EntitySyncEvent entitySyncEvent = (EntitySyncEvent) event2;
            if (entitySyncEvent.getType() != EntitySyncEvent.Types.Pre) {
                return;
            }
            if (mode.selectedMode.equals("Really World")) {
                if (!CelestialManager.isReallyWorld()) { // походу это анти пакетлог
                    return;
                }
                if (!mc.player.isUsingItem() || mc.player.getItemInUseMaxCount() != 2) {
                    return;
                }
                mc.player.connection.sendPacket(new CPacketHeldItemChange(mc.player.inventory.currentItem % 8 + 1));
                mc.player.connection.sendPacket(new CPacketHeldItemChange(mc.player.inventory.currentItem));
            }
        }
        if (event2 instanceof EventUpdate) {
            if (!canNoSlow()) {
                return;
            }
            switch (mode.selectedMode) {
                case "Really World": {
                    if (!mc.player.isUsingItem() || !mc.player.onGround || mc.player.movementInput.jump) break;
                    float f3 = (1.0f - ((EntityLivingBase)mc.player).landMovementFactor) * 0.9f;
                    f3 *= 0.3f;
                    f3 = (float)((double)f3 + 0.055);
                    mc.player.motionX *= (double)f3;
                    mc.player.motionZ *= (double)f3;
                    break;
                }
                case "Matrix": {
                    boolean bl2 = (double)mc.player.fallDistance > 0.725;
                    if (!mc.player.isUsingItem()) break;
                    if (mc.player.onGround && !mc.player.movementInput.jump) {
                        if (mc.player.ticksExisted % 2 != 0) break;
                        float f4 = mc.player.moveStrafing == 0.0f ? 0.5f : 0.4f;
                        mc.player.motionX *= (double)f4;
                        mc.player.motionZ *= (double)f4;
                        break;
                    }
                    if (!bl2) break;
                    float f5 = mc.player.fallDistance > 1.4 ? 0.95f : 0.97f;
                    mc.player.motionX *= (double)f5;
                    mc.player.motionZ *= (double)f5;
                    break;
                }
                case "Старый Sunrise": {
                    if (!mc.player.isUsingItem()) break;
                    if (mc.player.onGround && !mc.gameSettings.keyBindJump.isPressed()) {
                        if (mc.player.ticksExisted % 2 != 0) break;
                        mc.player.motionX *= 0.47;
                        mc.player.motionZ *= 0.47;
                        break;
                    }
                    if (!((double)mc.player.fallDistance > 0.2)) break;
                    mc.player.motionX *= (double)0.93f;
                    mc.player.motionZ *= (double)0.93f;
                }
            }
        }
    }
}

