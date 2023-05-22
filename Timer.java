/*
 * Decompiled with CFR 0.150.
 */
package a;

@Module.registerModule(name ="Timer", alias="Timer", description="Ускоряет игру", category= ModuleCategory.MOVEMENT)
public class Timer extends Module{
    public final NumberSetting timerSpeed = new NumberSetting("Скорость", 1.8f, 1.1f, 5.0f, 0.1f);
    public final NumberSetting decreaseRate = new NumberSetting("Скорость убывания", 1.0f, 0.5f, 3.0f, 0.1f).VisibleIf(smart::getValue);
    public final BooleanSetting smart = new BooleanSetting("Умный", true);
    public final BooleanSetting indicator = new BooleanSetting("Индикатор", true).visibleIf(smart::getValue);
    public final NumberSetting addOnTheMove = new NumberSetting("Добавлять в движении", 0.0f, 0.0f, 1.0f, 0.05f).VisibleIf(smart::getValue);
    private static float violation = 0.0f;
    private double prevPosX;
    private double prevPosY;
    private double prevPosZ;

    private float yaw;
    private float pitch;

    public Timer() {
        register(timerSpeed, smart, indicator, decreaseRate, addOnTheMove);
    }

    public static float getViolation() {
        return violation;
    }

    @Override
    public void onDisable(Module module) {
        if (module == this) {
            AttackAura.cpsLimit = mc.player.fallDistance > 0.0f ? 7 : (mc.player.getCooledAttackStrength(0.0f) > 0.9 ? 5 : 9);
            mc.timer.timerSpeed = 1.0f;
        }
        super.onDisable(module);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            mc.timer.timerSpeed = timerSpeed.getNumberValue();
            if (!smart.getValue() || mc.timer.timerSpeed <= 1.0f) {
                return;
            }
            if (violation < 90f / timerSpeed.getNumberValue()) {
                violation += decreaseRate.getNumberValue();
                violation = MathHelper.clamp(violation, 0.0f, 100f / timerSpeed.getNumberValue());
            } else {
                toggle();
            }
        }
    }


    public void updateTimer(float rotationYaw, float rotationPitch, double posX, double posY, double posZ) { // -> EntityPlayerSP updateWalkingPlayer()
        violation = notMoving() ? (float)(violation - (decreaseRate.getNumberValue() + 0.4)) : violation - (addOnTheMove.getNumberValue() / 10.0f);
        violation = (float) MathHelper.clamp(violation, 0.0, Math.floor(100f / mc.timer.timerSpeed));
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        yaw = rotationYaw;
        pitch = rotationPitch;
    }

    private boolean notMoving() {
        return prevPosX == mc.player.posX
                && prevPosY == mc.player.posY
                && prevPosZ == mc.player.posZ
                && yaw == mc.player.rotationYaw
                && pitch == mc.player.rotationPitch;
    }
}

