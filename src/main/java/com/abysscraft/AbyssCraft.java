package com.abysscraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;

public class AbyssCraft implements ModInitializer {
    private static List<Map<String, Object>> depthZones;
    private static List<Map<String, Object>> materials;
    private static Gson gson = new Gson();

    @Override
    public void onInitialize() {
        System.out.println("AbyssCraft: Initializing the Deep...");
        loadSheets();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                int depth = 64 - client.player.getBlockY();
                String zone = getZoneForDepth(depth);
                double pressure = getPressureForZone(zone);
                
                String hud = String.format("§7[§bDEPTH: %dm§7] §7[§eZONE: %s§7] §7[§cPRESS: %.1fatm§7]", 
                                           depth, zone, pressure);
                client.player.sendMessage(Text.literal(hud), true);

                if (client.tickCount % 20 == 0) {
                    performPressureCheck(client);
                }
            }
        });
    }

    private void loadSheets() {
        try {
            String depthContent = new String(Files.readAllBytes(Paths.get("sheets/depth_zones.json")));
            depthZones = gson.fromJson(depthContent, new TypeToken<List<Map<String, Object>>>(){}.getType());
            
            String matContent = new String(Files.readAllBytes(Paths.get("sheets/materials.json")));
            materials = gson.fromJson(matContent, new TypeToken<List<Map<String, Object>>>(){}.getType());
        } catch (IOException e) {
            System.err.println("AbyssCraft: Failed to load sheets!");
        }
    }

    private void performPressureCheck(MinecraftClient client) {
        int depth = 64 - client.player.getBlockY();
        double currentPressure = getPressureForZone(getZoneForDepth(depth));
        var pos = client.player.getBlockPos();
        boolean isSafe = false;
        
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    String blockId = net.minecraft.block.Blocks.get(client.world.getBlockState(pos.offset(x, y, z)).getBlock()).getTranslationKey();
                    if (getMaterialPressure(blockId) >= currentPressure) {
                        isSafe = true;
                        break;
                    }
                }
            }
        }

        if (!isSafe && currentPressure > 1.0) {
            System.out.println("§c[AbyssCraft] HULL STRESS DETECTED: Playing Barotrauma sound...");
            triggerLeak(client, pos);
        }
    }

    private void triggerLeak(MinecraftClient client, net.minecraft.util.math.BlockPos pos) {
        java.util.Random rand = new java.util.Random();
        int dx = rand.nextInt(3) - 1;
        int dy = rand.nextInt(3) - 1;
        int dz = rand.nextInt(3) - 1;
        net.minecraft.util.math.BlockPos leakPos = pos.offset(dx, dy, dz);
        if (client.world.getBlockState(leakPos).isAir()) {
            client.player.sendMessage(Text.literal("§c[!] HULL BREACH: Water is leaking in!"), false);
        }
    }

    private double getMaterialPressure(String blockId) {
        for (Map<String, Object> mat : materials) {
            if (blockId.contains((String) mat.get("id"))) {
                return ((Double) mat.get("pressure_rating")).doubleValue();
            }
        }
        return 0.0;
    }

    private String getZoneForDepth(int depth) {
        for (Map<String, Object> zone : depthZones) {
            int start = ((Double) zone.get("depth_start")).intValue();
            int end = ((Double) zone.get("depth_end")).intValue();
            if (depth >= start && depth <= end) {
                return (String) zone.get("zone");
            }
        }
        return "Unknown";
    }

    private double getPressureForZone(String zoneName) {
        for (Map<String, Object> zone : depthZones) {
            if (zone.get("zone").equals(zoneName)) {
                return (Double) zone.get("pressure");
            }
        }
        return 0.0;
    }
}
