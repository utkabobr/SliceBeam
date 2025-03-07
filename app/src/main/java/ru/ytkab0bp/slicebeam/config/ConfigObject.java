package ru.ytkab0bp.slicebeam.config;

import java.util.HashMap;
import java.util.Map;

import ru.ytkab0bp.slicebeam.BuildConfig;
import ru.ytkab0bp.slicebeam.R;
import ru.ytkab0bp.slicebeam.SliceBeam;
import ru.ytkab0bp.slicebeam.fragment.ProfileListFragment;

/** @noinspection CopyConstructorMissesField*/
public class ConfigObject implements ProfileListFragment.ProfileListItem {
    public final static int PROFILE_LIST_PRINT = 0, PROFILE_LIST_FILAMENT = 1, PROFILE_LIST_PRINTER = 2;

    private String title;
    public Map<String, String> values = new HashMap<>();

    // Used only in setup
    public String thumbnailUrl;

    // Type for isSelected()
    public int profileListType;

    public ConfigObject() {
        title = null;
    }

    public ConfigObject(String title) {
        this.title = title;
    }

    public ConfigObject(ConfigObject from) {
        this.title = from.title;
        this.values.putAll(from.values);
    }

    public String get(String key) {
        return values.get(key);
    }

    public void put(String key, String value) {
        values.put(key, value);
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean isSelected() {
        switch (profileListType) {
            case PROFILE_LIST_PRINT:
                return getTitle().equals(SliceBeam.CONFIG.presets.get("print"));
            case PROFILE_LIST_FILAMENT:
                return getTitle().equals(SliceBeam.CONFIG.presets.get("filament"));
            case PROFILE_LIST_PRINTER:
                return getTitle().equals(SliceBeam.CONFIG.presets.get("printer"));
        }
        return false;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("# generated by Slice Beam ").append(BuildConfig.VERSION_NAME).append("\n\n");
        for (Map.Entry<String, String> en : values.entrySet()) {
            sb.append(en.getKey()).append(" = ").append(en.getValue().replace("\n", "\\n")).append("\n");
        }
        return sb.toString();
    }

    public static ConfigObject createCustomPrinterProfile() {
        ConfigObject custom = new ConfigObject(SliceBeam.INSTANCE.getString(R.string.IntroCustomProfileName));
        custom.put("printer_technology", "FFF");
        custom.put("bed_shape", "0x0,200x0,200x200,0x200");
        custom.put("binary_gcode", "0");
        custom.put("gcode_flavor", "marlin");
        custom.put("max_print_height", "200");
        custom.put("min_layer_height", "0.15");
        custom.put("max_layer_height", "0.30");
        custom.put("layer_height", "0.2");
        custom.put("nozzle_diameter", "0.4");
        custom.put("z_offset", "0");
        custom.put("retract_length", "0.5");
        custom.put("retract_speed", "30");
        custom.put("deretract_speed", "30");
        custom.put("retract_before_travel", "2");

        custom.put("machine_limits_usage", "time_estimate_only");
        custom.put("machine_max_acceleration_e", "5000");
        custom.put("machine_max_acceleration_extruding", "500");
        custom.put("machine_max_acceleration_retracting", "1000");
        custom.put("machine_max_acceleration_travel", "500");
        custom.put("machine_max_acceleration_x", "500");
        custom.put("machine_max_acceleration_y", "500");
        custom.put("machine_max_acceleration_z", "100");
        custom.put("machine_max_feedrate_e", "60");
        custom.put("machine_max_feedrate_x", "500");
        custom.put("machine_max_feedrate_y", "500");
        custom.put("machine_max_feedrate_z", "10");
        custom.put("machine_max_jerk_e", "5");
        custom.put("machine_max_jerk_x", "8");
        custom.put("machine_max_jerk_y", "8");
        custom.put("machine_max_jerk_z", "0.4");
        custom.put("machine_min_extruding_rate", "0");
        custom.put("machine_min_travel_rate", "0");

        custom.put("start_gcode", "G90 ; use absolute coordinates\\nM83 ; extruder relative mode\\nM104 S{is_nil(idle_temperature[0]) ? 150 : idle_temperature[0]} ; set temporary nozzle temp to prevent oozing during homing\\nM140 S{first_layer_bed_temperature[0]} ; set final bed temp\\nG4 S30 ; allow partial nozzle warmup\\nG28 ; home all axis\\nG1 Z50 F240\\nG1 X2.0 Y10 F3000\\nM104 S{first_layer_temperature[0]} ; set final nozzle temp\\nM190 S{first_layer_bed_temperature[0]} ; wait for bed temp to stabilize\\nM109 S{first_layer_temperature[0]} ; wait for nozzle temp to stabilize\\nG1 Z0.28 F240\\nG92 E0\\nG1 X2.0 Y140 E10 F1500 ; prime the nozzle\\nG1 X2.3 Y140 F5000\\nG92 E0\\nG1 X2.3 Y10 E10 F1200 ; prime the nozzle\\nG92 E0");
        custom.put("end_gcode", "{if max_layer_z < max_print_height}G1 Z{z_offset+min(max_layer_z+2, max_print_height)} F600 ; Move print head up{endif}\\nG1 X5 Y{print_bed_max[1]*0.85} F{travel_speed*60} ; present print\\n{if max_layer_z < max_print_height-10}G1 Z{z_offset+min(max_layer_z+70, max_print_height-10)} F600 ; Move print head further up{endif}\\n{if max_layer_z < max_print_height*0.6}G1 Z{max_print_height*0.6} F600 ; Move print head further up{endif}\\nM140 S0 ; turn off heatbed\\nM104 S0 ; turn off temperature\\nM107 ; turn off fan\\nM84 X Y E ; disable motors");

        return custom;
    }

    public static ConfigObject createCustomFilamentProfile() {
        ConfigObject genericFilament = new ConfigObject(SliceBeam.INSTANCE.getString(R.string.IntroCustomProfileFilamentName));
        genericFilament.profileListType = ConfigObject.PROFILE_LIST_FILAMENT;
        genericFilament.put("first_layer_bed_temperature", "60");
        genericFilament.put("bed_temperature", "60");
        genericFilament.put("first_layer_temperature", "210");
        genericFilament.put("temperature", "210");
        genericFilament.put("filament_type", "PLA");
        genericFilament.put("slowdown_below_layer_time", "8");
        genericFilament.put("cooling", "1");
        genericFilament.put("fan_always_on", "1");
        genericFilament.put("fan_below_layer_time", "20");
        genericFilament.put("idle_temperature", "150");
        return genericFilament;
    }
}
