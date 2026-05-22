package kikijiji.playerreachdisplay.config;



import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.ArrayList;



public class PlayerReachDisplayConfig
{
    /* ----- 텍스트 레이어 ----- */

    public boolean showReach      = true;
    public boolean showShadow     = true;
    public boolean showBackground = true;

    public int textColor       = 0xFFFFFFFF;
    public int shadowColor     = 0xFF000000;
    public int backgroundColor = 0x50000000;



    /* ----- 크기 ----- */

    public int scale = 100;



    /* ----- 위치 ----- */

    public int offsetX = 0;
    public int offsetY = 0;

    public boolean useRelativePosition = false;

    public double positionX = 0.0;
    public double positionY = 0.0;



    /* ----- 엔티티 필터 ----- */

    public boolean enableEntityFilter = false;

    public boolean useWhitelist = true;

    public List<String> whitelist = new ArrayList<>();
    public List<String> blacklist = new ArrayList<>();



    /* ----- 거리별 색상 ----- */

    public boolean enableDistanceColor = false;

    public static final int MIN_DISTANCE_BAND_COUNT = 2;
    public static final int MAX_DISTANCE_BAND_COUNT = 5;

    public static final double MIN_DISTANCE_BAND_FROM = 0.0;
    public static final double MAX_DISTANCE_BAND_FROM = 10.0;

    public static final double DISTANCE_BAND_GAP = 0.01;

    public List<DistanceColorBand> distanceBands = new ArrayList<>();

    public static class DistanceColorBand
    {
        @SerializedName(value = "fromDistance", alternate = "maxDistance")
        public double fromDistance;

        public int    textColor;
        public int    shadowColor;
        public int    backgroundColor;
    }

    public PlayerReachDisplayConfig()
    {
        DistanceColorBand band1 = new DistanceColorBand();
        band1.fromDistance      = 0.0;
        band1.textColor         = 0xFF00FF00;
        band1.shadowColor       = 0xFF003300;
        band1.backgroundColor   = 0x40003300;
        distanceBands.add(band1);

        DistanceColorBand band2 = new DistanceColorBand();
        band2.fromDistance      = 1.0;
        band2.textColor         = 0xFFFFFF00;
        band2.shadowColor       = 0xFF333300;
        band2.backgroundColor   = 0x40FFFF00;
        distanceBands.add(band2);

        DistanceColorBand band3 = new DistanceColorBand();
        band3.fromDistance      = 2.0;
        band3.textColor         = 0xFFFFA500;
        band3.shadowColor       = 0xFF331A00;
        band3.backgroundColor   = 0x40FFA500;
        distanceBands.add(band3);

        DistanceColorBand band4 = new DistanceColorBand();
        band4.fromDistance      = 3.0;
        band4.textColor         = 0xFFFF0000;
        band4.shadowColor       = 0xFF330000;
        band4.backgroundColor   = 0x40FF0000;
        distanceBands.add(band4);
    }



    /* ----- 표시 유지 방식 ----- */

    public boolean keepLastHitDistance = true;

    public double resetAfterSeconds = 15.0;



    /* ----- 표시 포맷 ----- */

    public enum DisplayMode
    {
        NUMBER_ONLY,
        WITH_BLOCKS,
        WITH_M
    }

    public DisplayMode displayMode = DisplayMode.WITH_BLOCKS;



    /* ----- 복사 ----- */

    public PlayerReachDisplayConfig copy()
    {
        PlayerReachDisplayConfig c = new PlayerReachDisplayConfig();


        // 텍스트 레이어
        c.showReach      = this.showReach;
        c.showShadow     = this.showShadow;
        c.showBackground = this.showBackground;

        c.textColor       = this.textColor;
        c.shadowColor     = this.shadowColor;
        c.backgroundColor = this.backgroundColor;


        // 크기
        c.scale = this.scale;


        // 위치
        c.offsetX = this.offsetX;
        c.offsetY = this.offsetY;

        c.useRelativePosition = this.useRelativePosition;

        c.positionX = this.positionX;
        c.positionY = this.positionY;


        // 엔티티 필터
        c.enableEntityFilter = this.enableEntityFilter;

        c.useWhitelist = this.useWhitelist;

        c.whitelist = this.whitelist == null ? new ArrayList<>() : new ArrayList<>(this.whitelist);
        c.blacklist = this.blacklist == null ? new ArrayList<>() : new ArrayList<>(this.blacklist);


        // 거리별 색상
        c.enableDistanceColor = this.enableDistanceColor;

        c.distanceBands.clear();

        if (this.distanceBands != null)
        {
            for (DistanceColorBand band : this.distanceBands)
            {
                if (band == null)
                {
                    continue;
                }

                DistanceColorBand copyBand = new DistanceColorBand();

                copyBand.fromDistance = band.fromDistance;
                copyBand.textColor = band.textColor;
                copyBand.shadowColor = band.shadowColor;
                copyBand.backgroundColor = band.backgroundColor;

                c.distanceBands.add(copyBand);
            }
        }


        // 표시 유지 방식
        c.keepLastHitDistance = this.keepLastHitDistance;

        c.resetAfterSeconds = this.resetAfterSeconds;


        // 표시 포맷
        c.displayMode = this.displayMode;


        return c;
    }
}