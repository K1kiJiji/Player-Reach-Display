package kikijiji.playerreachdisplay.filter;



import java.util.List;
import java.util.Locale;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;

import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig;



public final class EntityFilters
{
    private static final String DEFAULT_NAMESPACE = "minecraft";



    /* ----- 생성자 차단 ----- */

    private EntityFilters()
    {
    }



    /* ----- 필터 판단 ----- */

    public static boolean shouldTrack(Entity entity, PlayerReachDisplayConfig config)
    {
        if (entity == null || config == null)
        {
            return false;
        }

        if (!config.enableEntityFilter)
        {
            return true;
        }

        String entityId = getEntityId(entity);

        if (entityId.isEmpty())
        {
            return false;
        }

        if (config.useWhitelist)
        {
            return shouldTrackByWhitelist(entityId, config.whitelist);
        }

        return shouldTrackByBlacklist(entityId, config.blacklist);
    }



    /* ----- 모드별 판단 ----- */

    private static boolean shouldTrackByWhitelist(String entityId, List<String> whitelist)
    {
        if (whitelist == null || whitelist.isEmpty())
        {
            return true;
        }

        return containsEntityId(whitelist, entityId);
    }

    private static boolean shouldTrackByBlacklist(String entityId, List<String> blacklist)
    {
        if (blacklist == null || blacklist.isEmpty())
        {
            return true;
        }

        return !containsEntityId(blacklist, entityId);
    }



    /* ----- ID 정규화 ----- */

    private static String getEntityId(Entity entity)
    {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (id == null)
        {
            return "";
        }

        return normalizeEntityId(id.toString());
    }

    private static boolean containsEntityId(List<String> list, String entityId)
    {
        String normalizedEntityId = normalizeEntityId(entityId);

        if (normalizedEntityId.isEmpty())
        {
            return false;
        }

        for (String raw : list)
        {
            String normalized = normalizeEntityId(raw);

            if (normalized.isEmpty())
            {
                continue;
            }

            if (normalizedEntityId.equals(normalized))
            {
                return true;
            }
        }

        return false;
    }

    private static String normalizeEntityId(String raw)
    {
        if (raw == null)
        {
            return "";
        }

        String value = raw.trim().toLowerCase(Locale.ROOT);

        if (value.isEmpty())
        {
            return "";
        }

        if (!value.contains(":"))
        {
            return DEFAULT_NAMESPACE + ":" + value;
        }

        return value;
    }
}