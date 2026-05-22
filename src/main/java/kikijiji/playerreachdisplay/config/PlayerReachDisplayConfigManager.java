package kikijiji.playerreachdisplay.config;



import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;

import java.nio.file.Path;
import java.nio.file.Files;

import net.fabricmc.loader.api.FabricLoader;

import kikijiji.playerreachdisplay.PlayerReachDisplay;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig.DistanceColorBand;



public class PlayerReachDisplayConfigManager
{
    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "player-reach-display.json";



    /* ----- 불러오기 ----- */

    public static PlayerReachDisplayConfig load()
    {
        Path configDir  = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve(FILE_NAME);

        if (Files.exists(configPath))
        {
            try (Reader reader = Files.newBufferedReader(configPath))
            {
                PlayerReachDisplayConfig config = GSON.fromJson(reader, PlayerReachDisplayConfig.class);

                if (config != null)
                {
                    PlayerReachDisplayConfig normalized = normalize(config);
                    save(normalized);
                    return normalized;
                }
            }
            catch (IOException | JsonParseException exception)
            {
                PlayerReachDisplay.LOGGER.error("Failed to load Player Reach Display config from {}", configPath, exception);
            }
        }

        PlayerReachDisplayConfig config = new PlayerReachDisplayConfig();
        save(config);
        return config;
    }



    /* ----- 저장 ----- */

    public static void save(PlayerReachDisplayConfig config)
    {
        if (config == null)
        {
            config = new PlayerReachDisplayConfig();
        }

        config = normalize(config);

        Path configDir  = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve(FILE_NAME);

        try
        {
            if (!Files.exists(configDir))
            {
                Files.createDirectories(configDir);
            }

            try (Writer writer = Files.newBufferedWriter(configPath))
            {
                GSON.toJson(config, writer);
            }
        }
        catch (IOException | JsonParseException exception)
        {
            PlayerReachDisplay.LOGGER.error("Failed to save Player Reach Display config to {}", configPath, exception);
        }
    }



    /* ----- 설정 보정 ----- */

    private static PlayerReachDisplayConfig normalize(PlayerReachDisplayConfig config)
    {
        PlayerReachDisplayConfig defaults = new PlayerReachDisplayConfig();

        if (config.whitelist == null)
        {
            config.whitelist = new ArrayList<>();
        }

        if (config.blacklist == null)
        {
            config.blacklist = new ArrayList<>();
        }

        if (config.distanceBands == null || config.distanceBands.isEmpty())
        {
            config.distanceBands = copyDistanceBands(defaults.distanceBands);
        }

        config.distanceBands.removeIf(Objects::isNull);

        if (config.distanceBands.isEmpty())
        {
            config.distanceBands = copyDistanceBands(defaults.distanceBands);
        }

        while (config.distanceBands.size() < PlayerReachDisplayConfig.MIN_DISTANCE_BAND_COUNT)
        {
            config.distanceBands.add(createDistanceBandFromLast(config.distanceBands));
        }

        while (config.distanceBands.size() > PlayerReachDisplayConfig.MAX_DISTANCE_BAND_COUNT)
        {
            config.distanceBands.removeLast();
        }

        for (PlayerReachDisplayConfig.DistanceColorBand band : config.distanceBands)
        {
            band.fromDistance = Math.clamp
            (
                    band.fromDistance,
                    PlayerReachDisplayConfig.MIN_DISTANCE_BAND_FROM,
                    PlayerReachDisplayConfig.MAX_DISTANCE_BAND_FROM
            );
        }

        config.distanceBands.sort(Comparator.comparingDouble(band -> band.fromDistance));

        if (!config.distanceBands.isEmpty())
        {
            config.distanceBands.getFirst().fromDistance = 0.0;
        }

        for (int i = 1; i < config.distanceBands.size(); i++)
        {
            DistanceColorBand previous = config.distanceBands.get(i - 1);
            DistanceColorBand current  = config.distanceBands.get(i);

            double min = previous.fromDistance + PlayerReachDisplayConfig.DISTANCE_BAND_GAP;
            double max = PlayerReachDisplayConfig.MAX_DISTANCE_BAND_FROM - PlayerReachDisplayConfig.DISTANCE_BAND_GAP * (config.distanceBands.size() - 1 - i);

            current.fromDistance = Math.clamp
            (
                    current.fromDistance,
                    min,
                    Math.max(min, max)
            );
        }

        config.scale = Math.clamp(config.scale, 10, 400);

        config.resetAfterSeconds = Math.clamp(config.resetAfterSeconds, 0.0, 600.0);

        config.positionX = clamp01(config.positionX);
        config.positionY = clamp01(config.positionY);

        return config;
    }


    /* ----- 거리 색상 복사 ----- */

    private static List<DistanceColorBand> copyDistanceBands(List<DistanceColorBand> source)
    {
        List<DistanceColorBand> result = new ArrayList<>();

        if (source == null)
        {
            return result;
        }

        for (DistanceColorBand band : source)
        {
            if (band == null)
            {
                continue;
            }

            DistanceColorBand copy = new DistanceColorBand();

            copy.fromDistance    = band.fromDistance;
            copy.textColor       = band.textColor;
            copy.shadowColor     = band.shadowColor;
            copy.backgroundColor = band.backgroundColor;

            result.add(copy);
        }

        return result;
    }

    /* ----- 거리 색상 추가 ----- */

    private static DistanceColorBand createDistanceBandFromLast(List<DistanceColorBand> bands)
    {
        DistanceColorBand band = new DistanceColorBand();

        if (bands == null || bands.isEmpty())
        {
            band.fromDistance    = 1.0;
            band.textColor       = 0xFFFFFFFF;
            band.shadowColor     = 0xFF000000;
            band.backgroundColor = 0x50000000;

            return band;
        }

        DistanceColorBand last = bands.getLast();

        band.fromDistance = Math.clamp
        (
                last.fromDistance + 1.0,
                PlayerReachDisplayConfig.MIN_DISTANCE_BAND_FROM,
                PlayerReachDisplayConfig.MAX_DISTANCE_BAND_FROM
        );

        band.textColor       = last.textColor;
        band.shadowColor     = last.shadowColor;
        band.backgroundColor = last.backgroundColor;

        return band;
    }

    private static double clamp01(double value)
    {
        return Math.clamp(value, 0.0, 1.0);
    }
}