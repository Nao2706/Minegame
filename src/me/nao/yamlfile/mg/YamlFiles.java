package me.nao.yamlfile.mg;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;

import me.nao.main.mg.Minegame;

public class YamlFiles extends YamlConfiguration {
	 
    private final String fileName;
    private final Minegame plugin;
    private final File file;
    private final File folder;

    // Constructor principal - este es el que importa
    public YamlFiles(Minegame plugin, String fileName, File folder) {
        this.folder = folder;
        this.plugin = plugin;
        this.fileName = fileName + (fileName.endsWith(".yml") ? "" : ".yml");
        this.file = new File(this.folder, this.fileName);
        createFile();
    }

    // Para archivos en la raíz: new YamlFile(plugin, "config.yml")
    public YamlFiles(Minegame plugin, String fileName) {
        this(plugin, fileName, plugin.getDataFolder());
    }

    // Para archivos en subcarpeta: new YamlFile(plugin, "mapa1", "mapas")
    public YamlFiles(Minegame plugin, String fileName, String subFolder) {
        this(plugin, fileName, new File(plugin.getDataFolder(), subFolder));
    }

    private void createFile() {
        try {
            if (!folder.exists()) {
                folder.mkdirs(); // crea la carpeta si no existe
            }

            if (file.exists()) {
                load(file);
                return;
            }

            // Si existe dentro del .jar, lo copia
            // OJO: esto solo funciona si el archivo está en la raíz del jar
            // Para archivos de mapas, siempre va al else
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
                load(file);
            } else {
                // Archivo de datos nuevo (mapas), lo crea vacío
                file.createNewFile();
                load(file);
                save(file);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Creation of Configuration '" + fileName + "' failed.", e);
        }
    }

    // ESTE es el save bueno. Guarda donde debe, no barre carpetas.
    public void save() {
        try {
            save(this.file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Save of the file '" + fileName + "' failed.", e);
        }
    }

    public void reload() {
        try {
            load(this.file);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Reload of the file '" + fileName + "' failed.", e);
        }
    }

    public void delete() {
        if (!file.delete()) {
            plugin.getLogger().log(Level.SEVERE, "Error on delete the file '" + fileName + "'.");
        }
    }

    // Getters útiles para tu manager
    public File getFile() { return file; }
    public String getFileName() { return fileName; }

    // ---- ESTO REEMPLAZA tu getSpecificYamlFile ----
    // Ya no necesitas una clase aparte.
    // Uso: YamlFile mapa = YamlFile.loadSpecific(plugin, "mapas", "mapa1");
    public static YamlFiles loadSpecific(Minegame plugin, String folder, String name) {
        return new YamlFiles(plugin, name, folder);
    }
}
