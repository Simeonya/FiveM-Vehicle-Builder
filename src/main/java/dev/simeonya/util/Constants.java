package dev.simeonya.util;

import java.util.Set;

public final class Constants {

    private Constants() {
    }

    public static final Set<String> STREAM_EXTENSIONS = Set.of("yft", "ytd", "ydd", "ydr", "ybn", "ymt", "ycd", "ynv", "awc");

    public static final Set<String> META_NAMES = Set.of("vehicles.meta", "carcols.meta", "carvariations.meta", "handling.meta", "vehiclelayouts.meta");

    public static final String MANIFEST_CONTENT = """
            fx_version 'cerulean'
            game 'gta5'
            
            files {
                'data/**/vehicles.meta',
                'data/**/carcols.meta',
                'data/**/carvariations.meta',
                'data/**/handling.meta',
                'data/**/vehiclelayouts.meta'
            }
            
            data_file 'VEHICLE_METADATA_FILE' 'data/**/vehicles.meta'
            data_file 'CARCOLS_FILE' 'data/**/carcols.meta'
            data_file 'VEHICLE_VARIATION_FILE' 'data/**/carvariations.meta'
            data_file 'HANDLING_FILE' 'data/**/handling.meta'
            data_file 'VEHICLE_LAYOUTS_FILE' 'data/**/vehiclelayouts.meta'
            """;
}